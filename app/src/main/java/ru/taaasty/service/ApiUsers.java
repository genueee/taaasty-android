package ru.taaasty.service;

import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.mime.TypedOutput;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.RecoveryPasswordResponse;
import ru.taaasty.model.RegisterUserResponse;
import ru.taaasty.model.Status;
import ru.taaasty.model.Userpic;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface ApiUsers {

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

    /**
     * Загрузка юзерпика
     * @param file
     * @return
     */
    @Multipart
    @POST("/users/userpic.json")
    Observable<Userpic> uploadUserpic(@Part("file") TypedOutput file);

    /**
     * Загрузка юзерпика
     * @param file
     * @return
     */
    @Multipart
    @POST("/users/userpic.json")
    Userpic uploadUserpicSync(@Part("file") TypedOutput file);

    /**
     * Удаление юзерпика
     * @return
     */
    @DELETE("/users/userpic.json")
    Observable<Status> deleteUserpic();

}
