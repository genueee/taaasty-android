package ru.taaasty;


import java.util.Date;
import java.util.List;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Query;
import ru.taaasty.model.Feed;
import ru.taaasty.model.FeedItem;

public interface TaaastyService {

    @GET("/feeds/live.json")
    void getLiveFeed(@Query("since_entry_id") Integer sinceEntryId,
                               @Query("limit") Integer limit,
                               Callback<Feed> cb);

    /**
     * Моя, только моя лента. Тоже самое что и tlog/by_id, только еще показываются и личные сообщения.
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param cb
     */
    @GET("/feeds/my.json")
    void getMyFeed(@Query("since_entry_id") Long sinceEntryId,
                     @Query("limit") Integer limit,
                     Callback<Feed> cb);

    /**
     * Лучшие записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param date
     * @param kind
     * @param rating
     * @param cb
     */
    @GET("/feeds/best.json")
    void getBestFeed(@Query("since_entry_id") Long sinceEntryId,
                   @Query("limit") Integer limit,
                   @Query("date") Date date,
                   @Query("kind") String kind,
                   @Query("rating") String rating,
                   Callback<Feed> cb);

    /**
     * Анонимные записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param cb
     */
    @GET("/feeds/anonymous.json")
    void getAnonymousFeed(@Query("since_entry_id") Long sinceEntryId,
                   @Query("limit") Integer limit,
                   Callback<Feed> cb);

    /**
     * Лента подписок с вкраплениями своих постов
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param cb
     */
    @GET("/feeds/friends.json")
    void getFriendsFeed(@Query("since_entry_id") Long sinceEntryId,
                          @Query("limit") Integer limit,
                          Callback<Feed> cb);

    /**
     * Поиск записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param cb
     */
    @GET("/feeds/search.json")
    void getFriendsFeed(
            @Query("query") String query,
            @Query("since_entry_id") Long sinceEntryId,
            @Query("limit") Integer limit,
            Callback<Feed> cb);
}
