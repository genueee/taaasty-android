package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.Message;

/**
 * Добавлено или обновлено сообщение в чате
 */
public class MessageChanged {

    public final Conversation conversation;

    public final Message message;

    public MessageChanged(Conversation conversation, Message message) {
        this.conversation = conversation;
        this.message = message;
    }
}
