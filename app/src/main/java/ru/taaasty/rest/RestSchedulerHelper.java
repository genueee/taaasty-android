package ru.taaasty.rest;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class RestSchedulerHelper {

    public static Scheduler getScheduler() {
        return Schedulers.io();
    }
}
