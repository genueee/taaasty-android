package ru.taaasty;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.OnCurrentUserChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.subjects.BehaviorSubject;

public class Session {

    private static final String SHARED_PREFS_NAME="authtoken";

    private static final String SHARED_PREFS_KEY_AUTHTOKEN="authtoken";

    private static final String SHARED_PREFS_KEY_USER="author";

    private static Session sSession;

    private String mAuthtoken;

    private Context mAppContext;

    private BehaviorSubject<CurrentUser> mCurrentUser;

    private Session() {
        mCurrentUser = BehaviorSubject.create(CurrentUser.UNAUTHORIZED);
    }

    @MainThread
    public static Session getInstance() {
        if (sSession == null) sSession = new Session();
        return sSession;
    }

    public void onAppInit(Context context) {
        mAppContext = context.getApplicationContext();
        load();
    }

    @Nullable
    public String getCurrentUserToken() {
        CurrentUser user = mCurrentUser.getValue();
        return user.isAuthorized() ? mAuthtoken : null;
    }

    /**
     * @return ID пользователя. {@linkplain CurrentUser#USER_UNAUTHORIZED_ID} (-1) если не авторизован
     */
    public long getCurrentUserId() {
        return mCurrentUser.getValue().getId();
    }

    public void setCurrentUser(@NonNull CurrentUser user) {
        if (user.isAuthorized()) {
            mAuthtoken = user.getApiKey().accessToken;
            mCurrentUser.onNext(user);
            persist();
        } else {
            logout();
        }
    }

    public void logout() {
        mAuthtoken = null;
        mCurrentUser.onNext(CurrentUser.UNAUTHORIZED);
        mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    public boolean isMe(long id) {
        // XXX: unauthorized - это я? По идее, у незарегенного пользователя не должно быть профиля
        return mCurrentUser.getValue().getId() == id;
    }

    public BehaviorSubject<CurrentUser> getUserObservable() {
        return mCurrentUser;
    }

    public Observable<CurrentUser> reloadCurrentUser() {
        return RestClient.getAPiUsers()
                .getMyInfo()
                .doOnNext(currentUser -> {
                    boolean changed = !currentUser.equals(mCurrentUser.getValue());
                    if (changed) {
                        mCurrentUser.onNext(currentUser);
                        EventBus.getDefault().post(new OnCurrentUserChanged(currentUser));
                    }
                });
        // return Observable.from(mCurrentUser);
    }

    // TODO постараться не использовать
    public CurrentUser getCachedCurrentUser() {
        return mCurrentUser.getValue();
    }

    /**
     * @return Пользователь авторизован
     */
    public boolean isAuthorized() {
        return mCurrentUser.getValue().isAuthorized();
    }

    private void load() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mAuthtoken = prefs.getString(SHARED_PREFS_KEY_AUTHTOKEN, null);
        String userSerialized = prefs.getString(SHARED_PREFS_KEY_USER, null);
        if (userSerialized != null) {
            mCurrentUser.onNext(NetworkUtils.getGson().fromJson(userSerialized, CurrentUser.class));
        } else {
            mCurrentUser.onNext(CurrentUser.UNAUTHORIZED);
        }
    }

    private void persist() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String userSerialized;
        if (mCurrentUser.getValue().isAuthorized()) {
            userSerialized = NetworkUtils.getGson().toJson(mCurrentUser.getValue());
        } else {
            userSerialized = null;
        }

        prefs.edit()
                .putString(SHARED_PREFS_KEY_AUTHTOKEN, mAuthtoken)
                .putString(SHARED_PREFS_KEY_USER, userSerialized)
                .apply();
    }

}
