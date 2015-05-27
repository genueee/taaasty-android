package ru.taaasty.rest;

import android.support.annotation.Nullable;

import ru.taaasty.rest.model.ResponseError;

/**
 * 401 код
 */
public class UnauthorizedException extends RuntimeException {

    @Nullable
    public final ResponseError error;

    public UnauthorizedException(Throwable throwable, ResponseError error) {
        super(throwable);
        this.error = error;
    }

    @Nullable
    public String getUserError() {
        return error == null ? null : error.error;
    }
}
