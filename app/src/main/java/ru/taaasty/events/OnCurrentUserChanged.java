package ru.taaasty.events;

import ru.taaasty.rest.model.CurrentUser;

/**
 * Вызываетя при смене параметров залогиненного пользователя.
 * Только при обновлении, не при логине/разлогине/и т.п.
 */
public class OnCurrentUserChanged {

    public final CurrentUser user;

    public OnCurrentUserChanged(CurrentUser user) {
        this.user = user;
    }



}
