package ru.taaasty.stetho;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;

/**
 * Created by alexey on 04.12.15.
 */
public interface StethoHelper {

    void init(Context appContext);

    void configureInterceptor(OkHttpClient httpClient);
}
