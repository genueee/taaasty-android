package ru.taaasty.ui.feeds;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import it.sephiroth.android.library.picasso.Picasso;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.ImageUtils;
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

    public static final String ARG_USER_ID = "ru.taaasty.ui.feeds.TlogActivity.user_id";

    private long mUserId;

    private Drawable mAbBackgroundDrawable;
    private Drawable mAbIconDrawable;
    int mLastAlpha = 0;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mAbTitle;

    private Subscription mFollowSubscribtion = SubscriptionHelper.empty();

    private View mSubscribeView;
    private View mUnsubscribeView;
    private View mFollowUnfollowProgressView;

    boolean mPerformSubscription;

    @Nullable
    private String mMyRelationship;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tlog);

        mUserId = getIntent().getLongExtra(ARG_USER_ID, -1);
        if (mUserId < 0) throw new IllegalArgumentException("no ARG_USER_ID");

        mAbTitle = new SpannableString("");
        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(Color.WHITE);

        mAbBackgroundDrawable = new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark));
        mAbBackgroundDrawable.setAlpha(0);
        mAbIconDrawable = new ColorDrawable(Color.TRANSPARENT);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setBackgroundDrawable(mAbBackgroundDrawable);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_tlog);
            mSubscribeView = ab.getCustomView().findViewById(R.id.subscribe);
            mSubscribeView.setOnClickListener(mOnSubscribtionClickListener);

            mUnsubscribeView = ab.getCustomView().findViewById(R.id.unsubscribe);
            mUnsubscribeView.setOnClickListener(mOnSubscribtionClickListener);

            mFollowUnfollowProgressView = ab.getCustomView().findViewById(R.id.follow_unfollow_progress);
            refreshFollowUnfollowView();
        }

        if (savedInstanceState == null) {
            Fragment tlogFragment = TlogFragment.newInstance(mUserId);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, tlogFragment)
                    .commit();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void onAvatarClicked(User user, TlogDesign design) {
        if (user == null) return;
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    @Override
    public void onTlogInfoLoaded(TlogInfo tlogInfo) {
        mMyRelationship = tlogInfo.getMyRelationship();
        User author = tlogInfo.author;
        mAbTitle = new SpannableString(author.getSlug());
        mAbTitle.setSpan(mAlphaForegroundColorSpan, 0, mAbTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ImageUtils.getInstance().loadAvatar(this, author.getUserpic(), author.getSlug(),
                mPicassoTarget, android.R.dimen.app_icon_size);
        refreshFollowUnfollowView();
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }

    @Override
    public void onListScroll(int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        float abAlpha;
        int intAlpha;

        if (totalCount == 0 || visibleCount == totalCount) {
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
            mAbIconDrawable.setAlpha(intAlpha);
            if (mAbTitle != null) {
                mAlphaForegroundColorSpan.setAlpha(abAlpha);
                getActionBar().setTitle(mAbTitle);
            }
        }
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

    void doFollow() {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindActivity(this,
                relApi.follow(String.valueOf(mUserId)));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
    }

    void doUnfollow() {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AndroidObservable.bindActivity(this,
                relApi.unfollow(String.valueOf(mUserId)));
        mPerformSubscription = true;
        refreshFollowUnfollowView();
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mFollowObserver);
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

    private final ImageUtils.DrawableTarget mPicassoTarget = new ImageUtils.DrawableTarget() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            mAbIconDrawable = new BitmapDrawable(getResources(), bitmap);
            mAbIconDrawable.setAlpha(mLastAlpha);
            getActionBar().setIcon(mAbIconDrawable);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            notifyError(getText(R.string.error_loading_image), null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        @Override
        public void onDrawableReady(Drawable drawable) {
            mAbIconDrawable = drawable;
            mAbIconDrawable.setAlpha(mLastAlpha);
            getActionBar().setIcon(mAbIconDrawable);
        }
    };
}