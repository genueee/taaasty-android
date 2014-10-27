package ru.taaasty.events;

import ru.taaasty.model.Relationship;

/**
 * Рассылаетмя при любом изменении relationship
 */
public class RelationshipChanged {

    public final Relationship relationship;

    public RelationshipChanged(Relationship relationsip) {
        this.relationship = relationsip;
    }

}
