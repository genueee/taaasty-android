package ru.taaasty.service;

import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedOutput;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Rating;
import ru.taaasty.model.iframely.IFramely;
import rx.Observable;

public interface ApiEntries {

    @Multipart
    @POST("/entries/image.json")
    Entry createImagePostSync(
            @Part("title") String title,
            @Part("privacy") String privacy,
            @Part("file") TypedOutput file);

    @Multipart
    @PUT("/entries/image/{id}.json")
    Entry updateImagePostSync(
            @Path("id") String id,
            @Part("title") String title,
            @Part("privacy") String privacy,
            @Part("file") TypedOutput file);


    @FormUrlEncoded
    @POST("/entries/text.json")
    Entry createTextPostSync(@Field("title") String title,
                                      @Field("text") String text,
                                      @Field("privacy") String privacy);

    @FormUrlEncoded
    @PUT("/entries/text/{id}.json")
    Entry updateTextPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("text") String text,
            @Field("privacy") String privacy);


    @FormUrlEncoded
    @POST("/entries/quote.json")
    Entry createQuoteEntrySync(@Field("text") String text,
                                        @Field("source") String source,
                                        @Field("privacy") String privacy);

    @FormUrlEncoded
    @PUT("/entries/quote/{id}.json")
    Entry updateQuoteEntrySync(
            @Path("id") String id,
            @Field("text") String text,
            @Field("source") String source,
            @Field("privacy") String privacy);

    @FormUrlEncoded
    @POST("/entries/anonymous.json")
    Entry createAnonymousPostSync(@Field("title") String title,
                             @Field("text") String text);

    @FormUrlEncoded
    @PUT("/entries/anonymous/{id}.json")
    Entry updateAnonymousPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("text") String text);

    @FormUrlEncoded
    @POST("/entries/video.json")
    Entry createVideoPostSync(@Field("title") String title,
                             @Field("video_url") String url,
                             @Field("privacy") String privacy);

    @FormUrlEncoded
    @PUT("/entries/video/{id}.json")
    Entry updateVideoPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("video_url") String url,
            @Field("privacy") String privacy);

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
     * Пожаловатсья на статью
     * @param id
     * @return
     */
    @POST("/entries/{id}/report.json")
    Observable<Object> reportEntry(@Path("id") Long id);

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

    @FormUrlEncoded
    @POST("/favorites.json")
    Observable<Object> addToFavorites(@Field("entry_id") long entryId);

    @DELETE("/favorites.json")
    Observable<Object> removeFromFavorites(@Query("entry_id") long entryId);


    @POST("/embeding/iframely.json")
    Observable<IFramely> getIframely(@Query("url") String url);
}
