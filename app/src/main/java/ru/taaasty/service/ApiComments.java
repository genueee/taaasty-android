package ru.taaasty.service;

import retrofit.http.GET;
import retrofit.http.Query;
import rx.Observable;

/**
 * http://api.taaasty.ru:80/v1/comments.json?entry_id=19221688
 *
 */
public interface ApiComments {

    /**
     * Получение комментариев
     * @param entryId
     * @param sinceCommentId
     * @param limit
     * @return
     */
    @GET("/comments.json")
    Observable<ru.taaasty.model.Comments> getComments(@Query("entry_id") Long entryId,
                               @Query("since_comment_id") Long sinceCommentId,
                               @Query("limit") Integer limit
                               );

}
