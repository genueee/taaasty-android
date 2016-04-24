package ru.taaasty.events.pusher;

import ru.taaasty.rest.model.RemovedMessages;

/**
 * push notification - удалены сообщения в чате.
 * Расылается при нажатии "удалить у себя", т.е. сообщения удаляются только у автора, у остальных
 * остаются видимыми.
 */
public class MessagesRemoved {

    public final RemovedMessages messages;

    public MessagesRemoved(RemovedMessages messages) {
        this.messages = messages;
    }


}
