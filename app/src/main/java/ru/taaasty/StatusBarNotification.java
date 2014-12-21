package ru.taaasty;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.util.ArrayList;

import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.Notification;
import ru.taaasty.model.User;
import ru.taaasty.ui.CustomTypefaceSpan;
import ru.taaasty.ui.messages.ConversationActivity;
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

    private volatile int mDisableStatusBarNotifications;

    private final Context mContext;

    private final NotificationManagerCompat mNotificationManager;

    private volatile ArrayList<Notification> mNotifications = new ArrayList<>(3);

    private boolean mSeveralConversations;

    @Nullable
    private Conversation.Message mLastMessage;

    private int mConversationMessagesCount;

    private LongSparseArray<Integer> mConversationVisibility = new LongSparseArray<>(1);

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
        if (ACTION_CANCEL_STATUS_BAR_NOTIFICATION.equals(intent.getAction())) {
            markNotificationsAsRead();
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

    public synchronized void onConversationVisibilityChanged(ConversationVisibilityChanged event) {
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

        Notification lastNotification = mNotifications.get(mNotifications.size() - 1);

        String title = mContext.getResources().getQuantityString(R.plurals.notifications_received_title,
                mNotifications.size(), mNotifications.size());

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

        notificationBuilder = new NotificationCompat.Builder(mContext)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(largeIcon)
                .setContentTitle(title)
                .setContentText(getNotificationText(lastNotification))
                        //.setNumber(mNotifications.size())
                .setContentIntent(resultPendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setWhen(lastNotification.createdAt.getTime())
                .setShowWhen(true)
                .setAutoCancel(true);

        if (mNotifications.size() == 1) {
            notificationBuilder.setDefaults(NotificationCompat.DEFAULT_SOUND);
        }

        mNotificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void refreshStatusBarConversationNotification() {
        NotificationCompat.Builder notificationBuilder;

        if (mConversationMessagesCount == 0) {
            mNotificationManager.cancel(CONVERSATION_NOTIFICATION_ID);
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

        Intent deleteIntent = new Intent();
        deleteIntent.setAction(ACTION_CANCEL_STATUS_BAR_CONVERSATION_NOTIFICATION);

        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(mContext, 0,
                deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = mContext.getResources().getQuantityString(R.plurals.conversation_received_title,
                mConversationMessagesCount, mConversationMessagesCount);

        Bitmap largeIcon = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.ic_launcher);

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

        mNotificationManager.notify(CONVERSATION_NOTIFICATION_ID, notificationBuilder.build());
    }

    private synchronized void cancelNotifications() {
        mNotifications.clear();
        mConversationMessagesCount = 0;
        mSeveralConversations = false;
        mNotificationManager.cancelAll();
    }

    private synchronized void cancelNotificationNotification() {
        mNotifications.clear();
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private void markNotificationsAsRead() {
        for (Notification notification: mNotifications) {
            if (!notification.isMarkedAsRead()) {
                PusherService.markNotificationAsRead(mContext, notification.id);
            }
        }
    }

    private synchronized void cancelConversationNotification() {
        mConversationMessagesCount = 0;
        mSeveralConversations = false;
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
}
