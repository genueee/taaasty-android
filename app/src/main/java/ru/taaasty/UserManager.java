package ru.taaasty;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import ru.taaasty.model.CurrentUser;

public class UserManager {

    private static final String SHARED_PREFS_NAME="authtoken";

    private static final String SHARED_PREFS_KEY_AUTHTOKEN="authtoken";

    private static UserManager mUserManager;

    private String mAuthtoken;

    private Context mAppContext;

    private UserManager() {

    }

    public static UserManager getInstance() {
        if (mUserManager == null) mUserManager = new UserManager();
        return mUserManager;
    }

    public void onAppInit(Context context) {
        mAppContext = context.getApplicationContext();
        loadAuthtoken();
    }

    @Nullable
    public String getCurrentUserToken() {
        return mAuthtoken;
    }

    public void setCurrentUser(CurrentUser user) {
        mAuthtoken = user.getApiKey().accessToken;
        persistAuthtoken();
    }

    private void loadAuthtoken() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        mAuthtoken = prefs.getString(SHARED_PREFS_KEY_AUTHTOKEN, null);
    }

    private void persistAuthtoken() {
        SharedPreferences prefs = mAppContext.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(SHARED_PREFS_KEY_AUTHTOKEN, mAuthtoken).apply();
    }

}
