package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.AdditionalMenuActivity;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.feeds.MyAdditionalFeedActivity;
import ru.taaasty.ui.feeds.MyAdditionalFeedFragment;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class MyFeedActivity extends Activity implements
        MyAdditionalFeedFragment.OnFragmentInteractionListener,
        MyFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedActivity";
    private static final int ADDITIONAL_MENU_REQUEST_CODE = 1;

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

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

        setContentView(R.layout.activity_my_feed);

        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        mTabbar.setActivated(R.id.btn_tabbar_my_feed);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
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

    void switchToLiveFeed() {
        Intent i = new Intent(this, LiveFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToSubscribtions() {
        Intent i = new Intent(this, SubscribtionsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private Tabbar.onTabbarButtonListener mTabbarListener = new Tabbar.onTabbarButtonListener() {
        @Override
        public void onTabbarButtonClicked(View v) {
            switch (v.getId()) {
                case R.id.btn_tabbar_my_feed:
                    // XXX
                    break;
                case R.id.btn_tabbar_live:
                    switchToLiveFeed();
                    break;
                case R.id.btn_tabbar_subscribtions:
                    switchToSubscribtions();
                    break;
                default:
                    if (DBG) Log.v(TAG, "onTabbarButtonListener " + v.getId());
                    Toast.makeText(MyFeedActivity.this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

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

    @Override
    public void onShowAdditionalmenuClicked() {
        Intent i = new Intent(this, AdditionalMenuActivity.class);
        startActivityForResult(i, ADDITIONAL_MENU_REQUEST_CODE);
    }

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        if (user == null) return;
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    public void onAdditionMenuItemClicked(int viewId) {
        switch (viewId) {
            case R.id.profile:
            case R.id.back_button:
                break;
            case R.id.favorites:
                openAdditionalFeed(MyAdditionalFeedActivity.FEED_TYPE_FAVORITES);
                break;
            case R.id.hidden:
                openAdditionalFeed(MyAdditionalFeedActivity.FEED_TYPE_PRIVATE);
                break;
            case R.id.friends:
                openAdditionalFeed(MyAdditionalFeedActivity.FEED_TYPE_FRIENDS);
                break;
            case R.id.settings:
                if (DBG) Log.v(TAG, "onAdditionMenuItemClicked settings");
                Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    void openAdditionalFeed(@MyAdditionalFeedActivity.FeedType int type) {
        Intent i = new Intent(this, MyAdditionalFeedActivity.class);
        i.putExtra(MyAdditionalFeedActivity.ARG_KEY_FEED_TYPE, type);
        startActivity(i);
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private Fragment mCurrentPrimaryItem;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return MyFeedFragment.newInstance();
                case 1:
                    return MyAdditionalFeedFragment.newInstance(MyAdditionalFeedActivity.FEED_TYPE_FAVORITES, position, getCount());
                case 2:
                    return MyAdditionalFeedFragment.newInstance(MyAdditionalFeedActivity.FEED_TYPE_PRIVATE, position, getCount());
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (mCurrentPrimaryItem != object) {
                mCurrentPrimaryItem = (Fragment) object;
                // onPrimaryItemChanged(mCurrentPrimaryItem);
            }
        }

        public Fragment getCurrentPrimaryItem() {
            return mCurrentPrimaryItem;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_my_feed).toUpperCase(Locale.getDefault());
                case 1:
                    return getString(R.string.title_favorites).toUpperCase(Locale.getDefault());
                case 2:
                    return getString(R.string.title_hidden_entries).toUpperCase(Locale.getDefault());
            }
            return null;
        }
    }
}