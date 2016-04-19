package ru.taaasty.rest.service;


import okhttp3.MultipartBody;
import retrofit2.http.Field;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import ru.taaasty.rest.model.TlogDesign;
import rx.Observable;

/**
 * Created by alexey on 08.08.14.
 */
public interface ApiDesignSettings {

    @GET("design_settings/{id_or_slug}.json")
    Observable<TlogDesign> getDesignSettings(@Path("id_or_slug") String idOrSlug);

    // XXX
    @PUT("design_settings/{id_or_slug}.json")
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
    @POST("design_settings/{id_or_slug}/cover.json")
    Observable<TlogDesign> uploadBackgroundSync(@Path("id_or_slug") String idOrSlug, @Part MultipartBody.Part file);//("file")
}
