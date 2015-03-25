package ru.taaasty;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.aviary.android.feather.sdk.IAviaryClientCredentials;
import com.aviary.android.feather.sdk.utils.AviaryIntentConfigurationValidator;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import java.util.Locale;

import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends MultiDexApplication implements IAviaryClientCredentials {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    private volatile Tracker mAnalyticsTracker;

    private volatile boolean mInterSessionStarted;

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

        if (BuildConfig.DEBUG) {
            try {
                AviaryIntentConfigurationValidator.validateConfiguration(this);
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalStateException("aviary validatuion error", e);
            }
        }
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
            if (!"release".equals(BuildConfig.BUILD_TYPE)) {
                analytics.setDryRun(true);
                analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            } else {
                analytics.getLogger().setLogLevel(Logger.LogLevel.ERROR);
            }
            analytics.setLocalDispatchPeriod(1000);
            mAnalyticsTracker = analytics.newTracker(R.xml.app_tracker);
        }
        return mAnalyticsTracker;
    }

        private void resetLanguage() {
        Configuration config = getBaseContext().getResources().getConfiguration();

        // Locale.US - это обычно юзеры, не меняющие язык. Подстраиваемся под них
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

    public void sendAnalyticsEvent(String category, String action, @Nullable String label) {
        HitBuilders.EventBuilder eb = new HitBuilders.EventBuilder(category, action);
        if (label != null) eb.setLabel(label);
        getTracker().send(eb.build());
    }

    public synchronized void startIntercomSession() {
        if (mInterSessionStarted) return;
        Long userId = UserManager.getInstance().getCurrentUserId();
        if (userId == null) return;
    }

    @Override
    public String getClientID() {
        return getResources().getString(R.string.adobe_client_id);
    }

    @Override
    public String getClientSecret() {
        return getResources().getString(R.string.adobe_client_secret);
    }

    @Override
    public String getBillingKey() {
        return "";
    }
}
