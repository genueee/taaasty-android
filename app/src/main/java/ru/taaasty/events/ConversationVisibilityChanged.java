package ru.taaasty.events;

/**
 * Открыт или закрыт диалог
 */
public class ConversationVisibilityChanged {

    public final long userId;

    public final boolean isShown;

    public ConversationVisibilityChanged(long userId, boolean shown) {
        this.userId = userId;
        this.isShown = shown;
    }

}
