package ru.taaasty.rest.model;

/**
 * Created by alexey on 11.09.14.
 */
public class Status {

    public static String status;

    /**
     * Статус в результате mark messages as read
     */
    public static class MarkMessagesAsRead {
        public static String status;
        public int readMessagesCount;
    }

}
