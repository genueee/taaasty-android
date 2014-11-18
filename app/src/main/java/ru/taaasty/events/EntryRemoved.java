package ru.taaasty.events;


/**
 * Пользователь удалил пост
 */
public class EntryRemoved {

    public final long postId;

    public EntryRemoved(long postId) {
        this.postId = postId;
    }
}
