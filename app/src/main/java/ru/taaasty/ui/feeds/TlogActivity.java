package ru.taaasty.ui.feeds;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.AlphaForegroundColorSpan;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;


public class TlogActivity extends ActivityBase implements TlogFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogActivity";

    private static final String ARG_USER_ID = "ru.taaasty.ui.feeds.TlogActivity.user_id";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "ru.taaasty.ui.feeds.TlogActivity.avatar_thumbnail_res";
    private static final String ARG_USER_SLUG = "ru.taaasty.ui.feeds.TlogActivity.user_slug";

    private static final String BUNDLE_KEY_LAST_ALPHA = "ru.taaasty.ui.feeds.TlogActivity.BUNDLE_KEY_LAST_ALPHA";
    private static final String BUNDLE_KEY_AB_TITLE = "ru.taaasty.ui.feeds.TlogActivity.BUNDLE_KEY_AB_TITLE";
    private static final String BUNDLE_KEY_IS_NAVIGATION_HIDDEN = "ru.taaasty.ui.feeds.TlogActivity.BUNDLE_KEY_IS_NAVIGATION_HIDDEN";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private Drawable mAbBackgroundDrawable;
    int mLastAlpha = 0;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mAbTitle;

    private Subscription mFollowSubscription = Subscriptions.unsubscribed();

    private View mSubscribeView;
    private View mUnsubscribeView;
    private View mFollowUnfollowProgressView;

    boolean mPerformSubscription;

    @Nullable
    private String mMyRelationship;

    private InterfaceVisibilityController mInterfaceVisibilityController;

    public static void startTlogActivity(Context source, long userId, View animateFrom, int avatarThumbnailSizeRes) {
        Intent intent = new Intent(source, TlogActivity.class);
        intent.putExtra(ARG_USER_ID, userId);
        intent.putExtra(ARG_AVATAR_THUMBNAIL_RES, avatarThumbnailSizeRes);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity)source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    public static void startTlogActivity(Context source, long userId, View animateFrom) {
        startTlogActivity(source, userId, animateFrom, R.dimen.avatar_extra_small_diameter_34dp);
    }

    public static void startTlogActivity(Context source, String userSlug, View animateFrom) {
        Intent intent = new Intent(source, TlogActivity.class);
        intent.putExtra(ARG_USER_SLUG, userSlug);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity)source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tlog);

        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(Color.WHITE);
        mAbBackgroundDrawable = new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark));

        mInterfaceVisibilityController = new InterfaceVisibilityController();
        if (savedInstanceState != null) {
            mAbTitle = new SpannableString(savedInstanceState.getString(BUNDLE_KEY_AB_TITLE));
            mLastAlpha = savedInstanceState.getInt(BUNDLE_KEY_LAST_ALPHA);
            mAbBackgroundDrawable.setAlpha(mLastAlpha);
        } else {
            mAbTitle = new SpannableString("");
            mAbBackgroundDrawable.setAlpha(0);
        }

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_tlog);
            ab.setBackgroundDrawable(mAbBackgroundDrawable);
            ab.setTitle(mAbTitle);

            if (savedInstanceState == null) {
                ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
            }

            mSubscribeView = ab.getCustomView().findViewById(R.id.subscribe);
            mSubscribeView.setOnClickListener(mOnSubscriptionClickListener);

            mUnsubscribeView = ab.getCustomView().findViewById(R.id.unsubscribe);
            mUnsubscribeView.setOnClickListener(mOnSubscriptionClickListener);

            mFollowUnfollowProgressView = ab.getCustomView().findViewById(R.id.follow_unfollow_progress);
            refreshFollowUnfollowView();
        }

        if (savedInstanceState == null) {
            Fragment tlogFragment;

            if (getIntent().hasExtra(ARG_USER_ID)) {
                long userId = getIntent().getLongExtra(ARG_USER_ID, -1);
                tlogFragment = TlogFragment.newInstance(userId, getIntent().getIntExtra(ARG_AVATAR_THUMBNAIL_RES, 0));
            } else {
                String userIdOrSlug = getIntent().getStringExtra(ARG_USER_SLUG);
                tlogFragment = TlogFragment.newInstance(userIdOrSlug);
            }

            getFragmentManager().beginTransaction()
                    .replace(R.id.container, tlogFragment)
                    .commit();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_KEY_LAST_ALPHA, mLastAlpha);
        outState.putString(BUNDLE_KEY_AB_TITLE, mAbTitle.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        return true;
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
    protected void onResume() {
        super.onResume();
        mInterfaceVisibilityController.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFollowSubscription.unsubscribe();
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

    @Override
    public void setFeedBackgroundColor(int color) {
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    public void onTlogInfoLoaded(TlogInfo tlogInfo) {
        mMyRelationship = tlogInfo.getMyRelationship();
        User author = tlogInfo.author;
        mAbTitle = new SpannableString(author.getName());
        mAbTitle.setSpan(mAlphaForegroundColorSpan, 0, mAbTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        refreshFollowUnfollowView();
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }

    @Override
    public void onListClicked() {
        mInterfaceVisibilityController.onListClicked();
    }

    @Override
    public void onNoSuchUser() {
        Toast.makeText(this, getString(R.string.error_user_with_this_name_not_found), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean isOverlayVisible() {
        return !mInterfaceVisibilityController.isNavigationHidden();
    }

    @Override
    public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        float abAlpha;
        int intAlpha;

        mInterfaceVisibilityController.onListScroll(dy, firstVisibleItem, firstVisibleFract, visibleCount, totalCount);

        // XXX: неверно работает, когда у юзера мало постов
        if (totalCount == 0 || visibleCount >= totalCount) {
            abAlpha = 0;
        } else {
            if (totalCount > 5) totalCount = 5;
            abAlpha = (firstVisibleItem + firstVisibleFract) / (float)totalCount;
            abAlpha = UiUtils.clamp(abAlpha, 0f, 1f);
        }

        intAlpha = (int)(255f * abAlpha);

        if (intAlpha == 0
                || (intAlpha == 255 && mLastAlpha != 255)
                || Math.abs(mLastAlpha - intAlpha) > 20) {
            mLastAlpha = intAlpha;
            mAbBackgroundDrawable.setAlpha(intAlpha);
            if (mAbTitle != null) {
                mAlphaForegroundColorSpan.setAlpha(abAlpha);
                getActionBar().setTitle(mAbTitle);
            }
        }
    }

    private void refreshData() {
        TlogFragment fragment = (TlogFragment)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.refreshData(true);
    }

    void refreshFollowUnfollowView() {
        if (mPerformSubscription) {
            mSubscribeView.setVisibility(View.INVISIBLE);
            mUnsubscribeView.setVisibility(View.INVISIBLE);
            mFollowUnfollowProgressView.setVisibility(View.VISIBLE);
            return;
        }

        if (mMyRelationship == null) {
            mSubscribeView.setVisibility(View.INVISIBLE);
            mUnsubscribeView.setVisibility(View.INVISIBLE);
            mFollowUnfollowProgressView.setVisibility(View.GONE);
            return;
        }

        boolean meSubscribed = Relationship.isMeSubscribed(mMyRelationship);
        mSubscribeView.setVisibility(meSubscribed ? View.INVISIBLE : View.VISIBLE);
        mUnsubscribeView.setVisibility(meSubscribed ? View.VISIBLE : View.INVISIBLE);
        mFollowUnfollowProgressView.setVisibility(View.GONE);
    }

    @Nullable
    private Long getUserId() {
        TlogFragment fragment = (TlogFragment)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) {
            return ((TlogFragment)fragment).getUserId();
        }
        return null;
    }

    void doFollow() {
        if (getUserId() == null) return;
        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindActivity(this,
                relApi.follow(getUserId().toString()));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    void doUnfollow() {
        if (getUserId() == null) return;
        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindActivity(this,
                relApi.unfollow(getUserId().toString()));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    private final class InterfaceVisibilityController {

        private final Handler mHideActionBarHandler;
        private volatile boolean userForcedToShowInterface = false;
        private boolean mNavigationHidden;

        public InterfaceVisibilityController() {
            mHideActionBarHandler = new Handler();
            userForcedToShowInterface = false;
        }

        public void onResume() {
            mNavigationHidden = !getActionBar().isShowing();
            runHideActionBarTimer();
        }

        public void onDestroy() {
            mHideActionBarHandler.removeCallbacks(mHideAbRunnable);
        }

        public void onListClicked() {
            if (mNavigationHidden) {
                userForcedToShowInterface = true;
            }
            toggleShowOrHideHideyBarMode();
        }

        public boolean isNavigationHidden() {
            return mNavigationHidden;
        }

        public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
            if (dy < -50
                    || totalCount == 0
                    || (firstVisibleItem == 0 && firstVisibleFract < 0.1)
                    ) {
                userForcedToShowInterface();
            }
        }

        void onVisibilityChanged(boolean shown) {
            mNavigationHidden = !shown;
            TlogFragment fragment = (TlogFragment)getFragmentManager().findFragmentById(R.id.container);
            if (fragment != null) fragment.onOverlayVisibilityChanged(shown);
        }

        private void userForcedToShowInterface() {
            if (mNavigationHidden) {
                userForcedToShowInterface = true;
                toggleShowOrHideHideyBarMode();
            }
        }

        @SuppressLint("InlinedApi")
        private void toggleShowOrHideHideyBarMode() {
            if (!mNavigationHidden) {
                getActionBar().hide();
                onVisibilityChanged(false);
            } else {
                getActionBar().show();
                userForcedToShowInterface = false;
                onVisibilityChanged(true);
                runHideActionBarTimer();
            }
        }

        private void runHideActionBarTimer() {
            mHideActionBarHandler.removeCallbacks(mHideAbRunnable);
            mHideActionBarHandler.postDelayed(mHideAbRunnable, HIDE_ACTION_BAR_DELAY);
        }

        private final Runnable mHideAbRunnable = new Runnable() {
            @Override
            public void run() {
                if (!userForcedToShowInterface && !mNavigationHidden) {
                    toggleShowOrHideHideyBarMode();
                }
            }
        };
    }

    private final Observer<Relationship> mFollowObserver = new Observer<Relationship>() {
        @Override
        public void onCompleted() {
            mPerformSubscription = false;
            refreshFollowUnfollowView();
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_follow), e);
            mPerformSubscription = false;
            refreshFollowUnfollowView();
        }

        @Override
        public void onNext(Relationship relationship) {
            mMyRelationship = relationship.getState();
        }
    };


    private final View.OnClickListener mOnSubscriptionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.subscribe:
                    doFollow();
                    break;
                case R.id.unsubscribe:
                    doUnfollow();
                    break;
            }
        }
    };
}