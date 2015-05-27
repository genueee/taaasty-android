package ru.taaasty;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.util.LongSparseArray;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.events.NotificationMarkedAsRead;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.NotificationList;
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
 * Created by alexey on 28.10.14.
 */
public class StatusBarNotification {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarNotification";

    private static final String PREFS_STATUS_BAR_NOTIFICATIONS = "statusbarnotification";

    private static final String PROPERTY_LAST_SEEN_NOTIFICATION_ID = "last_seen_notification_id";

    private static volatile StatusBarNotification sInstance;

    private final Context mContext;

    private final NotificationManagerCompat mNotificationManager;

    private volatile int mDisableStatusBarNotifications;

    /* Нотификации, которые мы показываем в статусбаре */
    private volatile ArrayList<Notification> mNotifications = new ArrayList<>(2);

    /**
     * ID последнего уведомления, которое пользователь видел.
     * Считаем, что уведомления в статусбаре - это уведомления, которые он ещё не видел.
     *
     */
    private volatile long mLastSeenNewestNotificationId = 0;


    private boolean mSeveralConversations;

    @Nullable
    private Conversation.Message mLastMessage;

    private int mConversationMessagesCount;

    private LongSparseArray<Integer> mConversationVisibility = new LongSparseArray<>(1);

    private Subscription mLoadNotificationsSubscription = Subscriptions.unsubscribed();

    private Subscription mLoadConversationsSubscription = Subscriptions.unsubscribed();

    private StatusBarNotification(Context context) {
        mContext = context.getApplicationContext();
        mNotificationManager = NotificationManagerCompat.from(context);
        mDisableStatusBarNotifications = 0;
        loadState();
        EventBus.getDefault().register(this);

    }

    public void onDestroy() {
        mLoadNotificationsSubscription.unsubscribe();
        mLoadConversationsSubscription.unsubscribe();
        EventBus.getDefault().unregister(this);
    }

    public static void onAppInit(Context context) {
        sInstance = new StatusBarNotification(context);
    }

    public static StatusBarNotification getInstance() {
        return sInstance;
    }

    public void onEventMainThread(NotificationReceived event) {
        // TODO
        if (event.notification.isMarkedAsRead()) {
            ListIterator<Notification> le = mNotifications.listIterator();
            while (le.hasNext()) {
                Notification n = le.next();
                if (n.id == event.notification.id) {
                    le.remove();
                }
            }
        } else {
            append(event.notification);
        }
        onNewNotificationIdSeen(event.notification.id);
    }

    public void onEventMainThread(ConversationVisibilityChanged event) {
        cancelConversationNotification();
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

    public void onEventMainThread(NotificationMarkedAsRead event) {
        long maxId = 0;
        for (int i=0, size=event.itemMap.size(); i < size; ++i) {
            maxId = Math.max(maxId, event.itemMap.keyAt(i));
        }
        if (maxId != 0) onNewNotificationIdSeen(maxId);
    }

    public synchronized void onNewNotificationIdSeen(long id) {
        if (id > mLastSeenNewestNotificationId) {
            mLastSeenNewestNotificationId = id;
            storeState(false);
        }
    }

    public void enableStatusBarNotifications() {
        addEnableDisableNotifications(false);
    }

    public void disableStatusBarNotifications() {
        addEnableDisableNotifications(true);
    }

    private synchronized void addEnableDisableNotifications(boolean enable) {
        if (enable) {
            mDisableStatusBarNotifications -= 1;
        } else {
            mDisableStatusBarNotifications += 1;
        }
        if (mDisableStatusBarNotifications == 1) cancelNotifications();
    }

    public synchronized void onNotificationsMarkedAsRead() {
        mNotifications.clear();
    }

    public synchronized void onConversationNotificationCancelled() {
        cancelConversationNotification();
    }

    /**
     * По GCM получено уведомление push_notification. Загружаются последние уведомления и показываются в статусбаре
     * @param intent на котором будет выполнен completeWakefulIntent()
     */
    // XXX абсолютно неверно. Возможно повторение показа нотификаций, которые уже были показаны
    public void onGcmPushNotificationReceived(final Intent intent) {

        if (!mLoadNotificationsSubscription.isUnsubscribed()) {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        long fromMsgId = mLastSeenNewestNotificationId;

        rx.Observable<NotificationList> observable = RestClient.getAPiMessenger()
                .getNotifications(null, (fromMsgId == 0 ? null : fromMsgId),
                        null, 2, "desc")
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        // XXX: должно быть в PusherService
                        GcmBroadcastReceiver.completeWakefulIntent(intent);
                    }
                });

