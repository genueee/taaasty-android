package ru.taaasty.events;

import java.util.Collection;

import ru.taaasty.model.Notification;

/**
 * Изменилось кол-во уведомлений, либо кол-во непрочитанных уведомлений, либо список уведомлений обновился полоностю
 */
public class NotificationsCountChanged {

    public final int count;

    public final int unreadCount;

    public final boolean notificationListRefreshed;

    public NotificationsCountChanged(Collection<Notification> notifications, boolean notificationListRefreshed) {
        int count = notifications.size();
        int unreadCount = 0;

        for (Notification notification: notifications) {
            if (!notification.isMarkedAsRead()) unreadCount += 1;
        }
        this.count = notifications.size();
        this.unreadCount = unreadCount;
        this.notificationListRefreshed = notificationListRefreshed;
    }

    public NotificationsCountChanged(int count, int unreadCount) {
        this.count = count;
        this.unreadCount = count;
        this.notificationListRefreshed = false;
    }

}
