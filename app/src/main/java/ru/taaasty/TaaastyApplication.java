package ru.taaasty;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.aviary.android.feather.sdk.IAviaryClientCredentials;
import com.aviary.android.feather.sdk.utils.AviaryIntentConfigurationValidator;
import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.vk.sdk.VKSdk;

import java.util.Locale;

import de.greenrobot.event.EventBus;
import frenchtoast.FrenchToast;
import io.intercom.android.sdk.Intercom;
import io.intercom.android.sdk.identity.Registration;
import io.intercom.android.sdk.preview.IntercomPreviewPosition;
import ru.taaasty.events.UiVisibleStatusChanged;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.GcmUtils;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import rx.functions.Action1;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends MultiDexApplication implements IAviaryClientCredentials {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    private volatile boolean mInterSessionStarted;

    private int mActiveActivitiesCount = 0;

    @Override
    public void onCreate() {
        if ("debug".equals(BuildConfig.BUILD_TYPE)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .permitDiskReads()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .detectCustomSlowCalls()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    // .penaltyDeath()
                    .build());
        }
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            try {
                EventBus.builder()
                        .throwSubscriberException(BuildConfig.DEBUG)
                        .logNoSubscriberMessages(false)
                        .installDefaultEventBus();
            } catch (Exception e) {
                // ignore
            }
        }

        if (BuildConfig.DEBUG) BuildConfig.STETHO.init(this);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath(FontManager.FONT_SYSTEM_DEFAULT_PATH)
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        NetworkUtils.getInstance().onAppInit(this);
        Session.getInstance().onAppInit(this);
        ImageUtils.getInstance().onAppInit(this);
        AnalyticsHelper.initInstance(this);
        resetLanguage();
        Intercom.initialize(this, BuildConfig.INTERCOM_API_KEY, BuildConfig.INTERCOM_APP_ID);

        if (BuildConfig.DEBUG) {
            try {
                AviaryIntentConfigurationValidator.validateConfiguration(this);
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalStateException("aviary validation error", e);
            }
        }
        GcmUtils.getInstance(this).setupGcm();
        PreferenceHelper.setDefaultValues(this, false);

        StatusBarNotifications.onAppInit(this);
        VKSdk.initialize(this);
        FacebookSdk.sdkInitialize(this);
        initActivityLifecycleTracker();
        FrenchToast.install(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (DBG) Log.v(TAG, "onTrimMemory() " + level);
        NetworkUtils.getInstance().onTrimMemory();
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

    public synchronized void startIntercomSession() {
        if (mInterSessionStarted) return;
        long userId = Session.getInstance().getCurrentUserId();
        if (userId == CurrentUser.USER_UNAUTHORIZED_ID) return;
        mInterSessionStarted = true;
        Intercom.client().setPreviewPosition(IntercomPreviewPosition.BOTTOM_RIGHT);
        Intercom.client().registerIdentifiedUser(new Registration().withUserId(String.valueOf(userId)));
    }

    public synchronized void endIntercomSession() {
        Intercom.client().reset();
        mInterSessionStarted = false;
    }

    @Override
    public String getClientID() {
        return BuildConfig.ADOBE_CLIENT_ID;
    }

    @Override
    public String getClientSecret() {
        return BuildConfig.ADOBE_CLIENT_SECRET;
    }

    @Override
    public String getBillingKey() {
        return "";
    }

    public boolean isUiActive() {
        return mActiveActivitiesCount > 0;
    }

    private void initActivityLifecycleTracker() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                mActiveActivitiesCount += 1;
                if (mActiveActivitiesCount == 1) {
                    EventBus.getDefault().post(new UiVisibleStatusChanged(1));
                    if (DBG) Log.d(TAG, "UiVisibleStatusChanged activities: " + mActiveActivitiesCount);
                }

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                mActiveActivitiesCount -= 1;
                if (mActiveActivitiesCount <= 0) {
                    EventBus.getDefault().post(new UiVisibleStatusChanged(0));
                    mActiveActivitiesCount = 0;
                    if (DBG) Log.d(TAG, "UiVisibleStatusChanged activities: " + mActiveActivitiesCount);
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }
}