        mLoadNotificationsSubscription = observable.subscribe(new Observer<NotificationList>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "get notification error", e);
            }

            @Override
            public void onNext(NotificationList notificationList) {
                if (notificationList.notifications.isEmpty()) {
                    // Не показанных нотификаций вроде нет. На всякий случай, оставляем всё как есть
                    if (DBG) Log.v(TAG, "onGcmPushNotificationReceived: no new notifications");
                    return;
                }
                long maxReadId = mLastSeenNewestNotificationId;
                List<Notification> notifications = new ArrayList<>(notificationList.notifications.size());

                // maxReadId - максимальный id последнего прочитанного уведомления
                for (Notification n: notificationList.notifications) {
                    if (n.isMarkedAsRead()) maxReadId = Math.max(n.id, maxReadId);
                }

                for (Notification n: notificationList.notifications) {
                    if (!n.isMarkedAsRead() && (n.id > maxReadId)) notifications.add(n);
                }

                synchronized (this) {
                    mNotifications.clear();
                    mNotifications.addAll(notifications);
                    Collections.sort(mNotifications, Notification.SORT_BY_ID_COMPARATOR);
                    onNewNotificationIdSeen(maxReadId);
                    refreshStatusBarNotification();
                }
            }
        });
    }

    /**
     * По GCM получено уведомление push_conversation. Последнее сообщение показывается в статусбаре
     * @param intent на котором будет выполнен completeWakefulIntent()
     */
    // XXX абсолютно неверно. Нотификации могут быть левые и могут задалбывать звуком
    public void onGcmPushConversationReceived(final Intent intent) {
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
                // Самое последнее непрочитанное сообщение - наше (на самом деле нет)
                for (Conversation conversation: conversations) {
                    if (conversation.unreadMessagesCount > 0) {
                        if (lastMessage == null ||
                                (conversation.lastMessage.createdAt.getTime() > lastMessage.createdAt.getTime())) {
                            lastMessage = conversation.lastMessage;
                            // API в lastMessage не возвращает conversationId и ещё дохуя каких полей
                            lastMessage.conversationId = conversation.id;
                        }
                    }
                }
                if (lastMessage != null) append(lastMessage);
            }
        });
    }

    public synchronized void append(Notification notification) {
        if (mDisableStatusBarNotifications > 0) return;
        mNotifications.add(notification);
        refreshStatusBarNotification();
    }

    public synchronized void append(Conversation.Message message) {
        if (mDisableStatusBarNotifications > 0) return;
        if (mConversationVisibility.get(message.recipientId, 0) != 0
                || mConversationVisibility.get(message.userId, 0) != 0) return;
        mConversationMessagesCount += 1;
        if (mLastMessage != null && (mLastMessage.conversationId != message.conversationId)) {
            mSeveralConversations = true;
        }
        mLastMessage = message;
        refreshStatusBarNotification();
    }

    public synchronized void refreshStatusBarNotification() {
        refreshStatusBarNotificationNotification();
        refreshStatusBarConversationNotification();
    }

    private void refreshStatusBarNotificationNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mNotifications.isEmpty()) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_POST);
            return;
        }

        if (mDisableStatusBarNotifications > 0) {
            return;
        }

        Notification lastNotification = mNotifications.get(mNotifications.size() - 1);
        if (mNotifications.size() == 1) {
            notificationBuilder = createSingleNotification(lastNotification);
            // notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND); TODO
        } else {
            notificationBuilder = createCollapsedNotification(lastNotification);
        }

        mNotificationManager.notify(Constants.NOTIFICATION_ID_POST, notificationBuilder.build());
    }

    private NotificationCompat.Builder createSingleNotification(Notification notification) {
        NotificationCompat.Builder notificationBuilder;

        // TODO largeIcon - нормальный, если есть в нотификации
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);

        int title = notification.getActionNotificationTitle();
        if (title == 0) title = R.string.notification_received_title;

        PendingIntent resultPendingIntent = createOpenNotificationsPendingIntent(notification);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(mContext.getString(title))
                .setContentText(getNotificationText(notification))
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(createMarkAsReadPendingIntent(new long[] {notification.id}))
                .setWhen(notification.createdAt.getTime())
                .setShowWhen(true)
                .setAutoCancel(true);
        return notificationBuilder;
    }

    private NotificationCompat.Builder createCollapsedNotification(Notification lastNotification) {
        NotificationCompat.Builder notificationBuilder;
        // TODO largeIcon - нормальный, если есть в нотификации
        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_launcher);

        PendingIntent resultPendingIntent = createOpenNotificationsPendingIntent(null);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(mContext.getString(R.string.notifications_received_title))
                .setContentText(getNotificationText(lastNotification))
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(createMarkAsReadPendingIntent(null))
                .setWhen(lastNotification.createdAt.getTime())
                .setShowWhen(true)
                .setAutoCancel(true);

        return notificationBuilder;
    }

    private PendingIntent createOpenNotificationsPendingIntent(@Nullable Notification notification) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);

        // Открывает сначала уведомления
        Intent notificationsIntent = new Intent(mContext, NotificationsActivity.class);
        if (notification != null) {
            notificationsIntent.putExtra(NotificationsActivity.ARK_KEY_MARK_NOTIFICATIONS_AS_READ, new long[] {notification.id});
        }
        stackBuilder.addNextIntent(notificationsIntent);

        if (notification != null) {
            // Затем то, что лучше
            Intent nextIntent = notification.getOpenNotificationActivityIntent(mContext);
            if (nextIntent != null) stackBuilder.addNextIntent(nextIntent);

        }
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent createMarkAsReadPendingIntent(long ids[]) {
        Intent deleteIntent = IntentService.getMarkNotificationAsReadIntent(mContext, ids, true);
        PendingIntent deletePendingIntent = PendingIntent.getService(mContext, 0, deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        return deletePendingIntent;
    }

    private void refreshStatusBarConversationNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mConversationMessagesCount == 0) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
            return;
        }

        if (mDisableStatusBarNotifications > 0 || mLastMessage == null) {
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
                .setContentText(Html.fromHtml(mLastMessage.contentHtml))
                .setWhen(mLastMessage.createdAt.getTime())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setColor(Color.YELLOW)
                .setAutoCancel(true);

        if (mConversationMessagesCount <= 3) {
            notificationBuilder.setSound(Uri.parse("android.resource://"
                    + mContext.getPackageName() + "/" + R.raw.incoming_message));
        }

        mNotificationManager.notify(Constants.NOTIFICATION_ID_CONVERSATION, notificationBuilder.build());
    }

    private synchronized void cancelNotifications() {
        mNotifications.clear();
        mConversationMessagesCount = 0;
        mSeveralConversations = false;
        mNotificationManager.cancelAll();
    }

    private synchronized void cancelConversationNotification() {
        mConversationMessagesCount = 0;
        mSeveralConversations = false;
        mNotificationManager.cancel(Constants.NOTIFICATION_ID_CONVERSATION);
    }

    private Spanned getNotificationText(Notification notification) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getName());

        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext,
                R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        ssb.append(notification.actionText);
        return ssb;
    }

    private synchronized void loadState() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_STATUS_BAR_NOTIFICATIONS, 0);
        mLastSeenNewestNotificationId = prefs.getLong(PROPERTY_LAST_SEEN_NOTIFICATION_ID, mLastSeenNewestNotificationId);
        if (DBG) Log.v(TAG, "loadState() lastSeenNotification: " + mLastSeenNewestNotificationId);
    }

    private synchronized void storeState(boolean sync) {
        if (DBG) Log.v(TAG, "storeState() lastSeenNotification: " + mLastSeenNewestNotificationId);

        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_STATUS_BAR_NOTIFICATIONS, 0)
                .edit()
                .putLong(PROPERTY_LAST_SEEN_NOTIFICATION_ID, mLastSeenNewestNotificationId);

        if (sync) {
            editor.commit();
        } else {
            editor.apply();
        }
    }

    public synchronized void onLogout() {
        mLastSeenNewestNotificationId = 0;
        mContext.getSharedPreferences(PREFS_STATUS_BAR_NOTIFICATIONS, 0).edit().clear().commit();
        cancelNotifications();
    }

}
