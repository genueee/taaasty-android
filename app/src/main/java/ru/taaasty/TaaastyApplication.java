package ru.taaasty;

import android.app.Application;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import java.util.Locale;

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
        ImageUtils.getInstance().onAppInit(this);
        VkontakteHelper.getInstance().onAppInit();
        getTracker();
        resetLanguage();
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

        private void resetLanguage() {
        Configuration config = getBaseContext().getResources().getConfiguration();

        // Locale.US - это обычно юзеры, не делающие менять язык. Подстраиваемся под них
        if (config.locale == null
                || (config.locale.equals(Locale.US))
                || (config.locale.equals(Locale.ROOT))) {
            Locale locale = new Locale("ru_RU");
            Log.i(TAG, "Reset locale " + config.locale + " to " + locale);
            Locale.setDefault(locale);
            config.locale = locale;
            getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
        }
    }


}
