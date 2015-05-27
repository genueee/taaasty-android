package ru.taaasty.events;

import ru.taaasty.rest.model.Relationship;

/**
 * Рассылаетмя при любом изменении relationship
 */
public class RelationshipChanged {

    public final Relationship relationship;

    public RelationshipChanged(Relationship relationsip) {
        this.relationship = relationsip;
    }

}
