package ru.taaasty.stetho;

import android.content.Context;

import okhttp3.OkHttpClient;

/**
 * Created by alexey on 04.12.15.
 */
public interface StethoHelper {

    void init(Context appContext);

    void configureInterceptor(OkHttpClient.Builder httpClient);
}
