package ru.taaasty.events;

import ru.taaasty.rest.model.RemovedMessages;

/**
 * Created by alexey on 15.04.16.
 */
public class MessagesRemoved {

    public final RemovedMessages messages;

    public MessagesRemoved(RemovedMessages messages) {
        this.messages = messages;
    }


}
