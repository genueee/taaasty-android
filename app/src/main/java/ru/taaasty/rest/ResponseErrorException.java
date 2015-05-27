package ru.taaasty.rest;

import android.support.annotation.Nullable;

import ru.taaasty.rest.model.ResponseError;

/**
 * Ошибка, которая парсится по ResponseError
 */
public class ResponseErrorException extends  RuntimeException {

    public final ResponseError error;

    public ResponseErrorException(Throwable throwable, ResponseError error) {
        super(throwable);
        this.error = error;
    }

    @Nullable
    public String getUserError() {
        return error == null ? null : error.error;
    }
}
