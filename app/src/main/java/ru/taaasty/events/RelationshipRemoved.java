package ru.taaasty.events;

import android.support.annotation.Nullable;

import ru.taaasty.rest.model.Relationship;

/**
 * Created by alexey on 23.03.15.
 */
public class RelationshipRemoved {

    /**
     * Старый id. Может быть null, если на момент иизменения он неизвестен
     */
    @Nullable
    public final Long id;

    /**
     * Новый relationship с id=null
     */
    public final Relationship relationship;

    public RelationshipRemoved(Relationship relationship) {
        this.id = null;
        this.relationship = relationship;
    }

    public RelationshipRemoved(long id, Relationship relationship) {
        this.id = id;
        this.relationship = relationship;
    }

}
