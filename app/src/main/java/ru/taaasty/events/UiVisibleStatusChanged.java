package ru.taaasty.events;

/**
 * Created by alexey on 31.10.15.
 */
public class UiVisibleStatusChanged {

    public final int activeActivitiesCount;

    public UiVisibleStatusChanged(int activitiesCount) {
        this.activeActivitiesCount = activitiesCount;
    }

}
