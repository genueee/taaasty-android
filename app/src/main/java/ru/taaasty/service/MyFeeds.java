package ru.taaasty.service;

import retrofit.http.GET;
import retrofit.http.Query;
import ru.taaasty.model.Feed;
import rx.Observable;

public interface MyFeeds {

    /**
     * Моя лента (с приватными сообщениями)
     * @param sinceEntryId
     * @param limit
     * @return
     */
    @GET("/my_feeds/tlog.json")
    Observable<Feed> getMyFeed(@Query("since_entry_id") Integer sinceEntryId,
                                 @Query("limit") Integer limit);

    /**
     * Друзья и свои посты
     * @param sinceEntryId
     * @param limit
     * @return
     */
    @GET("/my_feeds/friends.json")
    Observable<Feed> getMyFriendsFeed(@Query("since_entry_id") Integer sinceEntryId,
                            @Query("limit") Integer limit);

    /**
     * Избранные записи
     * @param sinceEntryId
     * @param limit
     * @return
     */
    @GET("/my_feeds/favorites.json")
    Observable<Feed> getMyFavoritesFeed(@Query("since_entry_id") Integer sinceEntryId,
                                      @Query("limit") Integer limit);

    /**
     * Приватные записи
     * @param sinceEntryId
     * @param limit
     * @return
     */
    @GET("/my_feeds/private.json")
    Observable<Feed> getMyPrivateFeed(@Query("since_entry_id") Integer sinceEntryId,
                                        @Query("limit") Integer limit);


}
