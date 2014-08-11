package ru.taaasty.service;

import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Rating;
import rx.Observable;

public interface ApiEntries {

    public static final String PRIVACY_LOCK = "lock";
    public static final String PRIVACY_UNLOCK = "unlock";
    public static final String PRIVACY_LIVE = "live";
    public static final String PRIVACY_RATING = "rating";

    /**
     * Получение статьи
     * @param id
     * @param withComments
     * @return
     */
    @GET("/entries/{id}.json")
    Observable<Entry> getEntry(@Path("id") Long id,
                                 @Query("include_comments") Boolean withComments);

    /**
     * Удаление статьи
     * @param id
     * @return
     */
    @DELETE("/entries/{id}.json")
    Observable<Object> deleteEntry(@Path("id") Long id);


    @POST("/entries/{entry_id}/votes.json")
    Observable<Rating> vote(@Path("entry_id") long entryId);

    @DELETE("/entries/{entry_id}/votes.json")
    Observable<Rating> unvote(@Path("entry_id") long entryId);
}
