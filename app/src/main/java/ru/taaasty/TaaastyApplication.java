package ru.taaasty;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.adobe.creativesdk.aviary.IAviaryClientCredentials;
import com.adobe.creativesdk.aviary.utils.AdobeImageEditorIntentConfigurationValidator;
import com.adobe.creativesdk.foundation.AdobeCSDKFoundation;
import com.crashlytics.android.Crashlytics;
import com.facebook.FacebookSdk;
import com.vk.sdk.VKSdk;

import de.greenrobot.event.EventBus;
import frenchtoast.FrenchToast;
import io.fabric.sdk.android.Fabric;
import ru.taaasty.events.UiVisibleStatusChanged;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.GcmUtils;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends MultiDexApplication implements IAviaryClientCredentials {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";
    private volatile boolean mInterSessionStarted;

    private int mActiveActivitiesCount = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        if ("debug".equals(BuildConfig.BUILD_TYPE)) {
            // https://code.google.com/p/android/issues/detail?id=35298#c2
            (new Handler()).postAtFrontOfQueue(() -> {
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
            });
        }
        if ("beta".equals(BuildConfig.BUILD_TYPE)) {
            Fabric.with(this, new Crashlytics());
        }

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

        // Aviary
        AdobeCSDKFoundation.initializeCSDKFoundation(getApplicationContext());
        if (BuildConfig.DEBUG) {
            try {
                AdobeImageEditorIntentConfigurationValidator.validateConfiguration(this);
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
