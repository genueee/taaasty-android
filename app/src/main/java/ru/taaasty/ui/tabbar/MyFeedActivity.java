package ru.taaasty.ui.tabbar;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;

import io.intercom.android.sdk.Intercom;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.SettingsActivity;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.feeds.AdditionalFeedActivity;
import ru.taaasty.ui.feeds.IRereshable;
import ru.taaasty.ui.feeds.MyFeedFragment;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.ui.relationships.FollowingFollowersActivity;
import ru.taaasty.utils.NetworkUtils;


public class MyFeedActivity extends TabbarActivityBase implements MyFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedActivity";

    public static final int ADDITIONAL_MENU_REQUEST_CODE = 1;

    private DrawerLayout mDrawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_my_feed);

        getWindow().getDecorView().setBackgroundDrawable(null); // Используем background у RecyclerView

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (savedInstanceState == null) {
            Fragment fragment = MyFeedFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
        initDrawerMenu();
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
            case R.id.menu_favorites:
                openFavorites();
                break;
            case R.id.menu_support:
                openSupport();
                break;
            case R.id.menu_hidden:
                openHidden();
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
    void onCreatePostActivityClosed(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                break;
            case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                openHidden();
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
        if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            mDrawerLayout.openDrawer(Gravity.RIGHT);
        }
    }

    @Override
    public void onCurrentUserAvatarClicked(View view, User user, TlogDesign design) {
        new UserInfoActivity.Builder(this)
                .set(user, view, design)
                .setPreloadAvatarThumbnail(R.dimen.avatar_normal_diameter)
                .setBackgroundThumbnailKey(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY)
                .startActivity();
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }

    void refreshData() {
        Fragment current = getFragmentManager().findFragmentById(R.id.container);
        if (current != null) ((IRereshable)current).refreshData(true);
    }

    public void onAdditionMenuItemClicked(int viewId) {
        switch (viewId) {
            case R.id.back_button:
                break;
            case R.id.friends:
                openFriends();
                break;
            case R.id.favorites:
                openFavorites();
                break;
            case R.id.hidden:
                openHidden();
                break;
            case R.id.settings:
                if (DBG) Log.v(TAG, "onAdditionMenuItemClicked settings");
                openSettings();
                break;
            case R.id.support:
                openSupport();
                break;
            case R.id.logout:
                logout();
                break;
            default:
                throw new IllegalStateException();
        }
    }

    void initDrawerMenu() {
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAdditionMenuItemClicked(v.getId());
                mDrawerLayout.closeDrawer(Gravity.RIGHT);
            }
        };

        for (int vid: new int[] {
                R.id.settings,
                R.id.friends,
                R.id.hidden,
                R.id.favorites,
                R.id.support,
                R.id.logout

        }) {
            findViewById(vid).setOnClickListener(listener);
        }
    }

    void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    void openFriends() {
        CurrentUser user = UserManager.getInstance().getCachedCurrentUser();
        if (user == null) return;
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER, user);
        i.putExtra(FollowingFollowersActivity.ARG_KEY_SHOW_SECTION, FollowingFollowersActivity.SECTION_FRIENDS);
        startActivity(i);
    }

    void openFavorites() {
        AdditionalFeedActivity.startFavoritesActivity(this, null);
    }

    void openHidden() {
        AdditionalFeedActivity.startHiddenRecordsActivity(this, null);
    }

    void openSupport() {
        Intercom.client().displayConversationsList();
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
}