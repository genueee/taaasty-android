package ru.taaasty;

import android.app.Application;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends Application {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    private Tracker mAnalyticsTracker;

    @Override
    public void onCreate() {
        if ("debug".equals(BuildConfig.BUILD_TYPE)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    // .penaltyDeath()
                    .build());
        }
        super.onCreate();

        CalligraphyConfig.initDefault(FontManager.FONT_SYSTEM_DEFAULT_PATH, R.attr.fontPath);

        FontManager.onAppInit(this);
        UserManager.getInstance().onAppInit(this);
        NetworkUtils.getInstance().onAppInit(this);
        ImageUtils.getInstance().onAppInit();
        VkontakteHelper.getInstance().onAppInit();
        getTracker();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DBG) Log.v(TAG, "onTrimMemory() " + level);
        NetworkUtils.getInstance().onTrimMemory();
        if (level >=  TRIM_MEMORY_UI_HIDDEN) {
            // Интерфейс свернут. Не держим сервис без необходимости
            // PusherService.stopPusher(this);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (DBG) Log.v(TAG, "onLowMemory() ");
        NetworkUtils.getInstance().onTrimMemory();
    }

    public synchronized Tracker getTracker() {
        if (mAnalyticsTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mAnalyticsTracker = analytics.newTracker(R.xml.app_tracker);
        }
        return mAnalyticsTracker;
    }

}
