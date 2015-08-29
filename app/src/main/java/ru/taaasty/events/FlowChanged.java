package ru.taaasty.events;

import ru.taaasty.rest.model.Flow;

/**
 * Обновился или добавился поток
 */
public class FlowChanged {

    public final Flow flow;

    public FlowChanged(Flow flow) {
        this.flow = flow;
    }
}
