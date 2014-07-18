package ru.taaasty.service;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.RecoveryPasswordResponse;
import ru.taaasty.model.RegisterUserResponse;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface Users {

    @GET("/users/me.json")
    Observable<CurrentUser> getMyInfo();

    /**
     * Регистрация пользователя
     * @param email
     * @param password
     * @param slug
     * @return
     */
    @FormUrlEncoded
    @POST("/users.json")
    Observable<RegisterUserResponse> regiserUser(@Field("email") String email,
                                                 @Field("password") String password,
                                                 @Field("slug") String slug);

    /**
     * Забыл пароль. Просьба выслать на емайл
     * @param slugOrEmail Емайл или ник
     * @return
     */
    @FormUrlEncoded
    @POST("/users/password/recovery.json")
    Observable<RecoveryPasswordResponse> recoveryPassword(@Field("slug_or_email") String slugOrEmail);

}
