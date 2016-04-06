package ru.taaasty.rest.service;


import retrofit.http.DELETE;
import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.mime.TypedOutput;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.FlowList;
import ru.taaasty.rest.model.FlowStaff;
import rx.Observable;

/**
 * Created by alexey on 29.08.15.
 */
public interface ApiFlows {

    /**
     * Список потоков
     * @param page Страница
     * @param limit Лимит
     * @return Список всех потоков
     */
    @GET("/flows.json")
    Observable<FlowList> getFlows(@Query("page") int page,
                                         @Query("limit") int limit);

    /**
     * Создание потока
     * @param name Название
     * @param title Описание
     * @param flowpic
     * @param staffIds
     * @return
     */
    @Multipart
    @POST("/flows.json")
    Flow createFlowSync(
            @Part("name") String name,
            @Part("title") String title,
            @Part("flowpic") TypedOutput flowpic,
            @Part("staff_ids") int[] staffIds
            );

    /**
     * Список потоков текущего пользователя
     * @param page Страница
     * @param limit Лимит
     * @param exposeStaffs Передавать дополнительно staffs
     * @return Список всех потоков
     */
    @GET("/flows/my.json")
    Observable<FlowList> getMyFlows(@Query("page") int page,
                                  @Query("limit") int limit,
                                  @Query("expose_staffs") Boolean exposeStaffs
    );

    /**
     * Данные о потоке
     */
    @GET("/flows/{id}.json")
    Observable<Flow> getFlow(@Path("id") long id);

    /**
     * Обновление потока
     * @param id
     * @param name
     * @param title
     * @param slug
     * @param flowpic
     * @param isPrivacy
     * @param isPremoderate
     * @return
     */
    @Multipart
    @PUT("/flows/{id}.json")
    Flow updateFlowSync(
            @Path("id") long id,
            @Part("name") String name,
            @Part("title") String title,
            @Part("slug") String slug,
            @Part("flowpic") TypedOutput flowpic,
            @Part("is_privacy") Boolean isPrivacy,
            @Part("is_premoderate") Boolean isPremoderate
    );

    /**
     * Добавление модератора в поток
     */
    @FormUrlEncoded
    @POST("/flows/{id}/staffs.json")
    Observable<Flow> addModerator(@Path("id") long flowId, @Field("user_id") long userId);

    /**
     * Смена роли модератора потока
     * @param flowId ID потока
     * @param userId ID пользователя
     * @param role роль: {@linkplain FlowStaff#ROLE_ADMIN}, {@linkplain FlowStaff#ROLE_MODERATOR}
     * @return
     */
    @FormUrlEncoded
    @PUT("/flows/{id}/staffs.json")
    Observable<Flow> changeModerator(@Path("id") long flowId,
                          @Field("user_id") long userId,
                          @Field("role") @FlowStaff.Role String role);

    /**
     * Удаление модератора из потока
     */
    @FormUrlEncoded
    @DELETE("/flows/{id}/staffs.json")
    Observable<Flow> removeModerator(@Path("id") long flowId, @Field("user_id") long userId);

    /**
     * Получить доступные потоки для поста/репоста
     */
    @GET("/flows/available.json")
    Observable<FlowList> getAvailableFlows(@Query("page") int page, @Query("limit") int limit, @Query("expose_staffs") boolean exposeStaffs);
}
