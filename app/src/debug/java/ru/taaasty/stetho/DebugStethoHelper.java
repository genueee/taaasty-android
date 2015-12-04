package ru.taaasty.stetho;

import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp.StethoInterceptor;
import com.squareup.okhttp.OkHttpClient;

/**
 * Created by alexey on 04.12.15.
 */
public final class DebugStethoHelper implements StethoHelper {
    @Override
    public void init(Context appContext) {
        Stetho.initializeWithDefaults(appContext);
    }

    @Override
    public void configureInterceptor(OkHttpClient httpClient) {
        httpClient.networkInterceptors().add(new StethoInterceptor());
    }
}
