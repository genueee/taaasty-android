package ru.taaasty.events;

import ru.taaasty.model.Conversation;

/**
 * Добавлено или обновлено сообщение в чате
 */
public class MessageChanged {

    public final Conversation.Message message;

    public MessageChanged(Conversation.Message message) {
        this.message = message;
    }
}
