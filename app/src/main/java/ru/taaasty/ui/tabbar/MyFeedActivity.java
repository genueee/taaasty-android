package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.Locale;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.AdditionalMenuActivity;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.feeds.IRereshable;
import ru.taaasty.ui.feeds.MyAdditionalFeedActivity;
import ru.taaasty.ui.feeds.MyAdditionalFeedFragment;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.ui.relationships.FollowingFollowersActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;


public class MyFeedActivity extends ActivityBase implements
        MyAdditionalFeedFragment.OnFragmentInteractionListener,
        MyFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedActivity";

    public static final int ADDITIONAL_MENU_REQUEST_CODE = 1;
    public static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 4;

    public static final String ARG_KEY_SHOW_SECTION = "ru.taaasty.ui.tabbar.MyAdditionalFeedFragment.KEY_SHOW_PAGE";

    public static final int SECTION_MY_TLOG = 0;
    public static final int SECTION_FAVORITES = 1;
    public static final int SECTION_HIDDEN = 2;

    private static final String KEY_CURRENT_USER = "ru.taaasty.ui.tabbar.MyFeedActivity.KEY_CURRENT_USER";
    private static final String KEY_CURRENT_USER_DESIGN = "ru.taaasty.ui.tabbar.MyFeedActivity.KEY_CURRENT_USER_DESIGN";

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    private User mCurrentUser;
    private TlogDesign mCurrentUserDesign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX
        if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        setContentView(R.layout.activity_my_feed);

        if (savedInstanceState != null) {
            mCurrentUser = savedInstanceState.getParcelable(KEY_CURRENT_USER);
            mCurrentUserDesign = savedInstanceState.getParcelable(KEY_CURRENT_USER_DESIGN);
        }

        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        mTabbar.setActivated(R.id.btn_tabbar_my_feed);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int initialSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, SECTION_MY_TLOG);
        mViewPager.setCurrentItem(initialSection, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ADDITIONAL_MENU_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    int viewId = data.getIntExtra(AdditionalMenuActivity.RESULT_REQUESTED_VIEW_ID, 0);
                    onAdditionMenuItemClicked(viewId);
                }
                break;
            case CREATE_POST_ACTIVITY_REQUEST_CODE:
                switch (resultCode) {
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                        if (mViewPager != null) mViewPager.setCurrentItem(SECTION_MY_TLOG, false);
                        break;
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                        if (mViewPager != null) mViewPager.setCurrentItem(SECTION_HIDDEN, false);
                        break;
                }
                // говно
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mSectionsPagerAdapter == null) return;
                        Fragment current = mSectionsPagerAdapter.getCurrentPrimaryItem();
                        if (current != null) ((IRereshable)current).refreshData();
                    }
                }, 200);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentUser != null) outState.putParcelable(KEY_CURRENT_USER, mCurrentUser);
        if (mCurrentUserDesign != null) outState.putParcelable(KEY_CURRENT_USER_DESIGN, mCurrentUserDesign);
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

    void openCreatePost() {
        Intent i = new Intent(this, CreatePostActivity.class);
        startActivityForResult(i, CREATE_POST_ACTIVITY_REQUEST_CODE);
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
                case R.id.btn_tabbar_post:
                    openCreatePost();
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
    public void onShowAdditionalMenuClicked() {
        Intent i = new Intent(this, AdditionalMenuActivity.class);
        startActivityForResult(i, ADDITIONAL_MENU_REQUEST_CODE);
    }

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    @Override
    public void onCurrentUserLoaded(User user, TlogDesign design) {
        mCurrentUser = user;
        mCurrentUserDesign = design;
    }

    public void openUserFeed(User user, TlogDesign design) {
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    public void onAdditionMenuItemClicked(int viewId) {
        switch (viewId) {
            case R.id.back_button:
                break;
            case R.id.friends:
                openFriends();
                break;
            case R.id.settings:
                if (DBG) Log.v(TAG, "onAdditionMenuItemClicked settings");
                Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    void openFriends() {
        if (mCurrentUser == null) return;
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER, mCurrentUser);
        i.putExtra(FollowingFollowersActivity.ARG_KEY_SHOW_SECTION, FollowingFollowersActivity.SECTION_FRIENDS);
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
                case SECTION_MY_TLOG:
                    return MyFeedFragment.newInstance();
                case SECTION_FAVORITES:
                    return MyAdditionalFeedFragment.newInstance(MyAdditionalFeedActivity.FEED_TYPE_FAVORITES, position, getCount());
                case SECTION_HIDDEN:
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
                case SECTION_MY_TLOG:
                    return getString(R.string.title_my_feed).toUpperCase(Locale.getDefault());
                case SECTION_FAVORITES:
                    return getString(R.string.title_favorites).toUpperCase(Locale.getDefault());
                case SECTION_HIDDEN:
                    return getString(R.string.title_hidden_entries).toUpperCase(Locale.getDefault());
            }
            return null;
        }
    }
}