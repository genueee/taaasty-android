package ru.taaasty.rest.model;

/**
 * Created by alexey on 05.09.14.
 */
public class ResponseError {

    public int responseCode;

    public String longMessage;

    public String errorCode;

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
