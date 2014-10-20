package ru.taaasty.events;

import ru.taaasty.PusherService;

/**
 * Created by alexey on 23.10.14.
 */
public class NotificationsStatus {

    public final PusherService.NotificationsStatus newStatus;

    public NotificationsStatus(PusherService.NotificationsStatus newStatus) {
        this.newStatus = newStatus;
    }
}
