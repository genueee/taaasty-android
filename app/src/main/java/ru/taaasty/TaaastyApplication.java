package ru.taaasty;

import android.app.Application;
import android.net.http.HttpResponseCache;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.io.IOException;

@ReportsCrashes(
        formKey = "",
        formUri = BuildConfig.ACRA_FORM_URI,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN,
        formUriBasicAuthPassword = BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD,
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false,
        resToastText = R.string.crash_toast_text
)
public class TaaastyApplication extends Application {
    private static final String TAG = "TaaastyApplication";
    private static final long HTTP_CACHE_SIZE = 20 * 1024 * 1024;

    @Override
    public void onCreate() {
        super.onCreate();

        // ACRA мешает при отладке
        if (BuildConfig.ENABLE_ACRA) ACRA.init(this);

        initHttpCache();
    }

    private void initHttpCache() {
        File httpCacheDir = new File(getCacheDir(), "http");
        try {
            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE);
        } catch (IOException e) {
            Log.e(TAG, "error install http cache", e);
        }
    }
}
