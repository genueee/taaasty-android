package ru.taaasty.service;

import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedFile;
import ru.taaasty.model.Entry;
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
}
