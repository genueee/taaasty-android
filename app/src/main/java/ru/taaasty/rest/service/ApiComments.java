package ru.taaasty.rest.service;

import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * http://api.taaasty.com:80/v1/comments.json?entry_id=19221688
 *
 */
public interface ApiComments {

    public static final String ORDER_ASC = "asc";

    public static final String ORDER_DESC = "desc";

    /**
     * Получение комментариев
     * @param entryId
     * @param fromCommentId
     * @param toCommentId
     * @param order
     * @param limit
     * @return
     */
    @GET("comments.json")
    Observable<ru.taaasty.rest.model.Comments> getComments(@Query("entry_id") long entryId,
                                                        @Query("from_comment_id") Long fromCommentId,
                                                        @Query("to_comment_id") Long toCommentId,
                                                        @Query("order") String order,
                                                        @Query("limit") Integer limit
    );

    /**
     * Добавление комментрия
     * @param entryId
     * @param text
     * @return
     */
    @FormUrlEncoded
    @POST("comments.json")
    Observable<ru.taaasty.rest.model.Comment> postComment(@Field("entry_id")long entryId,
                                                      @Field("text") String text);

    /**
     * Редактирование комментария
     * @param commentId
     * @param text
     * @return
     */
    @FormUrlEncoded
    @PUT("comments/{id}.json")
    Observable<ru.taaasty.rest.model.Comment> putComment(@Path("id")long commentId, @Field("text") String text);

    /**
     * Удаление комментариня
     * @param commentId
     * @return
     */
    @DELETE("comments/{id}.json")
    Observable<Object> deleteComment(@Path("id") long commentId);


    /**
     * Пожаловаться на комментарий
     * @param id
     * @return а блядь хуй его знает, что там с сервера возвращается
     */
    @POST("comments/{id}/report.json")
    Observable<Object> reportComment(@Path("id")long id);

}
