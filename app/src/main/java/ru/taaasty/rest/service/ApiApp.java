package ru.taaasty.rest.service;

import retrofit.http.GET;
import retrofit.http.Query;
import ru.taaasty.rest.model.AppVersionResponse;
import ru.taaasty.rest.model.Stats;
import rx.Observable;

public interface ApiApp {

    /**
     * Статистика по фидам и пользователям
     */
    @GET("/app/stats.json")
    Observable<Stats> getStats();

    @GET("/app/check_version.json")
    Observable<AppVersionResponse> checkVersion(@Query("app_name") String appName,
                                                @Query("app_version") String appVersion);
}
