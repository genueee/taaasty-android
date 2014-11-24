package ru.taaasty.model;

import java.util.Date;
import java.util.List;

/**
 * Created by alexey on 24.11.14.
 */
public class UpdateMessages {

    public long conversationId;

    /**
     * ID сообщений
     */
    public List<UpdateMessageInfo> messages;


    public static class UpdateMessageInfo {

        public long id;

        public Date readAt;

        public String uuid;

    }
}
