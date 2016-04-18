package ru.taaasty.events;

/**
 * Открыта или закрыта переписка. Нужно, чтобы не слать сообщения самому себе
 */
public class ConversationVisibilityChanged {

    public final long conversationId;

    public final boolean isShown;

    public ConversationVisibilityChanged(long conversationId, boolean shown) {
        this.conversationId = conversationId;
        this.isShown = shown;
    }

}
