package ru.taaasty;

import android.app.Application;
import android.net.http.HttpResponseCache;
import android.util.Log;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import java.io.File;
import java.io.IOException;

import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

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
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // ACRA мешает при отладке
        if (BuildConfig.ENABLE_ACRA) ACRA.init(this);

        CalligraphyConfig.initDefault("fonts/ProximaNova-Reg.otf", R.attr.fontPath);

        UserManager.getInstance().onAppInit(this);

        initHttpCache();
    }

    private void initHttpCache() {
        File cacheDir = getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = getCacheDir();
        }

        File httpCacheDir = new File(cacheDir, "taaasty");
        try {
            long cacheSize = NetworkUtils.calculateDiskCacheSize(httpCacheDir);
            if (DBG) Log.v(TAG, "cache size, mb: " + cacheSize / 1024/ 1024);
            HttpResponseCache.install(httpCacheDir, cacheSize);
        } catch (IOException e) {
            Log.e(TAG, "error install http cache", e);
        }
    }
}
