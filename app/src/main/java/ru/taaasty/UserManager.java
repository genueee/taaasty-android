package ru.taaasty;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import ru.taaasty.model.CurrentUser;
import ru.taaasty.service.Users;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;

public class UserManager {

    private static final String SHARED_PREFS_NAME="authtoken";

    private static final String SHARED_PREFS_KEY_AUTHTOKEN="authtoken";

    private static final String SHARED_PREFS_KEY_USER="user";

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

    public void setCurrentUser(CurrentUser user) {
        mAuthtoken = user.getApiKey().accessToken;
        mCurrentUser = user;
        persist();
    }

    public Observable<CurrentUser> getCurrentUser() {
        return NetworkUtils.getInstance().createRestAdapter().create(Users.class).getMyInfo();
        // return Observable.from(mCurrentUser);
    }

    private void load() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mAuthtoken = prefs.getString(SHARED_PREFS_KEY_AUTHTOKEN, null);
        String userSerialized = prefs.getString(SHARED_PREFS_KEY_USER, null);
        if (userSerialized != null) {
            mCurrentUser = NetworkUtils.getInstance().getGson().fromJson(userSerialized, CurrentUser.class);
        }
    }

    private void persist() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        String userSerialized = NetworkUtils.getInstance().getGson().toJson(mCurrentUser);
        prefs.edit()
                .putString(SHARED_PREFS_KEY_AUTHTOKEN, mAuthtoken)
                .putString(SHARED_PREFS_KEY_USER, userSerialized)
                .apply();
    }

}
