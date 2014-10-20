package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;


public abstract class TabbarActivityBase extends ActivityBase implements Tabbar.onTabbarButtonListener, CustomErrorView {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TabbarActivityBase";

    static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 4;

    UserManager mUserManager = UserManager.getInstance();
    Tabbar mTabbar;

    private boolean mSwithcingTab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        PusherService.startPusher(this);

    }

    abstract int getCurrentTabId();

    abstract void onCurrentTabButtonClicked();

    protected void onPostCreate (Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(this);
        mTabbar.setActivated(getCurrentTabId());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREATE_POST_ACTIVITY_REQUEST_CODE:
                onCreatePostActivityClosed(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy");
        if (!mSwithcingTab) PusherService.stopPusher(this);
    }

    @Override
    public void onTabbarButtonClicked(View v) {
        if (v.getId() == getCurrentTabId()) {
            onCurrentTabButtonClicked();
            return;
        }

        switch (v.getId()) {
            case R.id.btn_tabbar_live:
                switchToLiveFeed();
                break;
            case R.id.btn_tabbar_my_feed:
                switchToMyFeed();
                break;
            case R.id.btn_tabbar_subscriptions:
                switchToSubscriptions();
                break;
            case R.id.btn_tabbar_post:
                openCreatePost();
                break;
            case R.id.btn_tabbar_notifications:
                switchToNotifications();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    void onCreatePostActivityClosed(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG);
                break;
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                switchToMyFeed(MyFeedActivity.SECTION_HIDDEN);
                break;
        }
    }

    void switchToLiveFeed() {
        Intent i = new Intent(this, LiveFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToLoginForm() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        mSwithcingTab = true;
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToSubscriptions() {
        Intent i = new Intent(this, SubscriptionsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        mSwithcingTab = true;
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToMyFeed() { switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG); }

    void switchToMyFeed(int initialSection) {
        Intent i = new Intent(this, MyFeedActivity.class);
        i.putExtra(MyFeedActivity.ARG_KEY_SHOW_SECTION, initialSection);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        mSwithcingTab = true;
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToNotifications() {
        Intent i = new Intent(this, NotificationsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        mSwithcingTab = true;
        finish();
        overridePendingTransition(0, 0);
    }

    void openCreatePost() {
        Intent i = new Intent(this,     CreatePostActivity.class);
        startActivityForResult(i, CREATE_POST_ACTIVITY_REQUEST_CODE);
    }


}
