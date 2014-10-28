package ru.taaasty.events;


/**
 * Пользователь удалил пост
 */
public class PostRemoved {

    public final long postId;

    public PostRemoved(long postId) {
        this.postId = postId;
    }
}
