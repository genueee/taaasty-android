package ru.taaasty.service;

import java.util.Date;

import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Query;
import ru.taaasty.model.Feed;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface Feeds {
    @GET("/feeds/live.json")
    Observable<Feed> getLiveFeed(@Query("since_entry_id") Long sinceEntryId,
                     @Query("limit") Integer limit);


    /**
     * Моя, только моя лента. Тоже самое что и tlog/by_id, только еще показываются и личные сообщения.
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     */
    @GET("/feeds/my.json")
    Observable<Feed> getMyFeed(@Query("since_entry_id") Long sinceEntryId,
                   @Query("limit") Integer limit);

    /**
     * Лучшие записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     * @param date
     * @param kind
     * @param rating
     */
    @GET("/feeds/best.json")
    Observable<Feed> getBestFeed(@Query("since_entry_id") Long sinceEntryId,
                     @Query("limit") Integer limit,
                     @Query("date") Date date,
                     @Query("kind") String kind,
                     @Query("rating") String rating);

    /**
     * Анонимные записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     */
    @GET("/feeds/anonymous.json")
    Observable<Feed> getAnonymousFeed(@Query("since_entry_id") Long sinceEntryId,
                          @Query("limit") Integer limit);

    /**
     * Лента подписок с вкраплениями своих постов
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     */
    @GET("/feeds/friends.json")
    Observable<Feed> getFriendsFeed(@Query("since_entry_id") Long sinceEntryId,
                        @Query("limit") Integer limit);

    /**
     * Поиск записи
     * @param sinceEntryId ID публикации старше которой отдавать ленту
     * @param limit Ограничение по количеству записей
     */
    @GET("/feeds/search.json")
    Observable<Feed> getFriendsFeed(
            @Query("query") String query,
            @Query("since_entry_id") Long sinceEntryId,
            @Query("limit") Integer limit);
}
