package ru.taaasty.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.analytics.StandardExceptionParser;

import java.util.Collection;

import retrofit.RetrofitError;
import ru.taaasty.rest.ResponseErrorException;
import ru.taaasty.rest.UnauthorizedException;

/**
 * Created by alexey on 30.06.15.
 */
public class AnalyticsExceptionParser extends StandardExceptionParser {
    public AnalyticsExceptionParser(Context context, Collection<String> additionalPackages) {
        super(context, additionalPackages);
    }

    @Override
    public String getDescription(String threadName, Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            return getDescription((RetrofitError)throwable, null);
        } else if (throwable.getCause() instanceof RetrofitError) {
            return getDescription((RetrofitError)throwable.getCause(), throwable);
        } else {
            return super.getDescription(threadName, throwable);
        }
    }

    private String getDescription(RetrofitError retrofitError, @Nullable Throwable throwable) {
        String url = NetworkUtils.getUrlApiPath(retrofitError);
        String kind = retrofitError.getKind().name();
        String errorStatus = null;
        String error = null;
        if (throwable instanceof UnauthorizedException) {
            UnauthorizedException uae = (UnauthorizedException) throwable;
            if (uae.error != null) {
                error = uae.error.errorCode;
            }
        } else if (throwable instanceof ResponseErrorException) {
            ResponseErrorException ree = (ResponseErrorException) throwable;
            if (ree.error != null) {
                error = ree.error.errorCode;
            }
        }
        if (retrofitError.getResponse() != null) {
            errorStatus = String.valueOf(retrofitError.getResponse().getStatus());
            if (TextUtils.isEmpty(error)) {
                error = retrofitError.getResponse().getReason();
            }
        }
        if (TextUtils.isEmpty(error)) {
            error = retrofitError.getMessage();
        }
        return String.format("Net %s: %s %.56s; %s", kind,
                (errorStatus == null ? "" : errorStatus),
                (error == null ? "" : error), url);
    }
}
