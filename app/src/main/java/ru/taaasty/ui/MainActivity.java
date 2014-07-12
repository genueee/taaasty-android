package ru.taaasty.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.feeds.LiveFeedFragment;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.widgets.Tabbar;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends Activity implements
        MyFeedFragment.OnFragmentInteractionListener,
        LiveFeedFragment.OnFragmentInteractionListener {

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX
         if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        setContentView(R.layout.activity_main);

        mTabbar = (Tabbar)findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        if (savedInstanceState == null) switchToLiveFeed();
    }


    @Override
    public void onFeedButtonClicked(Uri uri) {

    }

    private void switchToLoginForm() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToMyFeed() {
        if (mTabbar.getActivatedViewId() == R.id.btn_tabbar_my_feed) {
            MyFeedFragment f = (MyFeedFragment)getFragmentManager().findFragmentById(R.id.container);
            f.refreshData();
        } else {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MyFeedFragment.newInstance())
                    .commit();
            mTabbar.setActivated(R.id.btn_tabbar_my_feed);
        }
    }

    void switchToLiveFeed() {
        if (mTabbar.getActivatedViewId() == R.id.btn_tabbar_live) {
            LiveFeedFragment f = (LiveFeedFragment)getFragmentManager().findFragmentById(R.id.container);
            f.refreshData();
        } else {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, LiveFeedFragment.newInstance())
                    .commit();
            mTabbar.setActivated(R.id.btn_tabbar_live);
        }
    }

    void switchToPostContent() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void switchToNotifications() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void switchToSubscribtions() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    private Tabbar.onTabbarButtonListener mTabbarListener = new Tabbar.onTabbarButtonListener() {
        @Override
        public void onTabbarButtonClicked(View v) {
            switch (v.getId()) {
                case R.id.btn_tabbar_my_feed:
                    switchToMyFeed();
                    break;
                case R.id.btn_tabbar_live:
                    switchToLiveFeed();
                    break;
                case R.id.btn_tabbar_post:
                    switchToPostContent();
                    break;
                case R.id.btn_tabbar_notifications:
                    switchToNotifications();
                    break;
                case R.id.btn_tabbar_subscribtions:
                    switchToSubscribtions();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    };

}
