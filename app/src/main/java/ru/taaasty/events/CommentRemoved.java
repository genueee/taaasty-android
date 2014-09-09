package ru.taaasty.events;

/**
 * Пользователь удалил комментарий
 */
public class CommentRemoved {

    public final long commentId;

    public CommentRemoved(long commentId) {
        this.commentId = commentId;
    }

}
