package ru.taaasty.events;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 20.03.15.
 */
public class MarkAllAsReadRequestCompleted {

    public Throwable exception;

    public MarkAllAsReadRequestCompleted(@Nullable Throwable e) {
        this.exception = e;
    }

}
