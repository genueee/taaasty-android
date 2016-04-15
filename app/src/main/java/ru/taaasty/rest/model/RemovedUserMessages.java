package ru.taaasty.rest.model;

/**
 * Created by alexey on 15.04.16.
 */
public class RemovedUserMessages {

    public long conversationId;

    public RemovedMessage messages[];

    public static class RemovedMessage {
        public long id;
        public String content;
        public String type;
    }
}
