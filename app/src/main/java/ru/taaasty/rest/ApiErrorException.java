package ru.taaasty.rest;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import java.io.IOException;

import ru.taaasty.R;
import ru.taaasty.rest.model.ResponseError;

/**
 * Ошибка от API
 */
public class ApiErrorException extends IOException {

    /**
     * Объект ошибки с сервера.
     * Может быть null, если ошибка была не на уровне HTTP, либо если сервер по каким-то причинам
     * вернул ошибку в другом формате
     */
    @Nullable
    public final ResponseError error;

    public ApiErrorException(String detailMessage,@Nullable ResponseError error) {
        super(detailMessage);
        this.error = error;
    }
    public ApiErrorException(String detailMessage,@Nullable Throwable throwable) {
        super(detailMessage,throwable);
        error = null;
    }

    public ApiErrorException(Throwable throwable) {
        super(throwable);
        error = null;
    }

    /**
     * Описание сообщения об ошибке, которое можно показать пользователю
     */
    public String getErrorUserMessage(@Nullable Resources resources) {
        return getErrorUserMessage(resources, R.string.server_error);
    }

    /**
     * Описание сообщения об ошибке, которое можно показать пользователю
     */
    public String getErrorUserMessage(@Nullable Resources resources, @StringRes int fallbackTextResId) {
        String fallbackText;
        if (resources != null) {
            fallbackText = resources.getString(fallbackTextResId);
        } else {
            fallbackText = "Server error";
        }

        return getErrorUserMessage(resources, fallbackText);
    }

    /**
     * Описание сообщения об ошибке, которое можно показать пользователю
     */
    public String getErrorUserMessage(@Nullable Resources resources, String fallbackText) {
        if (fallbackText == null) throw new IllegalArgumentException();

        if (error != null) {
            if (!TextUtils.isEmpty(error.longMessage)) return error.longMessage;
            if (!TextUtils.isEmpty(error.error)) return error.error;
        }

        if (isErrorUnauthorized()) {
            if (resources != null) {
                return resources.getString(R.string.error_invalid_email_or_password);
            } else {
                return "Invalid email or password";
            }
        }
        return fallbackText;
    }

    /**
     * @return HTTP status code. -1 при ошибке до уровня HTTP (код не был получен)
     */
    public int getHttpStatusCode() {
        if (error!=null){
            return error.responseCode;
        }
        return -1;
    }

    public boolean isError403Forbidden() {
        return getHttpStatusCode() == 403;
    }

    public boolean isError404NotFound() {
        return getHttpStatusCode() == 404;
    }

    /**
     * @return нет токена, т.е. требуется авторизация где бытаемя под анонимным пользователем
     */
    public boolean isErrorAuthorizationRequired() {
        return (error != null)
                && (getHttpStatusCode() == 417)
                && "no_token".equals(error.errorCode);
    }

    public boolean isErrorUnauthorized() {
        switch (getHttpStatusCode()) {
            case 401:
                return true;
            case 417:
                return error != null && "no_token".equals(error.errorCode);
            default:
                return false;
        }
    }


}
