package ru.taaasty.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import ru.taaasty.BuildConfig;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.AppVersionResponse;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by alexey on 03.12.15.
 */
public final class CheckAppHelper {

    private static final String APP_HELPER_PREFS_FILE_NAME = "app_version_check";

    private static final String KEY_LAST_RESPONSE = "last_response";

    private static final String KEY_LAST_CHECK_TIMESTAMP = "last_response_timestamp";



    public static boolean isNewVersionAvailableShown = false;

    private CheckAppHelper() {
    }


    public static void openUpdateAppLink(Activity activity) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=ru.taaasty"));
        activity.startActivity(intent);
        AnalyticsHelper.getInstance().sendAppUpdateEvent("нажато 'обновить'", BuildConfig.VERSION_NAME);
    }

    public static Observable<CheckVersionResult> createCheckVersionObservable(Context context) {
        final Context appContext = context.getApplicationContext();
        return RestClient.getAPiApp().checkVersion("android_offical", BuildConfig.VERSION_NAME)
                .observeOn(Schedulers.io())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .map(appVersionResponse -> {
                    if (appVersionResponse.update == null) return CheckVersionResult.DO_NOTHING;
                    return handleCheckVersionResponse(appContext, appVersionResponse);
                });
    }

    static CheckVersionResult handleCheckVersionResponse(Context context, AppVersionResponse response) {
        synchronized (CheckAppHelper.class) {
            switch (response.update) {
                case AppVersionResponse.UPDATE_STATUS_RECOMMENDED:
                    // Показываем всегда (при первом запуске)
                    saveLastResponse(context, response);
                    return new CheckVersionResult(CheckVersionResult.ACTION_SHOW_NEW_VERSION_AVAILABLE, response.message);
                case AppVersionResponse.UPDATE_STATUS_REQUIRED:
                    // Показываем вообще всегда
                    saveLastResponse(context, response);
                    return new CheckVersionResult(CheckVersionResult.ACTION_SHOW_APP_UNSUPPORTED_MESSAGE, response.message);
                case AppVersionResponse.UPDATE_STATUS_NOT_REQUIRED:
                case AppVersionResponse.UPDATE_STATUS_UNKNOWN:
                default:
                    if (TextUtils.isEmpty(response.message)) {
                        clearLastResponse(context);
                        return CheckVersionResult.DO_NOTHING;
                    } else {
                        return showIfRequired(context, CheckVersionResult.ACTION_SHOW_MESSAGE, response);
                    }
            }
        }
    }

    @WorkerThread
    private static CheckVersionResult showIfRequired(Context context, int action, AppVersionResponse response) {
        SharedPreferences prefs;
        int lastShownTs;
        AppVersionResponse lastAppVersionResponse = null;

        prefs = context.getSharedPreferences(APP_HELPER_PREFS_FILE_NAME, 0);
        String str = prefs.getString(KEY_LAST_RESPONSE, null);
        if (str != null) {
            lastAppVersionResponse = NetworkUtils.getGson().fromJson(str, AppVersionResponse.class);
        }
        if (Objects.equals(lastAppVersionResponse, response)) {
            return CheckVersionResult.DO_NOTHING; //уже показывали
        } else {
            saveLastResponse(context, response);
            return new CheckVersionResult(action, response.message);
        }
    }

    @WorkerThread
    private static void clearLastResponse(Context context) {
        context.getSharedPreferences(APP_HELPER_PREFS_FILE_NAME, 0).edit().clear().commit();
    }

    @WorkerThread
    private static void saveLastResponse(Context context, AppVersionResponse response) {
        long ts = System.currentTimeMillis();
        String responseJson = NetworkUtils.getGson().toJson(response);
        context.getSharedPreferences(APP_HELPER_PREFS_FILE_NAME, 0).edit()
                .putLong(KEY_LAST_CHECK_TIMESTAMP, ts)
                .putString(KEY_LAST_RESPONSE, responseJson)
                .commit();
    }


    public static class CheckVersionResult {

        /**
         * Ничего не нужно делать
         */
        public static final int ACTION_DO_NOTHING = 0; // Ничего не надо делать

        /**
         * Показать сообщение, без всяких кнопок
         */
        public static final int ACTION_SHOW_MESSAGE = 1;

        /**
         * Показать сообщение, что новая версия доступна
         */
        public static final int ACTION_SHOW_NEW_VERSION_AVAILABLE = 2;

        /**
         * Показать непрерываемое соообщение
         */
        public static final int ACTION_SHOW_APP_UNSUPPORTED_MESSAGE = 3;

        public final int action;

        public final String message;

        public static final CheckVersionResult DO_NOTHING = new CheckVersionResult(ACTION_DO_NOTHING, null);

        CheckVersionResult(int action, String message) {
            this.action = action;
            this.message = message;
        }

        @Override
        public String toString() {
            return "CheckVersionResult{" +
                    "action=" + action +
                    ", message='" + message + '\'' +
                    '}';
        }
    }

}
