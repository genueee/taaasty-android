package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.FabHelper;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.widgets.FabMenuLayout;

/**
 * Избранные и скрытые
 */
public class AdditionalFeedActivity extends ActivityBase implements MyAdditionalFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "AdditionalFeedActivity";

    public static final int SECTION_FAVORITES = 1;
    public static final int SECTION_HIDDEN = 2;

    public static final String ARG_KEY_SHOW_SECTION = "ru.taaasty.ui.feeds.AdditionalFeedActivity.KEY_SHOW_PAGE";

    private static final String KEY_CURRENT_USER = "ru.taaasty.ui.feeds.AdditionalFeedActivity.KEY_CURRENT_USER";
    private static final String KEY_CURRENT_USER_DESIGN = "ru.taaasty.ui.feeds.AdditionalFeedActivity.KEY_CURRENT_USER_DESIGN";

    private User mCurrentUser;
    private TlogDesign mCurrentUserDesign;

    private FabHelper mFabHelper;

    private FabHelper.AutoHideScrollListener mAutoHideScrollListener;

    private boolean mScheduleRefresh;

    public static void startFavoriteFeedActivity(Context source, View animateFrom) {
        startAdditionalFeedActivity(source, SECTION_FAVORITES, animateFrom);
    }

    public static void startPrivateFeedActivity(Context source, View animateFrom) {
        startAdditionalFeedActivity(source, SECTION_HIDDEN, animateFrom);
    }

    private static void startAdditionalFeedActivity(Context source, int section, View animateFrom) {
        Intent intent = new Intent(source, AdditionalFeedActivity.class);
        intent.putExtra(ARG_KEY_SHOW_SECTION, section);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity) source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Session.getInstance().getCachedCurrentUser() != null
                && Session.getInstance().getCachedCurrentUser().getDesign() != null) {
            TlogDesign design = Session.getInstance().getCachedCurrentUser().getDesign();
            if (design.isDarkTheme()) {
                setTheme(R.style.AppThemeDark);
            } else {
                setTheme(R.style.AppTheme);
            }
        }

        setContentView(R.layout.activity_additional_feed);

        getWindow().getDecorView().setBackgroundDrawable(null); // Используем background у RecyclerView

        int currentSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, 1);

        if (savedInstanceState != null) {
            mCurrentUser = savedInstanceState.getParcelable(KEY_CURRENT_USER);
            mCurrentUserDesign = savedInstanceState.getParcelable(KEY_CURRENT_USER_DESIGN);
        } else {
            Fragment fragment;
            switch (currentSection) {
                case SECTION_FAVORITES:
                    fragment = MyAdditionalFeedFragment.newInstance(MyAdditionalFeedFragment.FEED_TYPE_FAVORITES);
                    break;
                case SECTION_HIDDEN:
                    fragment = MyAdditionalFeedFragment.newInstance(MyAdditionalFeedFragment.FEED_TYPE_PRIVATE);
                    break;
                default:
                    throw new IllegalArgumentException("incorrect current section");
            }
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }

        mFabHelper = new FabHelper(findViewById(R.id.fab), (FabMenuLayout)findViewById(R.id.fab_menu));
        mFabHelper.setMenuListener(new FabHelper.FabMenuDefaultListener(this));
        mAutoHideScrollListener = new FabHelper.AutoHideScrollListener(mFabHelper);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_CODE_CREATE_POST:
                if (resultCode == Activity.RESULT_OK) {
                    mScheduleRefresh = true;
                }
                break;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mScheduleRefresh) {
            mScheduleRefresh = false;
            refreshData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_my_additional_feed, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mFabHelper.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            MessageHelper.showError(this, error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            MessageHelper.showError(this, error, exception);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_refresh:
                refreshData();
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
    public void onCurrentUserAvatarClicked(View view, User user, TlogDesign design) {
        new UserInfoActivity.Builder(this)
                .set(user, view, design)
                .setPreloadAvatarThumbnail(R.dimen.feed_header_avatar_normal_diameter)
                .setBackgroundThumbnailKey(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY)
                .startActivity();
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view, R.dimen.avatar_extra_small_diameter_34dp);
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        SharePostActivity.startActivity(this, entry);
    }

    @Override
    public RecyclerView.OnScrollListener getFragmentScrollListener() {
        return mAutoHideScrollListener;
    }


    void refreshData() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.container);
        if (current != null) ((IRereshable)current).refreshData(true);
    }
}