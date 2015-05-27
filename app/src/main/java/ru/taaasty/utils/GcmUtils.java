package ru.taaasty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import ru.taaasty.BuildConfig;
import ru.taaasty.GcmIntentService;
import ru.taaasty.rest.RestClient;

public class GcmUtils {
    private static final String TAG = "GcmUtils";
    private static final boolean DBG = BuildConfig.DEBUG;

    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";

    private static final String PREFS_GCM_DETAILS = "gcmdetails";

    private static GcmUtils sInstance;

    private final Context mContext;

    public static final String GCM_NOTIFICATION_TYPE_PUSH_NOTIFICATION = "push_notification";

    public static final String GCM_NOTIFICATION_TYPE_PUSH_MESSAGE = "push_message";


    @Nullable
    private String mRegId;

    public static synchronized GcmUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GcmUtils(context);
        }
        return sInstance;
    }

    private GcmUtils(Context context) {
        mContext = context.getApplicationContext();
        mRegId = loadRegistrationId();
    }

    public void setupGcm() {
        if (checkPlayServices()) {
            mRegId = loadRegistrationId();
            if (TextUtils.isEmpty(mRegId)) {
                GcmIntentService.startRegisterGcm(mContext);
            } else {
                // На всякий случай регимся на сервере каждый раз
                GcmIntentService.startSendGcmIdToServer(mContext);
            }
        }
    }

    public void onLogout() {
        if (mRegId == null) return;
        try {
            RestClient.getAPiDevice().unregister(mRegId);
        } catch (Throwable ignore) {
        } finally {
            mRegId = null;
            storeRegistrationId();
        }
    }

    public void onGcmRegistrationComplete(String newRegId) {
        mRegId = newRegId;
        storeRegistrationId();
    }

    @Nullable
    public String getRegistrationId() {
        return TextUtils.isEmpty(mRegId) ? null : mRegId;
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    private String loadRegistrationId() {
        final SharedPreferences prefs = mContext.getSharedPreferences(PREFS_GCM_DETAILS, 0);
        String regId = prefs.getString(PROPERTY_REG_ID, "");
          if (TextUtils.isEmpty(regId)) {
            return "";
        }
        String registeredVersion = prefs.getString(PROPERTY_APP_VERSION, "");
        if (!TextUtils.equals(registeredVersion, BuildConfig.VERSION_NAME)) {
            // App version changed
            return "";
        }

        return regId;
    }

    private void storeRegistrationId() {
        mContext.getSharedPreferences(PREFS_GCM_DETAILS, 0)
                .edit()
                .putString(PROPERTY_REG_ID, mRegId)
                .putString(PROPERTY_APP_VERSION, BuildConfig.VERSION_NAME)
                .commit();
    }

    public static String getGcmNotificationType(Bundle extras) {
        return extras.getString("collapse_key");
    }

    public static boolean isTastyMessage(Bundle extras) {
        String collapseKey = getGcmNotificationType(extras);
        return GCM_NOTIFICATION_TYPE_PUSH_NOTIFICATION.equals(collapseKey)
                || GCM_NOTIFICATION_TYPE_PUSH_MESSAGE.equals(collapseKey);
    }

}
