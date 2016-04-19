package ru.taaasty.stetho;

import android.content.Context;

import okhttp3.OkHttpClient;


public class DummyStethoHelper implements StethoHelper {
    @Override
    public void init(Context appContext) {
    }

    @Override
    public void configureInterceptor(OkHttpClient.Builder httpClient) {

    }
}
