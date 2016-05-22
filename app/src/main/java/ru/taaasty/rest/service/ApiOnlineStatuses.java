package ru.taaasty.rest.service;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.UserStatusInfo;
import rx.Observable;

public interface ApiOnlineStatuses {

    @GET("online_statuses.json")
    Observable<List<UserStatusInfo>> getUserInfo(@Query("user_ids") String userIds);



}
