package ru.taaasty.rest.model;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 05.09.14.
 */
public class ResponseError {

    /**
     * Код ответа. Совпадает с HTTP статусом.
     *
     * Первичным считаем HTTP статус и это поле стараемся не использовать.
     */
    public int responseCode;

    /**
     * В некоторых случаях текст ошибки для юзера приходит здесь, а не в {@link #error}
     * Например, при валидации формы здесь приходит текст всех ошибок валидации.
     */
    @Nullable
    public String longMessage;

    /**
     * Код ошибки. Например "user_authenticator/user_by_slug_not_found"
     */
    public String errorCode;

    /**
     * Текстовая расшифровка сообщения, которую можно показывать пользователю.
     *
     * По крайней мере, фронтендщики считают, что можно.
     */
    public String error;

    @Override
    public String toString() {
        return "ResponseError{" +
                "longMessage=" + longMessage +
                ", responseCode=" + responseCode +
                ", errorCode='" + errorCode + '\'' +
                ", error='" + error + '\'' +
                '}';
    }
}
