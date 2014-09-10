package ru.taaasty.service;

import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedOutput;
import ru.taaasty.model.TlogDesign;
import rx.Observable;

/**
 * Created by alexey on 08.08.14.
 */
public interface ApiDesignSettings {

    @GET("/design_settings/{slug}.json")
    Observable<TlogDesign> getDesignSettings(@Path("slug") String slug);

    // XXX
    @PUT("/design_settings/{slug}.json")
    Observable<Object> getDesignSettings(
            @Path("slug") String slug,
            @Field("feedColor") String feedColor,
            @Field("headerColor") String headerColor,
            @Field("fontType") String fontType,
            @Field("coverAlign") String coverAlign,
            @Field("feedOpacity") float feedOpacity
            );

    @Multipart
    @POST("/design_settings/{slug}.json")
    Observable<Object> postCover(
            @Path("slug") String slug,
            TypedFile photo);

    /**
     * Загрузка бэкграунда
     * @param file
     * @return
     */
    @Multipart
    @POST("/design_settings/{slug}/cover.json")
    TlogDesign uploadBackgroundSync(@Path("slug") String slug, @Part("file") TypedOutput file);
}
