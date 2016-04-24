package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.RemovedUserMessages;

/**
 * push notificaton - удалены собщения в чате.
 * Вызывается при нажатии "удалить у всех", т.е. сообщения удалены и у автора
 * и у всех пользователей
 * Тип: {@linkplain ru.taaasty.rest.model.RemovedUserMessages}.
 * В списке - новые, измененные сообщения. У новых сообщений остается тот же ID,
 * но меняется тип сообщения на System и текст.
 */
public class UserMessagesRemoved {

    public final RemovedUserMessages messages;

    public UserMessagesRemoved(RemovedUserMessages messages) {
        this.messages = messages;
    }

}
