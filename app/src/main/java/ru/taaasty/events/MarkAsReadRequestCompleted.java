package ru.taaasty.events;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 20.03.15.
 */
public class MarkAsReadRequestCompleted {

    public Throwable exception;

    public MarkAsReadRequestCompleted(@Nullable Throwable e) {
        this.exception = e;
    }

}
