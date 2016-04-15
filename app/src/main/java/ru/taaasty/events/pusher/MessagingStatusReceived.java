package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.MessagingStatus;

/**
 * Created by alexey on 16.12.14.
 */
public class MessagingStatusReceived {

    public final MessagingStatus data;

    public MessagingStatusReceived(MessagingStatus data) {
        this.data = data;
    }
}
