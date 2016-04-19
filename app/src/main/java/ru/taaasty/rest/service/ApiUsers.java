package ru.taaasty.rest.service;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.RecoveryPasswordResponse;
import ru.taaasty.rest.model.Status;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.Userpic;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface ApiUsers {

    @GET("users/me.json")
    Observable<CurrentUser> getMyInfo();

    /**
     * Регистрация пользователя
     * @param email
     * @param password
     * @param slug
     * @return
     */
    @FormUrlEncoded
    @POST("users.json")
    Observable<CurrentUser> registerUser(@Field("email") String email,
                                         @Field("password") String password,
                                         @Field("slug") String slug);

    /**
     * Регистрация пользователя vkontakte
     * @return
     */
    @FormUrlEncoded
    @POST("users/vkontakte.json")
    Observable<CurrentUser> registerUserVkontakte(@Field("token") String token,
                                         @Field("nickname") String nickname,
                                         @Field("avatar_url") String avatar_url,
                                         @Field("name") String name,
                                         @Field("first_name") String firstName,
                                         @Field("second_name") String secondName,
                                         @Field("sex") Integer sex
                                         );

    /**
     * Регистрация пользователя facebook
     * @return
     */
    @FormUrlEncoded
    @POST("users/facebook.json")
    Observable<CurrentUser> registerUserFacebook(@Field("token") String token,
                                                  @Field("nickname") String nickname,
                                                  @Field("avatar_url") String avatar_url,
                                                  @Field("name") String name,
                                                  @Field("first_name") String firstName,
                                                  @Field("second_name") String secondName,
                                                  @Field("email") String email,
                                                  @Field("sex") Integer sex
    );

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMySlug(@Field("slug") String slug);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyTitle(@Field("title") String title);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyEmail(@Field("email") String email);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyPassword(@Field("password") String password);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyIsPrivacy(@Field("is_privacy") boolean isPrivacy);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyIsDaylog(@Field("is_daylog") boolean isDaylog);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyAvailableNotifications(@Field("available_notifications") boolean availableNotifications);

    @FormUrlEncoded
    @PUT("users.json")
    Observable<CurrentUser> setMyIsFemale(@Field("is_female") boolean is_female);

    /**
     * Забыл пароль. Просьба выслать на емайл
     * @param slugOrEmail Емайл или ник
     * @return
     */
    @FormUrlEncoded
    @POST("users/password/recovery.json")
    Observable<RecoveryPasswordResponse> recoveryPassword(@Field("slug_or_email") String slugOrEmail);

    /**
     * Загрузка юзерпика
     * @param file
     * @return
     */
    @Multipart
    @POST("users/userpic.json")
    Observable<Userpic> uploadUserpic(@Part("file") MultipartBody.Part file);

    /**
     * Загрузка юзерпика
     * @param file
     * @return
     */
    @Multipart
    @POST("users/userpic.json")
    Observable<Userpic> uploadUserpicSync(@Part MultipartBody.Part file);//("file")

    /**
     * Удаление юзерпика
     * @return
     */
    @DELETE("users/userpic.json")
    Observable<Status> deleteUserpic();

    /**
     * Список пользователей, имена которых включают запрос
     * @param query
     * @param limit
     * @return
     */
    @GET("users/predict.json")
    Observable<List<User>> predict(@Query("query") String query, @Query("limit") Integer limit);

}
