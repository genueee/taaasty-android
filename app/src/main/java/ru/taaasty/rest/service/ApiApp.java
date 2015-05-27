package ru.taaasty.rest.service;

import retrofit.http.GET;
import ru.taaasty.rest.model.Stats;
import rx.Observable;

public interface ApiApp {

    /**
     * Статистика по фидам и пользователям
     */
    @GET("/app/stats.json")
    Observable<Stats> getStats();
}
