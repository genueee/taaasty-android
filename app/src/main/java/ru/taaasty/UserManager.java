package ru.taaasty;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.OnCurrentUserChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.functions.Action1;

public class UserManager {

    private static final String SHARED_PREFS_NAME="authtoken";

    private static final String SHARED_PREFS_KEY_AUTHTOKEN="authtoken";

    private static final String SHARED_PREFS_KEY_USER="author";

    private static UserManager mUserManager;

    private String mAuthtoken;

    private Context mAppContext;

    private CurrentUser mCurrentUser;

    private UserManager() {

    }

    public static UserManager getInstance() {
        if (mUserManager == null) mUserManager = new UserManager();
        return mUserManager;
    }

    public void onAppInit(Context context) {
        mAppContext = context.getApplicationContext();
        load();
    }

    @Nullable
    public String getCurrentUserToken() {
        return mAuthtoken;
    }

    @Nullable
    public String getCurrentUserSlug() {
        return mCurrentUser == null ? null : mCurrentUser.getSlug();
    }

    public void setCurrentUser(CurrentUser user) {
        mAuthtoken = user.getApiKey().accessToken;
        mCurrentUser = user;
        persist();
    }

    public void logout() {
        mCurrentUser = null;
        mAuthtoken = null;
        mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    public Long getCurrentUserId() {
        return mCurrentUser == null ? null : mCurrentUser.getId();
    }

    public boolean isMe(long id) {
        return mCurrentUser != null && mCurrentUser.getId() == id;
    }

    public Observable<CurrentUser> getCurrentUser() {
        return RestClient.getAPiUsers()
                .getMyInfo()
                .doOnNext(new Action1<CurrentUser>() {
                    @Override
                    public void call(CurrentUser currentUser) {
                        boolean changed = currentUser.equals(mCurrentUser);
                        mCurrentUser = currentUser;
                        if (changed) EventBus.getDefault().post(new OnCurrentUserChanged(currentUser));
                    }
                });
        // return Observable.from(mCurrentUser);
    }

    @Nullable
    public CurrentUser getCachedCurrentUser() {
        return mCurrentUser;
    }

    private void load() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mAuthtoken = prefs.getString(SHARED_PREFS_KEY_AUTHTOKEN, null);
        String userSerialized = prefs.getString(SHARED_PREFS_KEY_USER, null);
        if (userSerialized != null) {
            mCurrentUser = NetworkUtils.getGson().fromJson(userSerialized, CurrentUser.class);
        }
    }

    private void persist() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String userSerialized = NetworkUtils.getGson().toJson(mCurrentUser);
        prefs.edit()
                .putString(SHARED_PREFS_KEY_AUTHTOKEN, mAuthtoken)
                .putString(SHARED_PREFS_KEY_USER, userSerialized)
                .apply();
    }

}
