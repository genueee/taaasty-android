package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.feeds.AdditionalFeedActivity;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.widgets.ErrorTextView;


public abstract class TabbarActivityBase extends ActivityBase implements TabbarFragment.onTabbarButtonListener, CustomErrorView {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TabbarActivityBase";

    static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 4;

    Session mSession = Session.getInstance();
    TabbarFragment mTabbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mSession.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        PusherService.startPusher(this);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);
        initTabbar();
    }

    abstract int getCurrentTabId();

    abstract void onCurrentTabButtonClicked();

    @Override
    protected void onStart() {
        super.onStart();
        initTabbar();
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
            case R.id.btn_tabbar_post:
                openCreatePost();
                break;
            case R.id.btn_tabbar_notifications:
                switchToNotifications();
                break;
            case R.id.btn_tabbar_conversations:
                switchToConversations();
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
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            ert.setError(error, exception);
        }
    }

    void onCreatePostActivityClosed(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                switchToMyFeed();
                break;
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                openHidden();
                break;
        }
    }

    public TabbarFragment getTabbar() {
        if (mTabbar == null) initTabbar();
        return mTabbar;
    }

    void initTabbar() {
        if (mTabbar != null) return;
        mTabbar = (TabbarFragment) getSupportFragmentManager().findFragmentById(R.id.tabbar);
        mTabbar.setActivated(getCurrentTabId());
    }

    protected void openHidden() {
        AdditionalFeedActivity.startPrivateFeedActivity(this, null);
    }

    protected void switchToLoginForm() { switchToTab(LoginActivity.class); }

    protected void switchToLiveFeed() { switchToTab(LiveFeedActivity.class); }

    protected void switchToConversations() { switchToTab(ConversationsActivity.class); }

    protected void switchToNotifications() { switchToTab(NotificationsActivity.class);}

    protected void switchToMyFeed() { switchToTab(MyFeedActivity.class); }

    private void switchToTab( Class cls) {
        Intent i = new Intent(this, cls);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void openCreatePost() {
        CreatePostActivity.startCreatePostActivityForResult(this, CREATE_POST_ACTIVITY_REQUEST_CODE);
    }
}
