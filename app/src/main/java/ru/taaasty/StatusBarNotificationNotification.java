package ru.taaasty;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.NotificationMarkedAsRead;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.NotificationList;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.Userpic;
import ru.taaasty.ui.tabbar.NotificationsActivity;
import ru.taaasty.utils.UiUtils;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import static ru.taaasty.PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND;
import static ru.taaasty.PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE;

/**
 * Уведомления в статусбаре об уведомлениях
 */
public class StatusBarNotificationNotification {

    private static final String PREFS_STATUS_BAR_NOTIFICATIONS = "statusbarnotificationnotification";

    private static final String PROPERTY_LAST_SEEN_NOTIFICATION_ID = "last_seen_notification_id";

    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarNotifications";

    private final TaaastyApplication mContext;

    private final NotificationManagerCompat mNotificationManager;

    private final SharedPreferences mSharedPreferences;

    /* Нотификации, которые мы показываем в статусбаре */
    private volatile ArrayList<Notification> mNotifications = new ArrayList<>(2);

    /**
     * ID последнего уведомления, которое пользователь видел.
     * Считаем, что уведомления в статусбаре - это уведомления, которые он ещё не видел.
     *
     */
    private volatile long mLastSeenNewestNotificationId = 0;

    private volatile boolean mIsPaused;

    private Subscription mLoadNotificationsSubscription = Subscriptions.unsubscribed();

    private LoadNotificationDataTask mLoadImagesTask;

    StatusBarNotificationNotification(TaaastyApplication appContext) {
        mContext = appContext;
        mNotificationManager = NotificationManagerCompat.from(appContext);
        mSharedPreferences = mContext.getSharedPreferences(PreferenceHelper.PREFS_NAME, 0);
        mIsPaused = false;
    }

    public void onCreate() {
        loadState();
        EventBus.getDefault().register(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }

    public void onDestroy() {
        mLoadNotificationsSubscription.unsubscribe();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
        EventBus.getDefault().unregister(this);
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }
    }

    public void onLogout() {
        mLastSeenNewestNotificationId = 0;
        mContext.getSharedPreferences(PREFS_STATUS_BAR_NOTIFICATIONS, 0).edit().clear().commit();
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }
        cancelNotification();
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

    public void pause() {
        mIsPaused = true;
        cancelNotification();
    }

