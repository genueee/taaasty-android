package ru.taaasty;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.io.IOException;

import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.NetworkUtils;

/**
 * Created by alexey on 28.10.14.
 */
public class StatusBarNotifications {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "StatusBarNotifications";

    static final int WEARABLE_NOTIFICATION_BACKGROUND_WIDTH = 400;

    private static volatile StatusBarNotifications sInstance;

    private volatile int mDisableStatusBarNotifications;

    private final StatusBarNotificationNotification mNotificationsHandler;

    private final StatusBarConversationNotification mConversationHandler;

    private StatusBarNotifications(TaaastyApplication application) {
        mDisableStatusBarNotifications = 0;
        mNotificationsHandler = new StatusBarNotificationNotification(application);
        mConversationHandler = new StatusBarConversationNotification(application);
        mNotificationsHandler.onCreate();
        mConversationHandler.onCreate();
    }

    public static void onAppInit(TaaastyApplication application) {
        sInstance = new StatusBarNotifications(application);
    }

    public void onDestroy() {
        mNotificationsHandler.onDestroy();
        mConversationHandler.onDestroy();
    }

    public static StatusBarNotifications getInstance() {
        return sInstance;
    }

    public void enableStatusBarNotifications() {
        addEnableDisableNotifications(false);
    }

    public void disableStatusBarNotifications() {
        addEnableDisableNotifications(true);
    }

    private synchronized void addEnableDisableNotifications(boolean enable) {
        if (enable) {
            mDisableStatusBarNotifications -= 1;
        } else {
            mDisableStatusBarNotifications += 1;
        }

        if (mDisableStatusBarNotifications == 1) {
            mNotificationsHandler.pause();
            mConversationHandler.pause();
        } else {
            mNotificationsHandler.resume();
            mConversationHandler.resume();
        }
    }

    public synchronized void onNotificationsMarkedAsRead() {
        mNotificationsHandler.onNotificationsMarkedAsRead();
    }

    public void onNewNotificationIdSeen(long id) {
        mNotificationsHandler.onNewNotificationIdSeen(id);
    }

    public synchronized void onConversationNotificationCancelled() {
        mConversationHandler.onConversationNotificationCancelled();
    }


    public void onGcmPushNotificationReceived(final Intent intent) {
        mNotificationsHandler.onGcmNotificationReceived(intent);
    }

    public void onGcmPushConversationReceived(final Intent intent) {
        mConversationHandler.onGcmConversationReceived(intent);
    }

    public synchronized void onLogout() {
        mNotificationsHandler.onLogout();
        mConversationHandler.onLogout();
    }

    /**
     * AsyncTask для загрузки изображений для нотификации.
     * В execute() передавать url картинки, не в thumbor
     */
    public static class LoadNotificationDataTask extends AsyncTask<String, Void, Void> {

        private final Picasso mPicasso;

        private final boolean mRoundCorners;

        /**
         * Иконка для setBigIcon() в нотификации
         */
        protected volatile Bitmap bigIcon;

        /**
         * background для wearable
         */
        protected volatile Bitmap wearableBackground;


        private int mBigIconWidth;

        private final int mWearableBackgroundWidth = StatusBarNotifications.WEARABLE_NOTIFICATION_BACKGROUND_WIDTH;

        public LoadNotificationDataTask(Context context, boolean roundCorners) {
            mBigIconWidth = context.getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width);
            mPicasso = Picasso.with(context);
            mRoundCorners = roundCorners;
        }

        /**
         *
         * @param params: url, thumbor path
         * @return
         */
        @SuppressWarnings("SuspiciousNameCombination")
        @Override
        protected Void doInBackground(String... params) {
            String url;
            String pUrl = params[0];

            int largestWidth = Math.max(mBigIconWidth, mWearableBackgroundWidth); // Тут, в принципе, всегда 400

            if (TextUtils.isEmpty(pUrl)) {
                return null;
            }

            url = NetworkUtils.createThumborUrl(pUrl)
                    .resize(largestWidth, largestWidth)
                    .filter(ThumborUrlBuilder.noUpscale())
                    .toUrl();

            try {
                Bitmap bitmap = mPicasso
                        .load(url)
                        .resize(largestWidth, largestWidth)
                        .get();


                bigIcon = Bitmap.createScaledBitmap(bitmap, mBigIconWidth, mBigIconWidth, true);
                if (mRoundCorners) {
                    bigIcon = new CircleTransformation().transform(bigIcon);
                }

                wearableBackground = Bitmap.createScaledBitmap(bitmap, mWearableBackgroundWidth, mWearableBackgroundWidth, true);
            } catch (IOException e) {
                if (DBG) Log.i(TAG, "bitmap load error", e);
            }

            return null;
        }
    }

}
