package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.conversations.Conversation;

/**
 * Изменилась инфа по переписке
 */
public class ConversationChanged {

    public Conversation conversation;

    public ConversationChanged(Conversation conversation) {
        this.conversation = conversation;
    }

}
