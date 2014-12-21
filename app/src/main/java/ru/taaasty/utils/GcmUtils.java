package ru.taaasty.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;

/**
 * Created by alexey on 21.12.14.
 */
public class GcmUtils {

    private static final String PREFS_GCM_DETAILS = "gcmdetails";
    private static final String PROPERTY_REG_ID = "regid";

    private static GcmUtils sInstance;

    private final TaaastyApplication mContext;

    @Nullable
    private GoogleCloudMessaging mGoogleCloudMessaging;

    @Nullable
    private String mRegId;

    public static synchronized GcmUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GcmUtils(context);
        }
        return sInstance;
    }

    private GcmUtils(Context context) {
        mContext = (TaaastyApplication)context.getApplicationContext();
        getRegistrationId();
    }

    public void setupPlayServices() {
        if (checkPlayServices()) {
            mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(mContext);
            mRegId = getRegistrationId();
            if (TextUtils.isEmpty(mRegId)) {
                registerInBackground();
            } else {
                sendRegistrationIdToBackend();
            }
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    private String getRegistrationId() {
        final SharedPreferences prefs = mContext.getSharedPreferences(PREFS_GCM_DETAILS, 0);
        mRegId = prefs.getString(PROPERTY_REG_ID, "");
        return mRegId;
    }

    private void storeRegistrationId() {
        mContext.getSharedPreferences(PREFS_GCM_DETAILS, 0)
                .edit()
                .putString(PROPERTY_REG_ID, mRegId)
                .commit();
    }

    private void registerInBackground() {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (mGoogleCloudMessaging == null) {
                        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(mContext);
                    }
                    mRegId = mGoogleCloudMessaging.register(BuildConfig.GOOGLE_APP_ID);
                    sendRegistrationIdToBackend();
                    storeRegistrationId();
                } catch (IOException ex) {
                    Log.d("GcmUtils", "GCM ISSUE", ex);
                }
                return null;
            }
        }.execute(null, null, null);
    }

    private void sendRegistrationIdToBackend() {
        mContext.getIntercom().enablePush(mRegId, mContext.getPackageName(), R.drawable.ic_launcher);
    }


}
