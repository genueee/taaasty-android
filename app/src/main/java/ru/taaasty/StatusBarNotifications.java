package ru.taaasty;

import android.content.Intent;

/**
 * Created by alexey on 28.10.14.
 */
public class StatusBarNotifications {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarNotifications";

    private static final String PREFS_STATUS_BAR_NOTIFICATIONS = "statusbarnotification";

    private static volatile StatusBarNotifications sInstance;

    private volatile int mDisableStatusBarNotifications;

    private final StatusBarNotificationNotification mNotificationsHandler;

    private final StatusBarConversationNotification mConversationHandler;

    private StatusBarNotifications(TaaastyApplication application) {
        mDisableStatusBarNotifications = 0;
        mNotificationsHandler = new StatusBarNotificationNotification(application);
        mConversationHandler = new StatusBarConversationNotification(application);
        mNotificationsHandler.onCreate();
        mConversationHandler.onCreate();
    }

    public static void onAppInit(TaaastyApplication application) {
        sInstance = new StatusBarNotifications(application);
    }

    public void onDestroy() {
        mNotificationsHandler.onDestroy();
        mConversationHandler.onDestroy();
    }

    public static StatusBarNotifications getInstance() {
        return sInstance;
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

        if (mDisableStatusBarNotifications == 1) {
            mNotificationsHandler.pause();
            mConversationHandler.pause();
        } else {
            mNotificationsHandler.resume();
            mConversationHandler.resume();
        }
    }

    public synchronized void onNotificationsMarkedAsRead() {
        mNotificationsHandler.onNotificationsMarkedAsRead();
    }

    public void onNewNotificationIdSeen(long id) {
        mNotificationsHandler.onNewNotificationIdSeen(id);
    }

    public synchronized void onConversationNotificationCancelled() {
        mConversationHandler.onConversationNotificationCancelled();
    }


    public void onGcmPushNotificationReceived(final Intent intent) {
        mNotificationsHandler.onGcmNotificationReceived(intent);
    }

    public void onGcmPushConversationReceived(final Intent intent) {
        mConversationHandler.onGcmConversationReceived(intent);
    }

    public synchronized void onLogout() {
        mNotificationsHandler.onLogout();
        mConversationHandler.onLogout();
    }

}
