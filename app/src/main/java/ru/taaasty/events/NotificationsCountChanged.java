package ru.taaasty.events;

import java.util.Collection;

import ru.taaasty.model.Notification;

/**
 * Изменилось кол-во уведомлений, либо кол-во непрочитанных уведомлений
 */
public class NotificationsCountChanged {

    public final int count;

    public final int unreadCount;

    public NotificationsCountChanged(Collection<Notification> notifications) {
        int count = notifications.size();
        int unreadCount = 0;

        for (Notification notification: notifications) {
            if (!notification.isMarkedAsRead()) unreadCount += 1;
        }
        this.count = notifications.size();
        this.unreadCount = unreadCount;
    }

    public NotificationsCountChanged(int count, int unreadCount) {
        this.count = count;
        this.unreadCount = count;
    }

}
