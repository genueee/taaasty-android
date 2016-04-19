package ru.taaasty.rest.service;


import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Rating;
import ru.taaasty.rest.model.iframely.IFramely;
import rx.Observable;

public interface ApiEntries {

    @Multipart
    @POST("entries/image.json")
    Observable<Entry> createImagePostSync(
            @Part("title") String title,
            @Part("privacy") String privacy,
            @Part("tlog_id") Long tlogId,
            @Part("file") MultipartBody.Part file);

    @Multipart
    @PUT("entries/image/{id}.json")
    Observable<Entry> updateImagePostSync(
            @Path("id") String id,
            @Part("title") String title,
            @Part("privacy") String privacy,
            @Part("tlog_id") Long tlogId,
            @Part("file") MultipartBody.Part file);


    @FormUrlEncoded
    @POST("entries/text.json")
    Observable<Entry> createTextPostSync(@Field("title") String title,
                                      @Field("text") String text,
                                      @Field("privacy") String privacy,
                                      @Field("tlog_id") Long tlogId);

    @FormUrlEncoded
    @PUT("entries/text/{id}.json")
    Observable<Entry> updateTextPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("text") String text,
            @Field("privacy") String privacy,
            @Field("tlog_id") Long tlogId);


    @FormUrlEncoded
    @POST("entries/quote.json")
    Observable<Entry> createQuoteEntrySync(@Field("text") String text,
                                        @Field("source") String source,
                                        @Field("privacy") String privacy,
                                        @Field("tlog_id") Long tlogId);

    @FormUrlEncoded
    @PUT("entries/quote/{id}.json")
    Observable<Entry> updateQuoteEntrySync(
            @Path("id") String id,
            @Field("text") String text,
            @Field("source") String source,
            @Field("privacy") String privacy,
            @Field("tlog_id") Long tlogId);

    @FormUrlEncoded
    @POST("entries/anonymous.json")
    Observable<Entry> createAnonymousPostSync(@Field("title") String title,
                                        @Field("text") String text);

    @FormUrlEncoded
    @PUT("entries/anonymous/{id}.json")
    Observable<Entry> updateAnonymousPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("text") String text);

    @FormUrlEncoded
    @POST("entries/video.json")
    Observable<Entry> createVideoPostSync(@Field("title") String title,
                             @Field("video_url") String url,
                             @Field("privacy") String privacy,
                              @Field("tlog_id") Long tlogId);

    @FormUrlEncoded
    @PUT("entries/video/{id}.json")
    Observable<Entry> updateVideoPostSync(
            @Path("id") String id,
            @Field("title") String title,
            @Field("video_url") String url,
            @Field("privacy") String privacy,
            @Field("tlog_id") Long tlogId);

    /**
     * Получение статьи
     * @param id
     * @param withComments
     * @return
     */
    @GET("entries/{id}.json")
    Observable<Entry> getEntry(@Path("id") Long id,
                               @Query("include_comments") Boolean withComments);

    /**
     * Пожаловатсья на статью
     * @param id
     * @return
     */
    @POST("entries/{id}/report.json")
    Observable<Object> reportEntry(@Path("id") Long id);

    @POST("entries/{entry_id}/votes.json")
    Observable<Rating> vote(@Path("entry_id") long entryId);

    @DELETE("entries/{entry_id}/votes.json")
    Observable<Rating> unvote(@Path("entry_id") long entryId);

    @FormUrlEncoded
    @POST("favorites.json")
    Observable<Object> addToFavorites(@Field("entry_id") long entryId);

    @DELETE("favorites.json")
    Observable<Object> removeFromFavorites(@Query("entry_id") long entryId);


    @POST("embeding/iframely.json")
    Observable<IFramely> getIframely(@Query("url") String url);
}
