package ru.taaasty.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.model.CurrentUser;
import rx.functions.Action1;

/**
 * Created by arkhipov on 15.02.2016.
 */
public class AnalyticsHelper {

    private static AnalyticsHelper sInstance;

    private Context mContext;
    private volatile Tracker mAnalyticsTracker;

    private AnalyticsHelper(Context appContext) {
        mContext = appContext;
        getTracker();
    }

    public static void initInstance(Context appContext) {
        if (sInstance == null) {
            sInstance = new AnalyticsHelper(appContext);
        }
    }

    public static AnalyticsHelper getInstance() {
        return sInstance;
    }

    public synchronized Tracker getTracker() {
        if (mAnalyticsTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(mContext);
            if (!"release".equals(BuildConfig.BUILD_TYPE)) {
                analytics.setDryRun(true);
                analytics.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            } else {
                analytics.getLogger().setLogLevel(Logger.LogLevel.ERROR);
            }
            analytics.setLocalDispatchPeriod(1000);
            mAnalyticsTracker = analytics.newTracker(R.xml.app_tracker);
            mAnalyticsTracker.enableAdvertisingIdCollection(true);

            Session.getInstance().getUserObservable().subscribe(new Action1<CurrentUser>() {
                @Override
                public void call(CurrentUser currentUser) {
                    if (currentUser.isAuthorized()) {
                        mAnalyticsTracker.set("&uid", Long.toString(currentUser.getId()));
                    } else {
                        mAnalyticsTracker.set("&uid", null);
                    }
                }
            });

        }
        return mAnalyticsTracker;
    }

    private void sendAnalyticsEvent(String category, String action, @Nullable String label) {
        HitBuilders.EventBuilder eb = new HitBuilders.EventBuilder(category, action);
        eb.setCustomDimension(Constants.ANALYTICS_DIMENSION_GENDER,
                Session.getInstance().getCachedCurrentUser().isFemale()
                        ? Constants.ANALYTICS_GENDER_FEMALE : Constants.ANALYTICS_GENDER_MALE);
        eb.setCustomDimension(Constants.ANALYTICS_DIMENSION_DIARY_OPEN_STATUS,
                Session.getInstance().getCachedCurrentUser().isPrivacy()
                        ? Constants.ANALYTICS_TLOG_STATUS_CLOSED : Constants.ANALYTICS_TLOG_STATUS_OPENED);
        if (label != null) eb.setLabel(label);
        getTracker().send(eb.build());
    }

    public void sendAnalyticsShareEvent(String network, String targetUrl) {
        mAnalyticsTracker.send(new HitBuilders.SocialBuilder()
                .setNetwork(network)
                .setAction("Share")
                .setTarget(targetUrl)
                .setCustomDimension(Constants.ANALYTICS_DIMENSION_GENDER,
                        Session.getInstance().getCachedCurrentUser().isFemale()
                                ? Constants.ANALYTICS_GENDER_FEMALE : Constants.ANALYTICS_GENDER_MALE)
                .setCustomDimension(Constants.ANALYTICS_DIMENSION_DIARY_OPEN_STATUS,
                        Session.getInstance().getCachedCurrentUser().isPrivacy()
                                ? Constants.ANALYTICS_TLOG_STATUS_CLOSED : Constants.ANALYTICS_TLOG_STATUS_OPENED)
                .build()
        );
    }

    public void sendErrorAnalyticsEvent(CharSequence error, @Nullable Throwable exception) {
        if (exception == null) return;
        mAnalyticsTracker.send(new HitBuilders.ExceptionBuilder()
                .setDescription(
                        new AnalyticsExceptionParser(mContext, null)
                                .getDescription(Thread.currentThread().getName(), exception))
                .setFatal(false)
                .build()
        );
    }

    public void sendShowScreenEvent(String screenName) {
        mAnalyticsTracker.setScreenName(screenName);
        mAnalyticsTracker.send(new HitBuilders.AppViewBuilder().build());
    }

    public void sendUXEvent(String action) {
        sendUXEvent(action, null);
    }

    public void sendUXEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_UX, action, label);
    }

    public void sendAccountEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_ACCOUNT, action, label);
    }

    public void sendAccountEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_ACCOUNT, action, null);
    }

    public void sendPostsEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, action, label);
    }

    public void sendPostsEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, action, null);
    }

    public void sendFeedsEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FEEDS, action, label);
    }

    public void sendNotificationsEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_NOTIFICATIONS, action, null);
    }

    public void sendAppUpdateEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_APP_UPDATE, action, label);
    }

    public void sendFlowsEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FLOWS, action, null);
    }

    public void sendPreferencesAppEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_PREFERENCES_APP, action, null);
    }

    public void sendPreferencesProfileEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_PREFERENCES_PROFILE, action, label);
    }

    public void sendFeedsEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FEEDS, action, null);
    }

    public void sendFabEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FAB, action, label);
    }

    public void sendUsersEvent(String action) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_USERS, action, null);
    }

    public void sendUsersEvent(String action, String label) {
        sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_USERS, action, label);
    }
}
