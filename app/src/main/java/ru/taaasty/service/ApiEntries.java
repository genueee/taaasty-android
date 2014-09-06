package ru.taaasty.service;

import retrofit.client.Response;
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
import rx.Observable;

public interface ApiEntries {

    public static final String PRIVACY_PRIVATE = "private";
    public static final String PRIVACY_PUBLIC = "public";
    public static final String PRIVACY_LIVE = "live";

    @Multipart
    @POST("/entries/image.json")
    Response createImagePostSync(
            @Part("title") String title,
            @Part("privacy") String privacy,
            @Part("file") TypedOutput file);


    @FormUrlEncoded
    @POST("/entries/text.json")
    Observable<Object> createTextPost(@Field("title") String title,
                                      @Field("text") String text,
                                      @Field("privacy") String privacy);

    @FormUrlEncoded
    @POST("/entries/text.json")
    Response createTextPostSync(@Field("title") String title,
                                      @Field("text") String text,
                                      @Field("privacy") String privacy);

    @FormUrlEncoded
    @PUT("/entries/text/{id}.json")
    Observable<Object> updateTextPost(
            @Path("id") String id,
            @Field("title") String title,
            @Field("text") String text,
            @Field("privacy") String privacy);

    @FormUrlEncoded
    @POST("/entries/quote.json")
    Observable<Object> createQuoteEntry(@Field("text") String text,
                                      @Field("source") String source,
                                      @Field("privacy") String privacy);

    @FormUrlEncoded
    @POST("/entries/quote.json")
    Response createQuoteEntrySync(@Field("text") String text,
                                        @Field("source") String source,
                                        @Field("privacy") String privacy);

    @FormUrlEncoded
    @PUT("/entries/quote/{id}.json")
    Observable<Object> updateQuoteEntry(
            @Path("id") String id,
            @Field("text") String text,
            @Field("source") String source,
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


}
