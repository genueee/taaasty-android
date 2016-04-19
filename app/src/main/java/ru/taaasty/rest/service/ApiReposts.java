package ru.taaasty.rest.service;

import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Status;
import rx.Observable;

/**
 * Created by alexey on 19.09.15.
 */
public interface ApiReposts {

    /**
     * Репост
     * @param tlogId tlog, куда репостить
     * @param entryId запись
     * @return Запись. Хер его знает, какая. По крайней мере, в ней нет his_relationship,
     * my_relationship,is_favorited, is_watching, can_vote, can_favorite, can_edit, can_delete,
     * can_watch и отличаются can_report, rating-is_voteable. Лучше особо не использовать и запросить заново
     */
    @FormUrlEncoded
    @POST("reposts.json")
    Observable<Entry> repost(
            @Field("tlog_id") long tlogId,
            @Field("entry_id") long entryId);

    /**
     * Удалить пост из тлога
     * @param tlogId tlog, откуда удалять
     * @param entryId запись
     */
    @DELETE("reposts.json")
    Observable<Object> deletePost(
            @Query("tlog_id") long tlogId,
            @Query("entry_id") long entryId);

    /**
     * Одобрить репост
     */
    @FormUrlEncoded
    @PUT("reposts/accept.json")
    Observable<Object> acceptReport(
            @Field("tlog_id") long tlogId,
            @Field("entry_id") long entryId);

    @FormUrlEncoded
    @PUT("reposts/decline.json")
    Observable<Status> declineReport(
            @Field("tlog_id") long tlogId,
            @Field("entry_id") long entryId);

}
