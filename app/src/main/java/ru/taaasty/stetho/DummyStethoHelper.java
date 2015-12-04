package ru.taaasty.stetho;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;


public class DummyStethoHelper implements StethoHelper {
    @Override
    public void init(Context appContext) {
    }

    @Override
    public void configureInterceptor(OkHttpClient httpClient) {
    }
}
