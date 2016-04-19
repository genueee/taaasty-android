package ru.taaasty.stetho;

import android.content.Context;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import java.util.ListIterator;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;

/**
 * Created by alexey on 04.12.15.
 */
public final class DebugStethoHelper implements StethoHelper {
    @Override
    public void init(Context appContext) {
        Stetho.initializeWithDefaults(appContext);
    }

    @Override
    public void configureInterceptor(OkHttpClient.Builder httpClient) {
        ListIterator<Interceptor> it = httpClient.networkInterceptors().listIterator();
        while (it.hasNext()) {
            Interceptor interceptor = it.next();
            if (interceptor instanceof StethoInterceptor) it.remove();
        }

        httpClient.addNetworkInterceptor(new StethoInterceptor());
    }
}
