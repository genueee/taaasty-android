package ru.taaasty.events;

import ru.taaasty.model.Notification;

/**
 * Уведомление пришло изи изменилось
 */
public class NotificationReceived {

    public final Notification notification;

    public NotificationReceived(Notification notification) {
        this.notification = notification;
    }

}
