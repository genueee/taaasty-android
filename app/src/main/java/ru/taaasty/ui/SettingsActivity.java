package ru.taaasty.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.PreferenceHelper;
import ru.taaasty.R;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 30.12.14.
 */
public class SettingsActivity extends ActivityBase implements SettingsFragment.OnFragmentInteractionListener {
    private static final String TAG = "SettingsActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            Fragment userInfoFragment = SettingsFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        }

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    public void onResume() {
        super.onResume();
        getSharedPreferences(PreferenceHelper.PREFS_NAME, 0).registerOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getSharedPreferences(PreferenceHelper.PREFS_NAME, 0).unregisterOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }


    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onErrorRefreshUser(Throwable e) {
        Toast.makeText(this,
                UiUtils.getUserErrorText(getResources(), e, R.string.user_error),
                Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onCurrentUserLoaded(CurrentUser user) {
    }

    @Override
    public void onNestedPreferenceSelected(String key) {
        getSupportFragmentManager().beginTransaction().replace(R.id.container,
                SettingsFragment.NotificationsSettingsNestedFragment.newInstance(key))
                        .addToBackStack(null).commit();
    }

    /**
     * Listener для отправки аналитики для persistent настроек
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener mSharedPrefsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS:
                    sendAnalyticsAppEvent("все пуши", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS:
                    sendAnalyticsAppEvent("пуши о переписках", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_VIBRATE:
                    sendAnalyticsAppEvent("вибрация (пуши о переписках)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_SOUND:
                    sendAnalyticsAppEvent("звук (пуши о переписках)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_CONVERSATIONS_NOTIFICATIONS_LIGHTS:
                    sendAnalyticsAppEvent("световой индикатор (пуши о переписках)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_ENABLE_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS:
                    sendAnalyticsAppEvent("пуши об уведомлениях", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_VIBRATE:
                    sendAnalyticsAppEvent("вибрация (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_SOUND:
                    sendAnalyticsAppEvent("звук (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_NOTIFICATIONS_LIGHTS:
                    sendAnalyticsAppEvent("световой индикатор (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_VOTES_FAVORITES:
                    sendAnalyticsAppEvent("соб. голоса и избранное (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_NEW_COMMENTS:
                    sendAnalyticsAppEvent("соб. новые комментарии (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING:
                    sendAnalyticsAppEvent("соб. подписки (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_REQUEST:
                    sendAnalyticsAppEvent("соб. запросы на дружбу (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_FOLLOWING_APPROVE:
                    sendAnalyticsAppEvent("соб. одобрение дружбы (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                case PreferenceHelper.PREF_KEY_STATUS_BAR_NOTIFICATIONS_EVENTS_MENTIONS:
                    sendAnalyticsAppEvent("соб. упоминания (пуши об уведомлениях)", sharedPreferences.getBoolean(key, true));
                    break;
                default:
                    if (DBG) throw new IllegalStateException("unhandled preference key" + key);
            }
        }

        private void sendAnalyticsAppEvent(String action, boolean switchedOn) {
            String mainAction = switchedOn ? "Вкл. " : "Выкл. ";
            AnalyticsHelper.getInstance().sendPreferencesAppEvent(mainAction + action);
        }
    };
}
