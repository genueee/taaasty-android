package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.PreferenceHelper;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.MessageHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
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
public class SettingsFragment extends PreferenceFragmentCompat {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ProfileFragment";

    public static final int REQUEST_CODE_LOGIN = 1;

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

    private SwitchPreferenceCompat mIsPrivacyPref;

    private SwitchPreferenceCompat mIsFemalePref;

    private EditTextPreference mEmailPref;

    private SwitchPreferenceCompat mAvailableNotificationsPref;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        getPreferenceManager().setSharedPreferencesName(PreferenceHelper.PREFS_NAME);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.profile_preferences);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        ListView listView = (ListView)root.findViewById(android.R.id.list);
        if (listView != null) {
            Resources resources = root.getResources();
            int padding = resources.getDimensionPixelOffset(R.dimen.following_followers_horizontal_margin);
            listView.setPadding(padding, listView.getPaddingTop(), padding, listView.getPaddingBottom());
            listView.setClipToPadding(false);
            listView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        }

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mApiUsers = RestClient.getAPiUsers();
        mUsernamePref = (EditTextPreference) findPreference("preference_user_name");
        mTlogTitlePref = (EditTextPreference) findPreference("preference_tlog_title");
        mIsPrivacyPref = (SwitchPreferenceCompat) findPreference("preference_is_privacy");
        mIsFemalePref = (SwitchPreferenceCompat) findPreference("preference_is_female");
        mEmailPref = (EditTextPreference) findPreference("preference_email");
        mAvailableNotificationsPref = (SwitchPreferenceCompat) findPreference("preference_available_notifications");

        Preference.OnPreferenceClickListener nestedClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mListener != null) {
                    mListener.onNestedPreferenceSelected(preference.getKey());
                    return true;
                } else {
                    return false;
                }
            }
        };

        findPreference("pref_key_conversation_notifications").setOnPreferenceClickListener(nestedClickListener);
        findPreference("pref_key_notification_notifications").setOnPreferenceClickListener(nestedClickListener);
        mCurrentUser = Session.getInstance().getCachedCurrentUser();
        setupUser();
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
        //noinspection ConstantConditions
        ((ActivityBase)getActivity()).getSupportActionBar().setTitle(getPreferenceScreen().getTitle());
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
        Observable<CurrentUser> observableCurrentUser = Session.getInstance().reloadCurrentUser();

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
        //mUsernamePref.getEditText().setFilters(new InputFilter[]{new UsernameFilter()});

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

        mPutSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction)
                .subscribe(mUpdateUserObserver);
        setupRefreshingIndicator();
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
                AnalyticsHelper.getInstance().sendPreferencesProfileEvent(action, label);
            }
        };
    }

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
            MessageHelper.showError(SettingsFragment.this, e, R.string.error_saving_settings, REQUEST_CODE_LOGIN);
        }

        @Override
        public void onNext(CurrentUser info) {
            mCurrentUser = info;
            if (mListener != null) mListener.onCurrentUserLoaded(mCurrentUser);
            Session.getInstance().setCurrentUser(mCurrentUser);
            setupUser();
        }
    };

    public interface OnFragmentInteractionListener {
        void onErrorRefreshUser(Throwable e);
        void onCurrentUserLoaded(CurrentUser user);
        void onNestedPreferenceSelected(String key);
    }

    public static class NotificationsSettingsNestedFragment extends PreferenceFragmentCompat {
        private static final String TAG_KEY = "TAG_KEY";

        public static NotificationsSettingsNestedFragment newInstance(String key) {
            NotificationsSettingsNestedFragment fragment = new NotificationsSettingsNestedFragment();
            // supply arguments to bundle.
            Bundle args = new Bundle();
            args.putString(TAG_KEY, key);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            getPreferenceManager().setSharedPreferencesName(PreferenceHelper.PREFS_NAME);

            String key = getArguments().getString(TAG_KEY);
            switch (key) {
                case "pref_key_conversation_notifications":
                    addPreferencesFromResource(R.xml.preferences_conversation_notifications);
                    findPreference("reset_conversations_notifications_preferences").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            resetToDefaults(getPreferenceScreen());
                            return true;
                        }
                    });
                    break;
                case "pref_key_notification_notifications":
                    addPreferencesFromResource(R.xml.preferences_notification_notifications);
                    findPreference("reset_notifications_notifications_preferences").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            resetToDefaults(getPreferenceScreen());
                            return true;
                        }
                    });
                    break;
                default:
                    throw new IllegalArgumentException("unknown key " + key);
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            //noinspection ConstantConditions
            ((ActivityBase)getActivity()).getSupportActionBar().setTitle(getPreferenceScreen().getTitle());
        }

        private static void resetToDefaults(PreferenceGroup root) {
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
    }

}
