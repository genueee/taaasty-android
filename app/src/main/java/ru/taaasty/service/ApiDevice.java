package ru.taaasty.service;

import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import ru.taaasty.model.User;

/**
 * Created by alexey on 12.05.15.
 */
public interface ApiDevice {

    @FormUrlEncoded
    @POST("/devices/android.json")
    User register(@Field("token") String token);

    @FormUrlEncoded
    @DELETE("/devices/android.json")
    void unregister(@Field("token") String token);

}
