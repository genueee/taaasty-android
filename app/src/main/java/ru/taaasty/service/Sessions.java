package ru.taaasty.service;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import ru.taaasty.model.CurrentUser;
import rx.Observable;

/**
 * Created by alexey on 11.07.14.
 */
public interface Sessions {

    /**
     * Логин
     * @param login
     * @param spassword
     * @return
     */
    @FormUrlEncoded
    @POST("/sessions.json")
    Observable<CurrentUser> signIn(@Field("email") String login, @Field("password") String spassword);

}
