package ru.taaasty.events;

/**
 * Отправлена жалоба на комментарий
 */
public class ReportCommentSent {

    public final long commentId;

    public ReportCommentSent(long commentId) {
        this.commentId = commentId;
    }

}
