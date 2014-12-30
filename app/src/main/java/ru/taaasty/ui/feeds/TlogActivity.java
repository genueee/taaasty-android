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
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.AlphaForegroundColorSpan;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;


public class TlogActivity extends ActivityBase implements TlogFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogActivity";

    private static final String ARG_USER_ID = "ru.taaasty.ui.feeds.TlogActivity.user_id";
    private static final String ARG_USER_SLUG = "ru.taaasty.ui.feeds.TlogActivity.user_slug";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private Drawable mAbBackgroundDrawable;
    int mLastAlpha = 0;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mAbTitle;

    private Subscription mFollowSubscribtion = SubscriptionHelper.empty();

    private View mSubscribeView;
    private View mUnsubscribeView;
    private View mFollowUnfollowProgressView;

    boolean mPerformSubscription;

    private boolean isNavigationHidden = false;
    private final Handler mHideActionBarHandler = new Handler();
    private volatile boolean userForcedToChangeOverlayMode = false;


    @Nullable
    private String mMyRelationship;

    public static void startTlogActivity(Context source, long userId, View animateFrom) {
        Intent intent = new Intent(source, TlogActivity.class);
        intent.putExtra(ARG_USER_ID, userId);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity)source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
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

        mAbTitle = new SpannableString("");
        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(Color.WHITE);

        mAbBackgroundDrawable = new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark));
        mAbBackgroundDrawable.setAlpha(0);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setBackgroundDrawable(mAbBackgroundDrawable);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_tlog);
            ab.setTitle(null);
            ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
            mSubscribeView = ab.getCustomView().findViewById(R.id.subscribe);
            mSubscribeView.setOnClickListener(mOnSubscribtionClickListener);

            mUnsubscribeView = ab.getCustomView().findViewById(R.id.unsubscribe);
            mUnsubscribeView.setOnClickListener(mOnSubscribtionClickListener);

            mFollowUnfollowProgressView = ab.getCustomView().findViewById(R.id.follow_unfollow_progress);
            refreshFollowUnfollowView();
        }

        if (savedInstanceState == null) {
            Fragment tlogFragment;

            if (getIntent().hasExtra(ARG_USER_ID)) {
                long userId = getIntent().getLongExtra(ARG_USER_ID, -1);
                tlogFragment = TlogFragment.newInstance(userId);
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
        runHideActionBarTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFollowSubscribtion.unsubscribe();
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
        if (isNavigationHidden) {
            userForcedToChangeOverlayMode = true;
            toggleShowOrHideHideyBarMode();
        }
    }

    @Override
    public void onNoSuchUser() {
        Toast.makeText(this, getString(R.string.error_user_with_this_name_not_found), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        float abAlpha;
        int intAlpha;

        if (totalCount == 0 || firstVisibleItem == 0) {
            if (isNavigationHidden) {
                userForcedToChangeOverlayMode = true;
                toggleShowOrHideHideyBarMode();
            }
        }

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
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindActivity(this,
                relApi.follow(getUserId().toString()));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    void doUnfollow() {
        if (getUserId() == null) return;
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindActivity(this,
                relApi.unfollow(getUserId().toString()));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    @SuppressLint("InlinedApi")
    public void toggleShowOrHideHideyBarMode() {
        if (!isNavigationHidden) {
            getActionBar().hide();
            isNavigationHidden = true;
        } else {
            getActionBar().show();
            isNavigationHidden = false;
            userForcedToChangeOverlayMode = false;
            runHideActionBarTimer();
        }
    }

    private void runHideActionBarTimer() {
        mHideActionBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!userForcedToChangeOverlayMode && !isNavigationHidden) {
                    toggleShowOrHideHideyBarMode();
                }
            }
        }, HIDE_ACTION_BAR_DELAY);
    }

    private final Observer<Relationship> mFollowObserver = new Observer<Relationship>() {
        @Override
        public void onCompleted() {
            mPerformSubscription = false;
            refreshFollowUnfollowView();
            runHideActionBarTimer();
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


    private final View.OnClickListener mOnSubscribtionClickListener = new View.OnClickListener() {
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