package ru.taaasty.rest.service;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.Relationships;
import ru.taaasty.rest.model.Tag;
import ru.taaasty.rest.model.TlogInfo;
import rx.Observable;

public interface ApiTlog {

    @GET("tlog/{id}.json")
    Observable<TlogInfo> getUserInfo(@Path("id") String idOrSlug);

    @GET("tlog/{id}/entries.json")
    Observable<Feed> getEntries(@Path("id") String idOrSlug,
                                @Query("since_entry_id") Long sinceEntryId,
                                @Query("limit") Integer limit);

    // XXX
    @GET("tlog/{id}/calendar.json")
    Observable<Object> getCalendar(@Path("id") String idOrSlug,
                                   @Query("since_entry_id") Integer sinceEntryId,
                                @Query("limit") Integer limit);

    // XXX
    @GET("tlog/{id}/followers.json")
    Observable<Relationships> getFollowers(@Path("id") String idOrSlug,
                                    @Query("since_position") Integer sincePosition,
                                   @Query("limit") Integer limit);

    // XXX
    @GET("tlog/{id}/followings.json")
    Observable<Relationships> getFollowings(@Path("id") String idOrSlug,
                                    @Query("since_position") Integer sincePosition,
                                    @Query("limit") Integer limit);

    // XXX
    @GET("tlog/{id}/tags.json")
    Observable<List<Tag>> getTags(@Path("id") String idOrSlug);

}
