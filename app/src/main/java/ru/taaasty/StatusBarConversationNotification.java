package ru.taaasty;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.LongSparseArray;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.messages.ConversationActivity;
import ru.taaasty.ui.tabbar.NotificationsActivity;
import ru.taaasty.utils.UiUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Уведомления в статусбаре о диалогах
 */
public class StatusBarConversationNotification {

    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarConversation";

    private final TaaastyApplication mContext;

    private final NotificationManagerCompat mNotificationManager;

    private volatile boolean mIsPaused;

    private boolean mSeveralConversations;

    @Nullable
    private Conversation.Message mLastMessage;

    private int mConversationMessagesCount;

    private LongSparseArray<Integer> mConversationVisibility = new LongSparseArray<>(1);

    private Subscription mLoadConversationsSubscription = Subscriptions.unsubscribed();

    StatusBarConversationNotification(TaaastyApplication application) {
        mContext = application;
        mNotificationManager = NotificationManagerCompat.from(application);
        mIsPaused = false;
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mLoadConversationsSubscription.unsubscribe();
    }

    public void onLogout() {
        mConversationMessagesCount = 0;
        mConversationVisibility.clear();
        mLastMessage = null;
        mSeveralConversations = false;
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
        int val = mConversationVisibility.get(event.userId, 0);
        if (event.isShown) {
            val += 1;
            mConversationVisibility.put(event.userId, val);
        } else {
            if (val > 0) val -= 1;
            if (val == 0) {
                mConversationVisibility.remove(event.userId);
            } else {
                mConversationVisibility.put(event.userId, val);
            }
        }
    }

    public void onEventMainThread(MessageChanged event) {
        append(event.message);
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

        rx.Observable<List<Conversation>> observable = RestClient.getAPiMessenger().getConversations(null)
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        // XXX: должно быть в PusherService
                        GcmBroadcastReceiver.completeWakefulIntent(intent);
                    }
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
                Conversation.Message lastMessage = null;
                int activeConversationCount = 0;
                // Самое последнее непрочитанное сообщение - наше (на самом деле нет)
                for (Conversation conversation: conversations) {
                    if (conversation.unreadMessagesCount > 0) {
                        activeConversationCount += 1;
                        if (lastMessage == null ||
                                (conversation.lastMessage.createdAt.getTime() > lastMessage.createdAt.getTime())) {
                            lastMessage = conversation.lastMessage;
                            // API в lastMessage не возвращает conversationId и ещё дохуя каких полей
                            lastMessage.conversationId = conversation.id;
                            lastMessage.conversation = conversation;
                        }
                    }
                }
                if (lastMessage != null) append(lastMessage);
                mSeveralConversations = activeConversationCount > 1;
            }
        });
    }


    public synchronized void append(Conversation.Message message) {
        if (mIsPaused) return;
        if (mConversationVisibility.get(message.recipientId, 0) != 0
                || mConversationVisibility.get(message.userId, 0) != 0) return;

        if (message.isFromMe()) {
            // Игнорируем свои сообщения
            return;
        }

        if (!message.isMarkedAsRead()) {
            mConversationMessagesCount += 1;

            if (mLastMessage != null && (mLastMessage.conversationId != message.conversationId)) {
                mSeveralConversations = true;
            }
            mLastMessage = message;
            refreshNotification();
        } else {
            if (mLastMessage.id == message.id) {
                // XXX здесь можем получить ситуацию, когда mConversationMessagesCount > 0 и mLastMessage=null
                mLastMessage = null;
                mConversationMessagesCount -= 1;
                refreshNotification();
            }
        }
    }

    private void refreshNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mConversationMessagesCount == 0) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
            return;
        }

        if (mIsPaused || mLastMessage == null) {
            return;
        }

        PendingIntent resultPendingIntent;
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);

        // NotificationsActivity intent
        Intent firstIntent = new Intent(mContext, NotificationsActivity.class);
        firstIntent.putExtra(NotificationsActivity.ARG_KEY_SHOW_SECTION, NotificationsActivity.SECTION_CONVERSATIONS);
        stackBuilder.addNextIntent(firstIntent);

        // Conversation intent
        if (!mSeveralConversations) {
            long recipient;
            if (UserManager.getInstance().isMe(mLastMessage.recipientId)) {
                recipient = mLastMessage.userId;
            } else {
                recipient = mLastMessage.recipientId;
            }
            Intent conversationIntent = ConversationActivity.createIntent(mContext, mLastMessage.conversationId, recipient);
            stackBuilder.addNextIntent(conversationIntent);
        }

        resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

        Intent deleteIntent = new Intent(mContext, IntentService.class);
        deleteIntent.setAction(IntentService.ACTION_NOTIFY_CONVERSATION_NOTIFICATION_CANCELLED);
        PendingIntent deletePendingIntent = PendingIntent.getService(mContext, 0,
                deleteIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        String title = mContext.getResources().getQuantityString(R.plurals.conversation_received_title,
                mConversationMessagesCount, mConversationMessagesCount);

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getMessageText(mLastMessage))
                .setWhen(mLastMessage.createdAt.getTime())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(Color.YELLOW)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE | NotificationCompat.DEFAULT_LIGHTS)
                .setSound(Uri.parse("android.resource://"
                        + mContext.getPackageName() + "/" + R.raw.incoming_message))
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        ;

        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
        bigStyle.bigText(getMessageText(mLastMessage));
        notificationBuilder.setStyle(bigStyle);

        if (!mSeveralConversations) addRemoteInput(notificationBuilder);

        mNotificationManager.notify(Constants.NOTIFICATION_ID_CONVERSATION, notificationBuilder.build());
        mContext.sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_NOTIFICATIONS, "Показано уведомление о новом сообщении", null);
    }

    private void addRemoteInput(NotificationCompat.Builder builder) {
        RemoteInput remoteInput = new RemoteInput.Builder(IntentService.EXTRA_CONVERSATION_VOICE_REPLY_CONTENT)
                .setLabel(mContext.getText(R.string.notification_action_reply_label))
                .build();

        Intent replyIntent = IntentService.getVoiceReplyToConversationIntent(mContext, mLastMessage.conversationId, new long[] {mLastMessage.id});
        PendingIntent replyPendingIntent =
                PendingIntent.getService(mContext, 0, replyIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.drawable.ic_full_reply,
                        mContext.getString(R.string.notification_action_reply_title), replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .build();

        builder.extend(new NotificationCompat.WearableExtender().addAction(action));
    }

    private CharSequence getMessageText(Conversation.Message message) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();

        if (message.conversation != null
            && message.conversation.recipient != null
                && !UserManager.getInstance().isMe(message.conversation.recipient.getId())) {
            User author = message.conversation.recipient;
            ssb.append('@');
            ssb.append(author.getName());
            UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext, R.style.TextAppearanceSlugInlineGreen);
            if (!TextUtils.isEmpty(message.contentHtml)) {
                ssb.append(' ');
            }
        }
        ssb.append(UiUtils.safeFromHtml(message.contentHtml));
        return ssb;
    }
}
