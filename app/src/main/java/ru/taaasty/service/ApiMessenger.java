package ru.taaasty.service;

import java.util.List;

import retrofit.client.Response;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.ConversationMessages;
import ru.taaasty.model.Notification;
import ru.taaasty.model.PusherReadyResponse;
import ru.taaasty.model.Status;
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
    Observable<PusherReadyResponse> authReady(@Field("socket_id") String socketId);

    @FormUrlEncoded
    @PUT("/messenger/notifications/{id}/read.json")
    Observable<Notification> markNotificationAsRead(@Field("socket_id") String socketId,
                                  @Path("id") long notificationId);

    @GET("/messenger/conversations.json")
    Observable<List<Conversation>> getConversations(@Query("socket_id") String socketId);

    @FormUrlEncoded
    @POST("/messenger/conversations/by_user_id/{user_id}.json")
    Observable<Conversation> createConversation(@Field("socket_id") String socketId,
                                                           @Path("user_id") long slug);

    @GET("/messenger/conversations/by_id/{id}/messages.json")
    Observable<ConversationMessages> getMessages(@Query("socket_id") String socketId,
                                                           @Path("id") long conversationId,
                                                           @Query("from_message_id") Long fromMessageId,
                                                           @Query("to_message_id") Long toMessageId,
                                                           @Query("limit") Integer limit,
                                                           @Query("order") String order);

    /**
     * Отправка сообщения в переписку
     * @param socketId не обязателен
     * @param conversationId ID переписки.
     * @param content Содержимое собщения
     * @param uuid UUID сообщения, сгенерированный на стороне клиента
     * @param recipientId ID получателя. По умолчанию тот, что в беседе
     * @return
     */
    @FormUrlEncoded
    @POST("/messenger/conversations/by_id/{id}/messages.json")
    Observable<Conversation.Message> postMessage(@Field("socket_id") String socketId,
                                                        @Path("id") long conversationId,
                                                        @Query("content") String content,
                                                        @Query("uuid") String uuid,
                                                        @Query("recipient_id") Long recipientId);


    /**
     * Отмечает как прочитанные указанные сообщения
     * @param socketId не обязателен
     * @param conversationId ID переписки.
     * @param messageIds ID сообщений. Строка, через запятую.
     * @return
     */
    @FormUrlEncoded
    @PUT("/messenger/conversations/by_id/{id}/messages/read.json")
    Observable<Status.MarkMessagesAsRead> markMessagesAsRead(@Field("socket_id") String socketId,
                                                    @Path("id") long conversationId,
                                                    @Field("ids") String messageIds
                                                    );

}
