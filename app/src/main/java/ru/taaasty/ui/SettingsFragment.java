package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.preference.TwoStatePreference;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.PreferenceHelper;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.UserManager;
import ru.taaasty.rest.ResponseErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.service.ApiUsers;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SettingsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SettingsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SettingsFragment extends PreferenceFragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ProfileFragment";

    private OnFragmentInteractionListener mListener;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    private Subscription mUserSubscription = Subscriptions.unsubscribed();

    private Subscription mPutSubscription = Subscriptions.unsubscribed();

    private ApiUsers mApiUsers;

    private CurrentUser mCurrentUser;

    private EditTextPreference mUsernamePref;

    private EditTextPreference mTlogTitlePref;

    private SwitchPreference mIsPrivacyPref;

    private SwitchPreference mIsFemalePref;

    private EditTextPreference mEmailPref;

    private SwitchPreference mAvailableNotificationsPref;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getPreferenceManager().setSharedPreferencesName(PreferenceHelper.PREFS_NAME);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.profile_preferences);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mApiUsers = RestClient.getAPiUsers();
        mUsernamePref = (EditTextPreference) findPreference("preference_user_name");
        mTlogTitlePref = (EditTextPreference) findPreference("preference_tlog_title");
        mIsPrivacyPref = (SwitchPreference) findPreference("preference_is_privacy");
        mIsFemalePref = (SwitchPreference) findPreference("preference_is_female");
        mEmailPref = (EditTextPreference) findPreference("preference_email");
        mAvailableNotificationsPref = (SwitchPreference) findPreference("preference_available_notifications");

        findPreference("reset_conversations_notifications_preferences").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetToDefaults((PreferenceScreen)findPreference("status_bar_conversations_settings"));
                return true;
            }
        });

        findPreference("reset_notifications_notifications_preferences").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                resetToDefaults((PreferenceScreen)findPreference("status_bar_notifications_settings"));
                return true;
            }
        });
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshUser();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mSharedPrefsChangedListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        setOnPreferenceChangeListeners(null);
        mUserSubscription.unsubscribe();
        mPutSubscription.unsubscribe();
        mUsernamePref = null;
        mTlogTitlePref = null;
        mIsPrivacyPref = null;
        mIsFemalePref = null;
        mEmailPref = null;
        mAvailableNotificationsPref = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void refreshUser() {
        if (!mUserSubscription.isUnsubscribed()) {
            mUserSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }
        Observable<CurrentUser> observableCurrentUser = AppObservable.bindFragment(this,
                mApiUsers.getMyInfo());

        mUserSubscription = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
        setupRefreshingIndicator();
    }

    /**
     * Установка OnPreferenceChangeListener для не-persistent настроек (которые хранятся на сервере)
     * @param listener
     */
    private void setOnPreferenceChangeListeners(Preference.OnPreferenceChangeListener  listener) {
        mUsernamePref.setOnPreferenceChangeListener(listener);
        mTlogTitlePref.setOnPreferenceChangeListener(listener);
        mIsPrivacyPref.setOnPreferenceChangeListener(listener);
        mIsFemalePref.setOnPreferenceChangeListener(listener);
        mEmailPref.setOnPreferenceChangeListener(listener);
        mAvailableNotificationsPref.setOnPreferenceChangeListener(listener);
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            setupRefreshingIndicator();
        }
    };

    boolean isLoading() {
        return !mUserSubscription.isUnsubscribed() || !mPutSubscription.isUnsubscribed();
    }

    void setupRefreshingIndicator() {
        boolean showIndicator = isLoading();
        if (getActivity() != null) getActivity().setProgressBarIndeterminateVisibility(showIndicator);
        getPreferenceScreen().setEnabled(!showIndicator);
    }

    void setupUser() {
        setOnPreferenceChangeListeners(null);
        mUsernamePref.setText(mCurrentUser.getName());
        mUsernamePref.setSummary(mCurrentUser.getName());
        mUsernamePref.getEditText().setFilters(new InputFilter[]{new UsernameFilter()});

        if (TextUtils.isEmpty(mCurrentUser.getTitle())) {
            mTlogTitlePref.setSummary(R.string.title_tlog_summary);
            mTlogTitlePref.setText("");
        } else {
            mTlogTitlePref.setSummary(mCurrentUser.getTitle());
            mTlogTitlePref.setText(mCurrentUser.getTitle());
        }
        mIsPrivacyPref.setChecked(mCurrentUser.isPrivacy());
        mIsFemalePref.setChecked(mCurrentUser.isFemale());

        if (TextUtils.isEmpty(mCurrentUser.getEmail())) {
            mEmailPref.setSummary(R.string.summary_email);
            mEmailPref.setText("");
        } else {
            mEmailPref.setText(mCurrentUser.getEmail());
            mEmailPref.setText(mCurrentUser.getEmail());
        }
        mAvailableNotificationsPref.setChecked(mCurrentUser.areNotificationsAvailable());
        // Handler - для android 4.1 и даже там этот способ избежать циклического вызова mOnPreferenceChangeListener
        // вроде не работает
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                if (mUsernamePref != null)
                    setOnPreferenceChangeListeners(mOnPreferenceChangeListener);
            }
        });
    }

    void postForm(Observable<CurrentUser> observable) {
        if (!mPutSubscription.isUnsubscribed()) {
            mPutSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        mPutSubscription = AppObservable.bindFragment(this, observable)
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction)
                .subscribe(mUpdateUserObserver);
        setupRefreshingIndicator();
    }

    void resetToDefaults(PreferenceGroup root) {
        for (int i = root.getPreferenceCount() - 1; i >= 0; --i) {
            Preference preference = root.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                resetToDefaults((PreferenceGroup) preference);
                continue;
            }

            if (!preference.isPersistent()) continue;

            if (preference instanceof TwoStatePreference) {
                Boolean defVal = (Boolean) PreferenceHelper.getDefaultValue(preference.getKey());
                if (defVal == null) continue;
                ((TwoStatePreference) preference).setChecked(defVal);
            } else {
                throw new IllegalStateException("Unknown preference type");
            }
        }

    }

    private final Preference.OnPreferenceChangeListener mOnPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {

        Observable<CurrentUser> observable;

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (DBG) Log.v(TAG, "onPreferenceChange pref: " + preference.getKey() + " new val: " + newValue);
            if (mCurrentUser == null) return true;
            switch (preference.getKey()) {
                case "preference_user_name":
                    if (TextUtils.equals((CharSequence)newValue, mCurrentUser.getName())) {
                        return true;
                    } else if (TextUtils.isEmpty((CharSequence) newValue)) {
                        return false;
                    } else {
                        observable = mApiUsers.setMySlug((String) newValue)
                            .doOnCompleted(sendAnalyticsProfileEvent("Изменено название тлога", null));
                    }
                    break;
                case "preference_tlog_title":
                    if (TextUtils.equals((CharSequence)newValue, mCurrentUser.getTitle())) {
                         return true;
                    } else {
                        observable = mApiUsers.setMyTitle((String) newValue)
                                .doOnCompleted(sendAnalyticsProfileEvent("Изменено описание тлога", null));
                    }
                    break;
                case "preference_is_privacy":
                    if (mCurrentUser.isPrivacy() == (Boolean)newValue) {
                        return true;
                    } else {
                        observable = mApiUsers.setMyIsPrivacy((Boolean) newValue)
                                .doOnCompleted(sendAnalyticsProfileEvent("Изменена приватность",
                                        (Boolean) newValue ? "Закрытый тлог" : "Открытый тлог"));
                    }
                    break;
                case "preference_is_female":
                    if (mCurrentUser.isFemale() == (Boolean) newValue) {
                        return true;
                    } else {
                        observable = mApiUsers.setMyIsFemale((Boolean) newValue)
                                .doOnCompleted(sendAnalyticsProfileEvent("Изменено \"вы - девушка\"",
                                        (Boolean) newValue ? "Девушка" : "Не девушка"))
                        ;
                    }
                    break;
                case "preference_email":
                    if (TextUtils.equals(mCurrentUser.getEmail(), (CharSequence) newValue)) {
                        return true;
                    } else if (TextUtils.isEmpty((CharSequence) newValue)) {
                        return false;
                    } else {
                        observable = mApiUsers.setMyEmail((String) newValue)
                                .doOnCompleted(sendAnalyticsProfileEvent("Изменен емайл", null));
                    }
                    break;
                case "preference_available_notifications":
                    if (mCurrentUser.areNotificationsAvailable() == (Boolean) newValue) {
                        return true;
                    } else {
                        observable = mApiUsers.setMyAvailableNotifications((Boolean) newValue)
                                .doOnCompleted(sendAnalyticsProfileEvent("изменено \"отправлять емайл уведомления\" ",
                                        (Boolean) newValue ? "Отправлять" : "Не отправлять"));
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
            postForm(observable);
            return false;
        }
    };

    private Action0 sendAnalyticsProfileEvent(final String action, final String label) {
        return new Action0() {
            TaaastyApplication app = (TaaastyApplication)getActivity().getApplication();
            @Override
            public void call() {
                app.sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_PREFERENCES_PROFILE, action, label);
            }
        };
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
            if (getActivity() == null) return;
            String mainAction = switchedOn ? "Вкл. " : "Выкл. ";
            TaaastyApplication app = (TaaastyApplication)getActivity().getApplication();
            app.sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_PREFERENCES_APP, mainAction + action,
                    null);
        }
    };

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            if (mListener != null) mListener.onErrorRefreshUser(e);
        }

        @Override
        public void onNext(CurrentUser info) {
            mCurrentUser = info;
            if (mListener != null) mListener.onCurrentUserLoaded(mCurrentUser);
            setupUser();
        }
    };

    private final Observer<CurrentUser> mUpdateUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            if (e instanceof ResponseErrorException) {

            }
            if (mListener != null) mListener.notifyError("update user error", e);
        }

        @Override
        public void onNext(CurrentUser info) {
            mCurrentUser = info;
            if (mListener != null) mListener.onCurrentUserLoaded(mCurrentUser);
            UserManager.getInstance().setCurrentUser(mCurrentUser);
            setupUser();
        }
    };

    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onErrorRefreshUser(Throwable e);
        public void onCurrentUserLoaded(CurrentUser user);
    }


}
