package ru.taaasty.rest.service;

import retrofit.http.Field;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedOutput;
import ru.taaasty.rest.model.TlogDesign;
import rx.Observable;

/**
 * Created by alexey on 08.08.14.
 */
public interface ApiDesignSettings {

    @GET("/design_settings/{id_or_slug}.json")
    Observable<TlogDesign> getDesignSettings(@Path("id_or_slug") String idOrSlug);

    // XXX
    @PUT("/design_settings/{id_or_slug}.json")
    Observable<Object> putDesignSettings(
            @Path("id_or_slug") String idOrSlug,
            @Field("feedColor") String feedColor,
            @Field("headerColor") String headerColor,
            @Field("fontType") String fontType,
            @Field("coverAlign") String coverAlign,
            @Field("feedOpacity") float feedOpacity
            );

    /**
     * Загрузка бэкграунда
     * @param file
     * @return
     */
    @Multipart
    @POST("/design_settings/{id_or_slug}/cover.json")
    TlogDesign uploadBackgroundSync(@Path("id_or_slug") String idOrSlug, @Part("file") TypedOutput file);
}
