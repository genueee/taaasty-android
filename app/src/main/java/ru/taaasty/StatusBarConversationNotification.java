package ru.taaasty;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.LongSparseArray;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.pusher.MessageChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.ui.messages.ConversationActivity;
import ru.taaasty.ui.tabbar.ConversationsActivity;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.UiUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

import static ru.taaasty.PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE;

/**
 * Уведомления в статусбаре о диалогах
 */
public class StatusBarConversationNotification {

    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarConversation";

    private final TaaastyApplication mContext;

    private final NotificationManagerCompat mNotificationManager;

    private final SharedPreferences mSharedPreferences;

    private volatile boolean mIsPaused;

    private boolean mSeveralConversations;

    @Nullable
    private Conversation mLastConversation;

    @Nullable
    private Message mLastMessage;

    private int mConversationMessagesCount;

    private LongSparseArray<Integer> mConversationVisibility = new LongSparseArray<>(1);

    private Subscription mLoadConversationsSubscription = Subscriptions.unsubscribed();

    private LoadNotificationDataTask mLoadImagesTask = null;

    StatusBarConversationNotification(TaaastyApplication application) {
        mContext = application;
        mNotificationManager = NotificationManagerCompat.from(application);
        mSharedPreferences = mContext.getSharedPreferences(PreferenceHelper.PREFS_NAME, 0);
        mIsPaused = false;
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
        mLoadConversationsSubscription.unsubscribe();
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }
    }

    public void onLogout() {
        mConversationMessagesCount = 0;
        mConversationVisibility.clear();
        mLastMessage = null;
        mLastConversation = null;
        mSeveralConversations = false;
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }
        cancelNotification();
    }

    public void onConversationNotificationCancelled() {
        cancelNotification();
    }

    public void pause() {
        mIsPaused = true;
        cancelNotification();
    }

    public void resume() {
        mIsPaused = false;
    }

    public void cancelNotification() {
        mConversationMessagesCount = 0;
        mSeveralConversations = false;
        mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
    }

    public void onEventMainThread(ConversationVisibilityChanged event) {
        cancelNotification();
        int val = mConversationVisibility.get(event.conversationId, 0);
        if (event.isShown) {
            val += 1;
            mConversationVisibility.put(event.conversationId, val);
        } else {
            if (val > 0) val -= 1;
            if (val == 0) {
                mConversationVisibility.remove(event.conversationId);
            } else {
                mConversationVisibility.put(event.conversationId, val);
            }
        }
    }

    public void onEventMainThread(MessageChanged event) {
        if (event.conversation == null) {
            // TODO а что делать?
            return;
        } else {
            // TODO проверять на do_not_disturb и is_disabled
            append(event.conversation, event.message);
        }
    }

    /**
     * По GCM получено уведомление push_conversation. Последнее сообщение показывается в статусбаре
     * @param intent на котором будет выполнен completeWakefulIntent()
     */
    // XXX абсолютно неверно. Нотификации могут быть левые и могут задалбывать звуком
    public void onGcmConversationReceived(final Intent intent) {
        if (!mLoadConversationsSubscription.isUnsubscribed()) {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        if (!isNotificationsTurnedOn()) {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        rx.Observable<List<Conversation>> observable = RestClient.getAPiMessenger().getConversations(null)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .finallyDo(() -> {
                    // XXX: должно быть в PusherService
                    GcmBroadcastReceiver.completeWakefulIntent(intent);
                });

        mLoadConversationsSubscription = observable.subscribe(new Observer<List<Conversation>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "can not get conversations", e);
            }

            @Override
            public void onNext(List<Conversation> conversations) {
                Conversation lastMessageConversation = null;
                int activeConversationCount = 0;
                // Самое последнее непрочитанное сообщение - наше (на самом деле нет)
                for (Conversation conversation: conversations) {
                    if (conversation.getUnreadMessagesCount() > 0) {
                        activeConversationCount += 1;
                        if (conversation == null ||
                                (conversation.getLastMessage().createdAt.getTime() > lastMessageConversation.getLastMessage().createdAt.getTime())) {
                            lastMessageConversation = conversation;
                            // API в lastMessage не возвращает conversationId и ещё дохуя каких полей
                            lastMessageConversation.getLastMessage().conversationId = conversation.getId();
                        }
                    }
                }
                if (lastMessageConversation != null) append(lastMessageConversation, lastMessageConversation.getLastMessage());
                mSeveralConversations = activeConversationCount > 1;
            }
        });
    }


    public synchronized void append(Conversation conversation, Message message) {
        if (mIsPaused) return;
        if (mConversationVisibility.get(message.conversationId, 0) != 0) return;

        if (message.isFromMe(conversation)) {
            // Игнорируем свои сообщения
            return;
        }

        if (!message.isMarkedAsRead()) {
            mConversationMessagesCount += 1;

            if (mLastMessage != null && (mLastMessage.conversationId != message.conversationId)) {
                mSeveralConversations = true;
            }
            mLastMessage = message;
            mLastConversation = conversation;
            refreshNotification();
        } else {
            if (mLastMessage.id == message.id) {
                // XXX здесь можем получить ситуацию, когда mConversationMessagesCount > 0 и mLastMessage=null
                mLastMessage = null;
                mLastConversation = null;
                mConversationMessagesCount -= 1;
                refreshNotification();
            }
        }
    }

    private void refreshNotification() {
        NotificationCompat.Builder notificationBuilder;
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }

        if (mConversationMessagesCount == 0
                || !isNotificationsTurnedOn()) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
            return;
        }

        if (mIsPaused || mLastMessage == null) {
            return;
        }

        if ((mLastConversation == null)
                || (ConversationHelper.getInstance().getConversationIconUrl(mLastConversation) == null)) {
            // Облом, не будет у нас нормальной иконки, геморно грузить
            // TODO а всё таки надо грузить конверсейшн и её иконку
            refreshNotification(null, null);
        } else {
            String url = ConversationHelper.getInstance().getConversationIconUrl(mLastConversation);
            mLoadImagesTask = new LoadNotificationDataTask(mContext);
            mLoadImagesTask.execute(url);
        }
    }

    private void refreshNotification(Bitmap largeIcon, Bitmap wearableBackground) {
        NotificationCompat.Builder notificationBuilder;
        NotificationCompat.Action voiceReplyAction = null;

        if (mConversationMessagesCount == 0) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
            return;
        }

        if (mIsPaused || mLastMessage == null) {
            return;
        }

        PendingIntent resultPendingIntent = createContentPendingIntent(mLastConversation, mLastMessage, !mSeveralConversations);

        String title = mContext.getResources().getQuantityString(R.plurals.conversation_received_title,
                mConversationMessagesCount, mConversationMessagesCount);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getMessageText(mLastConversation, mLastMessage))
                .setWhen(mLastMessage.createdAt.getTime())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(createDeletePendingIntent())
                .setColor(mContext.getResources().getColor(R.color.unread_conversations_count_background))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        ;

        int defaults = 0;
        if (isVibrateTurnedOn()) defaults |= NotificationCompat.DEFAULT_VIBRATE;
        if (isLightsTurnedOn()) defaults |= NotificationCompat.DEFAULT_LIGHTS;
        if (defaults != 0) notificationBuilder.setDefaults(defaults);

        if (isSoundTurnedOn()) notificationBuilder.setSound(Uri.parse("android.resource://"
                + mContext.getPackageName() + "/" + R.raw.incoming_message));

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(getMessageText(mLastConversation, mLastMessage));
        notificationBuilder.setStyle(bigStyle);

        if (!mSeveralConversations) voiceReplyAction = createVoiceReplyAction(mLastMessage);

        if (wearableBackground != null || voiceReplyAction != null) {
            NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
            if (wearableBackground != null) extender.setBackground(wearableBackground);
            if (voiceReplyAction != null) extender.addAction(voiceReplyAction);
            notificationBuilder.extend(extender);
        }

        mNotificationManager.notify(Constants.NOTIFICATION_ID_CONVERSATION, notificationBuilder.build());
        AnalyticsHelper.getInstance().sendNotificationsEvent("Показано уведомление о новом сообщении");
    }

    private PendingIntent createContentPendingIntent(Conversation conversation, Message message, boolean showConversation) {
        // ConversationsActivity intent
        Intent firstIntent = new Intent(mContext, ConversationsActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addNextIntent(firstIntent);

        // Conversation intent
        if (showConversation) {
            Intent conversationIntent = ConversationActivity.createNotificationIntent(mContext, message.conversationId);
            stackBuilder.addNextIntent(conversationIntent);
        }

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent createDeletePendingIntent() {
        Intent deleteIntent = new Intent(mContext, IntentService.class);
        deleteIntent.setAction(IntentService.ACTION_NOTIFY_CONVERSATION_NOTIFICATION_CANCELLED);
        return PendingIntent.getService(mContext, 0,
                deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private NotificationCompat.Action createVoiceReplyAction(Message message) {
        RemoteInput remoteInput = new RemoteInput.Builder(IntentService.EXTRA_CONVERSATION_VOICE_REPLY_CONTENT)
                .setLabel(mContext.getText(R.string.notification_action_reply_label))
                .build();

        Intent replyIntent = IntentService.getVoiceReplyToConversationIntent(mContext,
                message.conversationId, new long[] {message.id});
        PendingIntent replyPendingIntent = PendingIntent.getService(mContext, 0, replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Action.Builder(R.drawable.ic_full_reply,
                mContext.getString(R.string.notification_action_reply_title), replyPendingIntent)
                .addRemoteInput(remoteInput)
                .build();
    }

    private CharSequence getMessageText(Conversation conversation, Message message) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        // Ставим ник перед сообщением, иначе в приходящем тексте его нет
        if (conversation != null
                && conversation.getType() == Conversation.Type.PRIVATE
            && ((PrivateConversation)conversation).getRecipient() != null
                && !Session.getInstance().isMe(((PrivateConversation)conversation).getRecipient().getId())) {
            User author = ((PrivateConversation) conversation).getRecipient();
            // wearable не поддерживает CustomTypefaceSpan/TextAppearanceSpan, поэтому ставим так
            UiUtils.appendStyled(ssb, author.getNameWithPrefix(),
                    new ForegroundColorSpan(mContext.getResources().getColor(R.color.text_color_green)));
            if (!TextUtils.isEmpty(message.contentHtml)) {
                ssb.append(' ');
            }
        }
        ssb.append(UiUtils.safeFromHtml(message.contentHtml));
        return ssb;
    }

    private boolean isNotificationsTurnedOn() {
        if (!PreferenceHelper.getBooleanValue(mSharedPreferences, PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS))
            return false;

        if (!PreferenceHelper.getBooleanValue(mSharedPreferences, PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS))
            return false;

        return true;
    }

    private boolean isVibrateTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE);
    }

    private boolean isSoundTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND);
    }

    private boolean isLightsTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS);
    }


    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key == null) return;
            switch (key) {
                case PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS:
                case PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS:
                case PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE:
                case PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND:
                case PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS:
                    refreshNotification();
                    break;
            }
        }
    };

    private class LoadNotificationDataTask extends StatusBarNotifications.LoadNotificationDataTask {

        public LoadNotificationDataTask(Context context) {
            super(context, true);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mLoadImagesTask = null;
            refreshNotification(bigIcon, wearableBackground);
        }
    }
}
