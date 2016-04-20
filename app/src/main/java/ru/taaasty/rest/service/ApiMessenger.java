package ru.taaasty.rest.service;

import java.util.List;
import java.util.Map;

import retrofit.client.Response;
import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.PartMap;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedOutput;
import ru.taaasty.rest.model.MarkNotificationsAsReadResponse;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.NotificationList;
import ru.taaasty.rest.model.Status;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.MessageList;
import rx.Observable;

/**
 * Created by alexey on 21.10.14.
 */
public interface ApiMessenger {

    @FormUrlEncoded
    @POST("/messenger/auth.json")
    Response authPusher(@Field("channel_name") String channelName, @Field("socket_id") String socketId);

    @FormUrlEncoded
    @POST("/messenger/only_ready.json")
    Observable<Void> authReady2(@Field("socket_id") String socketId);

    @GET("/messenger/notifications.json")
    Observable<NotificationList> getNotifications(@Query("socket_id") String socketId,
                                                 @Query("from_notification_id") Long fromMessageId,
                                                 @Query("to_notification_id") Long toMessageId,
                                                 @Query("limit") Integer limit,
                                                 @Query("order") String order);

    @GET("/messenger/notifications.json")
    NotificationList getNotificationsSync(@Query("socket_id") String socketId,
                                                  @Query("from_notification_id") Long fromMessageId,
                                                  @Query("to_notification_id") Long toMessageId,
                                                  @Query("limit") Integer limit,
                                                  @Query("order") String order);


    @FormUrlEncoded
    @PUT("/messenger/notifications/{id}/read.json")
    Notification markNotificationAsReadSync(@Field("socket_id") String socketId,
                                            @Path("id") long notificationId);

    @FormUrlEncoded
    @PUT("/messenger/notifications/{id}/read.json")
    Observable<Notification> markNotificationAsRead(@Field("socket_id") String socketId,
                                            @Path("id") long notificationId);

    @FormUrlEncoded
    @POST("/messenger/notifications/read.json")
    List<MarkNotificationsAsReadResponse> markAllNotificationsAsRead(
            @Field("socket_id") String socketId,
            @Field("last_id") Long lastId);


    @GET("/messenger/conversations.json")
    Observable<List<Conversation>> getConversations(@Query("socket_id") String socketId);

    @FormUrlEncoded
    @POST("/messenger/conversations/by_user_id/{user_id}.json")
    Observable<Conversation> createConversation(@Field("socket_id") String socketId,
                                                           @Path("user_id") long slug);


    @GET("/messenger/conversations/by_id/{id}.json")
    Observable<Conversation> getConversation(@Path("id") long id);

    @GET("/messenger/conversations/by_id/{id}.json")
    Conversation getConversationSync(@Path("id") long id);

    @GET("/messenger/conversations/by_id/{id}/messages.json")
    Observable<MessageList> getMessages(@Query("socket_id") String socketId,
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
    Observable<Message> postMessage(@Field("socket_id") String socketId,
                                    @Path("id") long conversationId,
                                    @Query("content") String content,
                                    @Query("uuid") String uuid,
                                    @Query("recipient_id") Long recipientId
                                    );

    /**
     * Отправка сообщения в переписку
     * @param socketId не обязателен
     * @param conversationId ID переписки.
     * @param content Содержимое собщения
     * @param uuid UUID сообщения, сгенерированный на стороне клиента
     * @param recipientId ID получателя. По умолчанию тот, что в беседе
     * @return
     */
    @Multipart
    @POST("/messenger/conversations/by_id/{id}/messages.json")
    Observable<Message> postMessageWithAttachments(@Part("socket_id") String socketId,
                                    @Path("id") long conversationId,
                                    @Part("content") String content,
                                    @Part("uuid") String uuid,
                                    @Part("recipient_id") Long recipientId,
                                    @PartMap Map<String,TypedOutput> Files
                                    );


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
    Message postMessageSync(@Field("socket_id") String socketId,
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

    /**
     * Отмечает как прочитанные указанные сообщения
     * @param socketId не обязателен
     * @param conversationId ID переписки.
     * @param messageIds ID сообщений. Строка, через запятую.
     * @return
     */
    @FormUrlEncoded
    @PUT("/messenger/conversations/by_id/{id}/messages/read.json")
    Status.MarkMessagesAsRead markMessagesAsReadSync(@Field("socket_id") String socketId,
                                                             @Path("id") long conversationId,
                                                             @Field("ids") String messageIds
    );


    /**
     * Создание группового чата - обсуждения записи
     * @param entryId ID записи
     * @return
     */
    @FormUrlEncoded
    @POST("/messenger/conversations/by_entry_id.json")
    Observable<Conversation> createGroupConversationByEntry(@Field("socket_id") String socketId,
                                                       @Field("id") long entryId);

    /**
     * Создание группового чата с указанными пользователями
     * @param ids список участников группы через запятую
     */
    @Multipart
    @POST("/messenger/conversations/by_user_ids.json")
    Observable<Conversation> createGroupConversation(@Part("socket_id") String socketId,
                                                     @Part("topic") String topic,
                                                     @Part("ids") String ids,
                                                     @Part("avatar") TypedOutput avatar,
                                                     @Part("background_image") TypedOutput background
    );

    /**
     * Изменение группового чата
     */
    @Multipart
    @POST("/messenger/conversations/by_id/{conv_id}.json")
    Observable<Conversation> editGroupConversation(@Path("conv_id") String conv_id,
                                                     @Part("socket_id") String socketId,
                                                     @Part("topic") String topic,
                                                     @Part("ids") String ids,
                                                     @Part("avatar") TypedOutput avatar,
                                                     @Part("background_image") TypedOutput background
    );

    @DELETE("/messenger/conversations/by_id/{conv_id}.json")
    Observable<Object> deleteConversation(@Path("conv_id") String conv_id,
                                          @Query("socket_id") String socketId);

    @POST("/messenger/conversations/by_id/{conv_id}/not_disturb.json")
    Observable<Conversation> doNotDisturbTurnOn(@Path("conv_id") long conv_id, @Query("socket_id") String socketId);

    @DELETE("/messenger/conversations/by_id/{conv_id}/not_disturb.json")
    Observable<Conversation> doNotDisturbTurnOff(@Path("conv_id") long conv_id, @Query("socket_id") String socketId);
}
