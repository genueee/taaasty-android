package ru.taaasty;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.util.ArrayList;

import ru.taaasty.model.Notification;
import ru.taaasty.model.User;
import ru.taaasty.ui.CustomTypefaceSpan;
import ru.taaasty.ui.tabbar.NotificationsActivity;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 28.10.14.
 */
public class StatusBarNotification extends BroadcastReceiver {

    private static final String ACTION_CANCEL_STATUS_BAR_NOTIFICATION =
            "\"ru.taaasty.PusherService.action.ACTION_CANCEL_STATUS_BAR_NOTIFICATION";

    private static final int NOTIFICATION_ID = 1;

    private static final int MAX_NOTIFICATIONS = 3;

    private volatile int mDisableStatusBarNotifications;

    private final Context mContext;

    private final NotificationManagerCompat mNotificationManager;

    private volatile ArrayList<Notification> mStatusBarNotifications = new ArrayList<>(MAX_NOTIFICATIONS);

    public StatusBarNotification(Context context) {
        mContext = context;
        mNotificationManager = NotificationManagerCompat.from(context);
        mDisableStatusBarNotifications = 0;
        mContext.registerReceiver(this, new IntentFilter(ACTION_CANCEL_STATUS_BAR_NOTIFICATION));
    }

    public void onDestroy() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_CANCEL_STATUS_BAR_NOTIFICATION.equals(intent.getAction())) {
            cancelNotification();
        }
    }

    public synchronized void addEnableDisableNotifications(boolean enable) {
        if (enable) {
            mDisableStatusBarNotifications -= 1;
        } else {
            mDisableStatusBarNotifications += 1;
        }
        if (mDisableStatusBarNotifications == 1) cancelNotification();
    }

    public synchronized void append(Notification notification) {
        if (mDisableStatusBarNotifications > 0) return;
        mStatusBarNotifications.add(notification);
        refreshStatusBarNotification();
    }

    public synchronized void refreshStatusBarNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mStatusBarNotifications.isEmpty()) {
            mNotificationManager.cancelAll();
            return;
        }

        if (mDisableStatusBarNotifications > 0) {
            return;
        }

        Intent resultIntent = new Intent(mContext, NotificationsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(ACTION_CANCEL_STATUS_BAR_NOTIFICATION);

        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(mContext, 0,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = mContext.getResources().getQuantityString(R.plurals.notifications_received_title,
                mStatusBarNotifications.size(), mStatusBarNotifications.size());

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getNotificationText(mStatusBarNotifications.get(0)))
                        //.setNumber(mStatusBarNotifications.size())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setWhen(mStatusBarNotifications.get(0).createdAt.getTime())
                .setShowWhen(true)
                .setAutoCancel(true);

        if (mStatusBarNotifications.size() == 1) {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private synchronized void cancelNotification() {
        mStatusBarNotifications.clear();
        mNotificationManager.cancelAll();
    }

    private Spanned getNotificationText(Notification notification) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getSlug());

        CustomTypefaceSpan cts = new CustomTypefaceSpan(mContext, R.style.TextAppearanceSlugInlineGreen,
                FontManager.getInstance().getFontSystemBold());

        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext,
                R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        ssb.append(notification.actionText);
        return ssb;
    }
}
