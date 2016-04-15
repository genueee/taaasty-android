package ru.taaasty.events.pusher;

import ru.taaasty.BuildConfig;
import ru.taaasty.rest.model.Notification;

/**
 * Уведомление пришло изи изменилось
 */
public class NotificationReceived {

    public final Notification notification;

    public NotificationReceived(Notification notification) {
        this.notification = notification;
    }

    @Override
    public String toString() {
        if (!BuildConfig.DEBUG) return super.toString();
        return "NotificationReceived{" +
                "notification=" + notification +
                '}';
    }
}
