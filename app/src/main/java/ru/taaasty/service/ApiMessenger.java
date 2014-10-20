package ru.taaasty.service;

import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import ru.taaasty.model.Notification;
import ru.taaasty.model.PusherReadyResponse;
import rx.Observable;

/**
 * Created by alexey on 21.10.14.
 */
public interface ApiMessenger {

    @FormUrlEncoded
    @POST("/messenger/auth.json")
    Response authPusher(@Field("channel_name") String channelName, @Field("socket_id") String socketId);

    @FormUrlEncoded
    @POST("/messenger/ready.json")
    PusherReadyResponse authReady(@Field("socket_id") String socketId);

    @FormUrlEncoded
    @PUT("/messenger/notifications/{id}/read.json")
    Observable<Notification> markNotificationAsRead(@Field("socket_id") String socketId,
                                  @Path("id") long notificationId);

}
