package ru.taaasty.events;

/**
 * тправлна жалоба на комментарий
 */
public class ReportCommentSent {

    public final long commentId;

    public ReportCommentSent(long commentId) {
        this.commentId = commentId;
    }

}