    public void resume() {
        mIsPaused = false;
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

    public synchronized void onNotificationsMarkedAsRead() {
        long maxId = 0;
        for (Notification n: mNotifications) maxId = Math.max(maxId, n.id);
        mNotifications.clear();
        onNewNotificationIdSeen(maxId);
    }

    /**
     * По GCM получено уведомление push_notification. Загружаются последние уведомления и показываются в статусбаре
     * @param intent на котором будет выполнен completeWakefulIntent()
     */
    // XXX абсолютно неверно. Возможно повторение показа нотификаций, которые уже были показаны
    public void onGcmNotificationReceived(final Intent intent) {

        if (!mLoadNotificationsSubscription.isUnsubscribed()) {
            GcmBroadcastReceiver.completeWakefulIntent(intent);
            return;
        }

        if (!isNotificationsTurnedOn()) {
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
                if (notificationList.notifications.length == 0) {
                    // Не показанных нотификаций вроде нет. На всякий случай, оставляем всё как есть
                    if (DBG) Log.v(TAG, "onGcmPushNotificationReceived: no new notifications");
                    return;
                }
                long maxReadId = mLastSeenNewestNotificationId;
                List<Notification> notifications = new ArrayList<>(notificationList.notifications.length);

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
                    refreshNotification();
                }
            }
        });
    }

    public synchronized void append(Notification notification) {
        if (mIsPaused) return;
        mNotifications.add(notification);
        refreshNotification();
    }

    private void refreshNotification() {
        if (mLoadImagesTask != null) {
            mLoadImagesTask.cancel(true);
            mLoadImagesTask = null;
        }

        if (mIsPaused) {
            return;
        }

        if (!isNotificationsTurnedOn()) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_POST);
            return;
        }

        Notification lastNotification = null;
        boolean isCollapsed = false;

        for (int i = mNotifications.size() - 1; i >= 0; --i) {
            Notification notification = mNotifications.get(i);
            if (isNotificationEventDisabled(notification)) continue;
            if (lastNotification == null) {
                lastNotification = notification;
            } else {
                isCollapsed = true;
                break;
            }
        }

        if (lastNotification == null) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_POST);
            return;
        }

        // Есть картинка - показываем её
        if (lastNotification.hasImage()) {
            mLoadImagesTask = new LoadNotificationDataTask(lastNotification, isCollapsed , mContext, false);
            mLoadImagesTask.execute(lastNotification.image.url);
        } else if (lastNotification.sender != null && lastNotification.sender.getUserpic() != null) {
            // Есть юзерпик - отлично
            Userpic userpic = lastNotification.sender.getUserpic();
            mLoadImagesTask = new LoadNotificationDataTask(lastNotification, isCollapsed, mContext, true);
            mLoadImagesTask.execute(userpic.originalUrl);
        } else {
            // Ничего нет. Ну нет так нет.
            refreshNotification(lastNotification, isCollapsed, null, null);
        }
    }

    /**
     * Обновление уведомления в статусбаре после загрузки картинок
     */
    private void refreshNotification(Notification lastNotification, boolean isCollapsed, @Nullable Bitmap largeIcon, @Nullable Bitmap wearableBackground) {
        NotificationCompat.Builder notificationBuilder;

        if (lastNotification == null) {
            mNotificationManager.cancel(Constants.NOTIFICATION_ID_POST);
            return;
        }

        if (mIsPaused) return;

        notificationBuilder = createNotification(lastNotification, isCollapsed,
                largeIcon, wearableBackground);

        mContext.sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_NOTIFICATIONS, "Показано уведомление о новом уведомлении", null);
        mNotificationManager.notify(Constants.NOTIFICATION_ID_POST, notificationBuilder.build());
    }

    public void cancelNotification() {
        mNotifications.clear();
        mNotificationManager.cancel(Constants.NOTIFICATION_ID_POST);
    }

    private NotificationCompat.Builder createNotification(Notification notification,
                                                          boolean isCollapsed,
                                                          Bitmap largeIcon,
                                                          Bitmap wearableBackground) {
        NotificationCompat.Builder notificationBuilder;
        PendingIntent resultIntent, deleteIntent;
        int title;

        if (isCollapsed) {
            title = R.string.notifications_received_title;
        } else {
            title = notification.getActionNotificationTitle();
            if (title == 0) title = R.string.notification_received_title;
        }

        resultIntent = createOpenNotificationsPendingIntent(notification);
        deleteIntent = createMarkAsReadPendingIntent(isCollapsed ? null : new long[]{notification.id});

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(mContext.getText(title))
                .setContentText(getNotificationText(notification, true))
                .setContentIntent(resultIntent)
                .setDeleteIntent(deleteIntent)
                .setWhen(notification.createdAt.getTime())
                .setColor(mContext.getResources().getColor(R.color.green_background_normal))
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
        ;

        int defaults = 0;
        if (isVibrateTurnedOn()) defaults |= NotificationCompat.DEFAULT_VIBRATE;
        if (isLightsTurnedOn()) defaults |= NotificationCompat.DEFAULT_LIGHTS;
        if (isSoundTurnedOn()) defaults |= NotificationCompat.DEFAULT_SOUND;
        if (defaults != 0) notificationBuilder.setDefaults(defaults);

        if (!TextUtils.isEmpty(notification.text)) {
            NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
            bigStyle.bigText(getNotificationText(notification, true));
            notificationBuilder.setStyle(bigStyle);
        }

        if (wearableBackground != null) {
            NotificationCompat.WearableExtender extender = new NotificationCompat.WearableExtender();
            extender.setBackground(wearableBackground);
            notificationBuilder.extend(extender);
        }

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
        return PendingIntent.getService(mContext, 0, deleteIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Spanned getNotificationText(Notification notification, boolean allowLongText) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        UiUtils.appendStyled(ssb, author.getNameWithPrefix(),
                new ForegroundColorSpan(mContext.getResources().getColor(R.color.text_color_green)));

        ssb.append(' ');
        ssb.append(notification.actionText);
        if (!TextUtils.isEmpty(notification.text) && allowLongText) {
            ssb.append(": ");
            ssb.append(UiUtils.safeFromHtml(notification.text));
        }
        return ssb;
    }

    private synchronized void loadState() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_STATUS_BAR_NOTIFICATIONS, 0);
        mLastSeenNewestNotificationId = prefs.getLong(PROPERTY_LAST_SEEN_NOTIFICATION_ID, mLastSeenNewestNotificationId);
        if (DBG) Log.v(TAG, "loadState() lastSeenNotification: " + mLastSeenNewestNotificationId);
    }

    @SuppressLint("CommitPrefEdits")
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

    private boolean isNotificationsTurnedOn() {
        if (!PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS)) return false;

        return PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS);


    }

    private boolean isVibrateTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences,
                PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE);
    }

    private boolean isSoundTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences, PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND);
    }

    private boolean isLightsTurnedOn() {
        return PreferenceHelper.getBooleanValue(mSharedPreferences, PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS);
    }

    private boolean isNotificationEventDisabled(Notification notification) {
        if (notification.action == null) return true;
        switch (notification.action) {
            case Notification.ACTION_VOTE:
            case Notification.ACTION_FAVORITE:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES);
            case Notification.ACTION_FOLLOWING_APPROVE:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE);
            case Notification.ACTION_FOLLOWING_REQUEST:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST);
            case Notification.ACTION_FOLLOWING:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING);
            case Notification.ACTION_NEW_COMMENT:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS);
            case Notification.ACTION_NEW_MENTION:
                return !PreferenceHelper.getBooleanValue(mSharedPreferences,
                        PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS);
            default:
                return false;
        }
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key == null) return;
            switch (key) {
                case PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS:
                case PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE:
                case PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS:
                    refreshNotification();
                    break;
            }
        }
    };

    private class LoadNotificationDataTask extends StatusBarNotifications.LoadNotificationDataTask {

        private final Notification mLastNotification;
        private final boolean mIsCollapsed;

        public LoadNotificationDataTask(Notification lastNotification, boolean isCollapsed, Context context, boolean roundCorners) {
            super(context, roundCorners);
            mLastNotification = lastNotification;
            mIsCollapsed = isCollapsed;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mLoadImagesTask = null;
            refreshNotification(mLastNotification, mIsCollapsed, bigIcon, wearableBackground);
        }
    }
}
