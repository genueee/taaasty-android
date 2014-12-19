package ru.taaasty;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.ArrayList;

import ru.taaasty.model.Conversation;
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
            "ru.taaasty.PusherService.action.ACTION_CANCEL_STATUS_BAR_NOTIFICATION";

    private static final String ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION =
            "ru.taaasty.PusherService.action.ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION";

    private static final int NOTIFICATION_ID = 1;

    private static final int CONVERSATION_NOTIFICATION_ID = 2;

    private static final int MAX_NOTIFICATIONS = 3;

    private volatile int mDisableStatusBarNotifications;

    private final Context mContext;

    private final NotificationManagerCompat mNotificationManager;

    private volatile ArrayList<Notification> mNotifications = new ArrayList<>(MAX_NOTIFICATIONS);

    private volatile ArrayList<Conversation> mConversations = new ArrayList<>(MAX_NOTIFICATIONS);

    public StatusBarNotification(Context context) {
        mContext = context;
        mNotificationManager = NotificationManagerCompat.from(context);
        mDisableStatusBarNotifications = 0;
        IntentFilter filter = new IntentFilter(ACTION_CANCEL_STATUS_BAR_NOTIFICATION);
        filter.addAction(ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION);
        mContext.registerReceiver(this, filter);
    }

    public void onDestroy() {
        mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO
        if (ACTION_CANCEL_STATUS_BAR_NOTIFICATION.equals(intent.getAction())) {
            cancelNotificationNotification();
        } else if (ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION.equals(intent.getAction())) {
            cancelConversationNotification();
        }
    }

    public synchronized void addEnableDisableNotifications(boolean enable) {
        if (enable) {
            mDisableStatusBarNotifications -= 1;
        } else {
            mDisableStatusBarNotifications += 1;
        }
        if (mDisableStatusBarNotifications == 1) cancelNotifications();
    }

    public synchronized void append(Notification notification) {
        if (mDisableStatusBarNotifications > 0) return;
        mNotifications.add(notification);
        refreshStatusBarNotification();
    }

    public synchronized void append(Conversation conversation) {
        if (mDisableStatusBarNotifications > 0) return;
        mConversations.add(conversation);
        refreshStatusBarNotification();
    }

    public synchronized void refreshStatusBarNotification() {
        refreshStatusBarNotificationNotification();
        refreshStatusBarConversationNotification();
    }

    private void refreshStatusBarNotificationNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mNotifications.isEmpty()) {
            mNotificationManager.cancel(NOTIFICATION_ID);
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
                mNotifications.size(), mNotifications.size());

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getNotificationText(mNotifications.get(0)))
                        //.setNumber(mNotifications.size())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setWhen(mNotifications.get(0).createdAt.getTime())
                .setShowWhen(true)
                .setAutoCancel(true);

        if (mNotifications.size() == 1) {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void refreshStatusBarConversationNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mConversations.isEmpty()) {
            mNotificationManager.cancel(CONVERSATION_NOTIFICATION_ID);
            return;
        }

        if (mDisableStatusBarNotifications > 0) {
            return;
        }

        Intent resultIntent = new Intent(mContext, NotificationsActivity.class); // TODO
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION);

        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(mContext, 0,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = mContext.getResources().getQuantityString(R.plurals.conversation_received_title,
                mConversations.size(), mConversations.size());

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getConversationText(mConversations.get(0)))
                        //.setNumber(mNotifications.size())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setWhen(mConversations.get(0).createdAt.getTime())
                .setShowWhen(true)
                .setSound(Uri.parse("android.resource://"
                        + mContext.getPackageName() + "/" + R.raw.incoming_message))
                .setAutoCancel(true);

        mNotificationManager.notify(CONVERSATION_NOTIFICATION_ID, notificationBuilder.build());
    }

    private synchronized void cancelNotifications() {
        mNotifications.clear();
        mConversations.clear();
        mNotificationManager.cancelAll();
    }

    private synchronized void cancelNotificationNotification() {
        mNotifications.clear(); // TODO: mark as read
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private synchronized void cancelConversationNotification() {
        mConversations.clear(); // TODO: mark as read
        mNotificationManager.cancel(CONVERSATION_NOTIFICATION_ID);
    }

    private Spanned getNotificationText(Notification notification) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getName());

        CustomTypefaceSpan cts = new CustomTypefaceSpan(mContext, R.style.TextAppearanceSlugInlineGreen,
                FontManager.getInstance().getFontSystemBold());

        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext,
                R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        ssb.append(notification.actionText);
        return ssb;
    }

    private Spanned getConversationText(Conversation conversation) {
        User author = conversation.recipient;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getName());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext, R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        if (conversation.lastMessage != null && !TextUtils.isEmpty(conversation.lastMessage.contentHtml)) {
            ssb.append(Html.fromHtml(conversation.lastMessage.contentHtml));
        }
        return ssb;
    }
}
