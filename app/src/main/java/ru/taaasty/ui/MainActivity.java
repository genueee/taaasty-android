package ru.taaasty.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.feeds.AdditionalMenuActivity;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.feeds.LiveFeedFragment;
import ru.taaasty.widgets.Tabbar;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MainActivity extends Activity implements
        MyFeedFragment.OnFragmentInteractionListener,
        LiveFeedFragment.OnFragmentInteractionListener {
    private static final int ADDITIONAL_MENU_REQUEST_CODE = 1;

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

        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        if (savedInstanceState == null) switchToLiveFeed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADDITIONAL_MENU_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                int viewId = data.getIntExtra(AdditionalMenuActivity.RESULT_REQUESTED_VIEW_ID, 0);
                onAdditionMenuItemClicked(viewId);
            }
        }
    }

    @Override
    public void onFeedButtonClicked(Uri uri) {

    }

    @Override
    public void onShowAdditionalmenuClicked() {
        Intent i = new Intent(this, AdditionalMenuActivity.class);
        startActivityForResult(i, ADDITIONAL_MENU_REQUEST_CODE);
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

    private void closeAllFragments() {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    void switchToMyFeed() {
        if (mTabbar.getActivatedViewId() == R.id.btn_tabbar_my_feed) {
            MyFeedFragment f = (MyFeedFragment) getFragmentManager().findFragmentById(R.id.container);
            f.refreshData();
        } else {
            closeAllFragments();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MyFeedFragment.newInstance())
                    .commit();
            mTabbar.setActivated(R.id.btn_tabbar_my_feed);
        }
    }

    void switchToLiveFeed() {
        if (mTabbar.getActivatedViewId() == R.id.btn_tabbar_live) {
            LiveFeedFragment f = (LiveFeedFragment) getFragmentManager().findFragmentById(R.id.container);
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

    public void onAdditionMenuItemClicked(int viewId) {
        switch (viewId) {
            case R.id.profile:
                break;
            case R.id.favorites:
            case R.id.hidden:
            case R.id.settings:
            case R.id.friends:
            case R.id.back_button:
                Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new IllegalStateException();
        }
    }
}
