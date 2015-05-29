package ru.taaasty.rest.service;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import ru.taaasty.rest.model.CurrentUser;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface ApiSessions {

    /**
     * Логин
     * @param login
     * @param password
     * @return
     */
    @FormUrlEncoded
    @POST("/sessions.json")
    Observable<CurrentUser> signIn(@Field("email") String login, @Field("password") String password);

    @FormUrlEncoded
    @POST("/sessions/vkontakte.json")
    Observable<CurrentUser> signInVkontakte(@Field("user_id") String userId, @Field("token") String token);

    @FormUrlEncoded
    @POST("/sessions/facebook.json")
    Observable<CurrentUser> signInFacebook(@Field("user_id") String userId, @Field("token") String token);

}
