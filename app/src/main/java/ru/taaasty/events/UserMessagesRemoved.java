package ru.taaasty.events;

import ru.taaasty.rest.model.RemovedUserMessages;

/**
 * Created by alexey on 15.04.16.
 */
public class UserMessagesRemoved {

    public final RemovedUserMessages messages;

    public UserMessagesRemoved(RemovedUserMessages messages) {
        this.messages = messages;
    }

}
