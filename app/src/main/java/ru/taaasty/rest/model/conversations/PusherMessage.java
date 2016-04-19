package ru.taaasty.rest.model.conversations;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 19.04.16.
 */
public class PusherMessage {

    /**
     * Приходит вроде только в уведомлениях и, возможно, через пушер.
     */
    @Nullable
    public Conversation conversation;

    public PusherMessage() {
    }

}
