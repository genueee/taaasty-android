package ru.taaasty.service;

import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.http.Query;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.Relationships;
import ru.taaasty.model.RelationshipsSummary;
import rx.Observable;

public interface ApiRelationships {

    @GET("/relationships/summary.json")
    Observable<RelationshipsSummary> getRelationshipsSummary();

    /**
     *
     * @param path  {@link ru.taaasty.model.Relationship#RELATIONSHIP_FRIEND},
     *  {@link ru.taaasty.model.Relationship#RELATIONSHIP_GUESSED}
     *  {@link ru.taaasty.model.Relationship#RELATIONSHIP_IGNORED}
     *  {@link ru.taaasty.model.Relationship#RELATIONSHIP_REQUESTED}
     * @param sincePosition
     * @param limit
     * @return Список моих отношений. Отсортированы по position.
     */
    @GET("/relationships/to/{type}.json")
    Observable<Relationships> getRelationshipsTo(
            @Path("type") String path,
            @Query("since_position") Integer sincePosition,
            @Query("limit") Integer limit
    );

    /**
     *
     * @param idOrSlug
     * @return Узнать, какое у меня с ним отношение
     */
    @GET("/relationships/to/tlog/{id_or_slug}.json")
    Observable<Relationships> getRelationshipsToTlog(
            @Path("id_or_slug") String idOrSlug);


    @POST("/relationships/to/tlog/{id_or_slug}/follow.json")
    Observable<Relationship> follow(
            @Path("id_or_slug") String idOrSlug);

    @POST("/relationships/to/tlog/{id_or_slug}/unfollow.json")
    Observable<Relationship> unfollow(
            @Path("id_or_slug") String idOrSlug);

    @POST("/relationships/to/tlog/{id_or_slug}/ignore.json")
    Observable<Relationship> ignore(
            @Path("id_or_slug") String idOrSlug);

    @POST("/relationships/to/tlog/{id_or_slug}/cancel.json")
    Observable<Relationship> cancel(
            @Path("id_or_slug") String idOrSlug);

    /**
     *
     * @param path {@link ru.taaasty.model.Relationship#RELATIONSHIP_FRIEND},
     *  {@link ru.taaasty.model.Relationship#RELATIONSHIP_GUESSED}
     *  {@link ru.taaasty.model.Relationship#RELATIONSHIP_REQUESTED}
     * @param sincePosition
     * @param limit
     * @return
     */
    @GET("/relationships/by/{type}.json")
    Observable<Relationships> getRelationshipsBy(
            @Path("type") String path,
            @Query("since_position") Integer sincePosition,
            @Query("limit") Integer limit
    );

    /**
     * Узнать какое у него со мной отношение
     * @param idOrSlug
     * @return
     */
    @GET("/relationships/by/tlog/{id_or_slug}.json")
    Observable<Relationship> getRelationshipsByTlog(
            @Path("id_or_slug") String idOrSlug);

    @POST("/relationships/by/tlog/{id_or_slug}/approve.json")
    Observable<Relationship> approveTlogRelationship(
            @Path("id_or_slug") String idOrSlug);

    @POST("/relationships/by/tlog/{id_or_slug}/disapprove.json")
    Observable<Relationship> disapproveTlogRelationship(
            @Path("id_or_slug") String idOrSlug);

    @DELETE("/relationships/by/tlog/{id_or_slug}.json")
    Observable<Relationship> unsubscribe(@Path("id_or_slug") String idOrSlug);

    @GET("/relationships/by/requested.json")
    Observable<Relationships> getRelationshipsRequested(
            @Query("since_position") Integer sincePosition,
            @Query("limit") Integer limit,
            @Query("expose_reverse") Boolean exposeReverse
    );

}
