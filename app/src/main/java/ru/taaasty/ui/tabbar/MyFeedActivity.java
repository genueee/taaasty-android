package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FragmentStatePagerAdapterBase;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.AdditionalMenuActivity;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.feeds.IRereshable;
import ru.taaasty.ui.feeds.MyAdditionalFeedFragment;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.ui.relationships.FollowingFollowersActivity;
import ru.taaasty.utils.NetworkUtils;


public class MyFeedActivity extends TabbarActivityBase implements
        MyAdditionalFeedFragment.OnFragmentInteractionListener,
        MyFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedActivity";

    public static final int ADDITIONAL_MENU_REQUEST_CODE = 1;

    public static final String ARG_KEY_SHOW_SECTION = "ru.taaasty.ui.tabbar.MyAdditionalFeedFragment.KEY_SHOW_PAGE";

    public static final int SECTION_MY_TLOG = 0;
    public static final int SECTION_FAVORITES = 1;
    public static final int SECTION_HIDDEN = 2;

    private static final String KEY_CURRENT_USER = "ru.taaasty.ui.tabbar.MyFeedActivity.KEY_CURRENT_USER";
    private static final String KEY_CURRENT_USER_DESIGN = "ru.taaasty.ui.tabbar.MyFeedActivity.KEY_CURRENT_USER_DESIGN";

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    private User mCurrentUser;
    private TlogDesign mCurrentUserDesign;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_feed);

        if (savedInstanceState != null) {
            mCurrentUser = savedInstanceState.getParcelable(KEY_CURRENT_USER);
            mCurrentUserDesign = savedInstanceState.getParcelable(KEY_CURRENT_USER_DESIGN);
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int initialSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, SECTION_MY_TLOG);
        mViewPager.setCurrentItem(initialSection, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_my_feed, menu);
        return true;
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_my_feed;
    }

    @Override
    void onCurrentTabButtonClicked() {
        refreshData();
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
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshData();
                break;
            case R.id.menu_settings:
                openSettings();
                break;
            case R.id.menu_friends:
                openFriends();
                break;
            case R.id.menu_quit:
                logout();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentUser != null) outState.putParcelable(KEY_CURRENT_USER, mCurrentUser);
        if (mCurrentUserDesign != null) outState.putParcelable(KEY_CURRENT_USER_DESIGN, mCurrentUserDesign);
    }

    @Override
    void onCreatePostActivityClosed(int requestCode, int resultCode, Intent data) {
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
                refreshData();
            }
        }, 200);
    }

    @Override
    public void onShowAdditionalMenuClicked() {
        Intent i = new Intent(this, AdditionalMenuActivity.class);
        startActivityForResult(i, ADDITIONAL_MENU_REQUEST_CODE);
    }

    @Override
    public void onCurrentUserAvatarClicked(View view, User user, TlogDesign design) {
        new UserInfoActivity.Builder(this)
                .set(user, view, design)
                .setPreloadAvatarThumbnail(R.dimen.avatar_normal_diameter)
                .startActivity();
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view);
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }

    @Override
    public void onCurrentUserLoaded(User user, TlogDesign design) {
        mCurrentUser = user;
        mCurrentUserDesign = design;
    }

    void refreshData() {
        if (mSectionsPagerAdapter == null) return;
        Fragment current = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        if (current != null) ((IRereshable)current).refreshData();
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
                openSettings();
                break;
            case R.id.logout:
                logout();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    void openSettings() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    void openFriends() {
        if (mCurrentUser == null) return;
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER, mCurrentUser);
        i.putExtra(FollowingFollowersActivity.ARG_KEY_SHOW_SECTION, FollowingFollowersActivity.SECTION_FRIENDS);
        startActivity(i);
    }

    void logout() {

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getText(R.string.clean_cache));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                NetworkUtils.getInstance().factoryReset(MyFeedActivity.this);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                progressDialog.dismiss();
                Intent mStartActivity = new Intent(MyFeedActivity.this, LiveFeedActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(MyFeedActivity.this, mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                finish();
                System.exit(0);
            }
        }.execute();
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case SECTION_MY_TLOG:
                    return MyFeedFragment.newInstance();
                case SECTION_FAVORITES:
                    return MyAdditionalFeedFragment.newInstance(MyAdditionalFeedFragment.FEED_TYPE_FAVORITES, position, getCount());
                case SECTION_HIDDEN:
                    return MyAdditionalFeedFragment.newInstance(MyAdditionalFeedFragment.FEED_TYPE_PRIVATE, position, getCount());
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 3;
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