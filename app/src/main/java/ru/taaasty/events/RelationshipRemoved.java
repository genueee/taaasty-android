package ru.taaasty.events;

import ru.taaasty.model.Relationship;

/**
 * Created by alexey on 23.03.15.
 */
public class RelationshipRemoved {

    /**
     * Старый id
     */
    public final long id;

    /**
     * Новый relationship с id=null
     */
    public final Relationship relationship;

    public RelationshipRemoved(long id, Relationship relationship) {
        this.id = id;
        this.relationship = relationship;
    }

}
