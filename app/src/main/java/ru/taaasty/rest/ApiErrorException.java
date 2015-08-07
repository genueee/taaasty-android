package ru.taaasty.rest;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.TextUtils;

import retrofit.RetrofitError;
import ru.taaasty.R;
import ru.taaasty.rest.model.ResponseError;

/**
 * Ошибка от API
 */
public class ApiErrorException extends RuntimeException {

    /**
     * Объект ошибки с сервера.
     * Может быть null, если ошибка была не на уровне HTTP, либо если сервер по каким-то причинам
     * вернул ошибку в другом формате
     */
    @Nullable
    public final ResponseError error;

    public ApiErrorException(Throwable throwable, @Nullable ResponseError error) {
        super(throwable);
        this.error = error;
    }

    public RetrofitError getRetrofitError() {
        return (RetrofitError) getCause();
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
        RetrofitError ree = (RetrofitError)getCause();
        if (getRetrofitError().getKind() != RetrofitError.Kind.HTTP) return -1;
        return ree.getResponse().getStatus();
    }

    /**
     * @return Сетевая ошибка (HTTP ответ обычно не получен)
     */
    public boolean isNetworkError() {
        return getRetrofitError().getKind() == RetrofitError.Kind.NETWORK;
    }

    public boolean isError403Forbidden() {
        return getHttpStatusCode() == 403;
    }

    public boolean isError404NotFound() {
        return getHttpStatusCode() == 404;
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
