package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.conversations.Message;

/**
 * Добавлено или обновлено сообщение в чате
 */
public class MessageChanged {

    public final Message message;

    public MessageChanged(Message message) {
        this.message = message;
    }
}
