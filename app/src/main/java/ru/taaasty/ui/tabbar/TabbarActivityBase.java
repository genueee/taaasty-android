package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.support.annotation.LayoutRes;
import android.util.Log;
import android.view.View;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.ui.feeds.AdditionalFeedActivity;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;

public abstract class TabbarActivityBase extends ActivityBase implements TabbarFragment.onTabbarButtonListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TabbarActivityBase";

    public static final int REQUEST_CODE_LOGIN = 1;

    Session mSession = Session.getInstance();
    TabbarFragment mTabbar;

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
        if (Session.getInstance().isAuthorized()) PusherService.startPusher(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_CODE_CREATE_POST:
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
    public void onBackPressed() {
        if (mTabbar != null) {
            if (mTabbar.onBackPressed()) return;
        }
        super.onBackPressed();
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
    public boolean onFabMenuItemClicked(View v) {
        return false;
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
}
