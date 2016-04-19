package ru.taaasty.rest.service;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import ru.taaasty.rest.model.User;
import rx.Observable;

/**
 * Created by alexey on 12.05.15.
 */
public interface ApiDevice {

    @FormUrlEncoded
    @POST("devices/android.json")
    Observable<User> register(@Field("token") String token);

    @FormUrlEncoded
    @DELETE("devices/android.json")
    Call<ResponseBody> unregister(@Field("token") String token);

}
