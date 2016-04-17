package ru.taaasty.events;

import ru.taaasty.rest.model2.Stats;

/**
 * Created by alexey on 09.12.14.
 */
public class OnStatsLoaded {

    public final Stats stats;

    public OnStatsLoaded(Stats stats) {
        this.stats = stats;
    }

}
