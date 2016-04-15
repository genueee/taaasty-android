package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.UpdateMessages;

/**
 * Created by alexey on 04.12.14.
 */
public class UpdateMessagesReceived {

    public final UpdateMessages updateMessages;

    public UpdateMessagesReceived(UpdateMessages updateMessages) {
        this.updateMessages = updateMessages;
    }

}
