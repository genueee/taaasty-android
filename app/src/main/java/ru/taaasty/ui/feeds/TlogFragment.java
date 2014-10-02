package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxListView;

import java.util.Locale;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class TlogFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogFragment";
    private static final String ARG_USER_ID = "user_id";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private ParallaxListView mListView;
    private View mEmptyView;
    private ViewGroup mHeaderView;

    private ApiTlog mTlogService;
    private FeedItemAdapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
    private Subscription mUserSubscribtion = SubscriptionHelper.empty();

    private long mUserId;
    private TlogInfo mTlogInfo;

    // XXX: anti picasso weak ref
    private TargetSetHeaderBackground mFeedDesignTarget;

    private int mRefreshCounter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static TlogFragment newInstance(long userId) {
        TlogFragment f = new  TlogFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        f.setArguments(b);
        return f;
    }

    public TlogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mUserId = args.getLong(ARG_USER_ID);
        mTlogService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_tlog, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (ParallaxListView) v.findViewById(R.id.list_view);
        mHeaderView = (ViewGroup) inflater.inflate(R.layout.header_tlog, mListView, false);
        mEmptyView = v.findViewById(R.id.empty_view);

        mHeaderView.findViewById(R.id.avatar).setOnClickListener(mOnClickListener);

        mRefreshLayout.setOnRefreshListener(this);
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                View child = view.getChildAt(0);
                float firstVisibleFract;
                if (child == null) {
                    firstVisibleFract = 0;
                } else {
                    firstVisibleFract = -1f * (float) child.getTop() / (float) child.getHeight();
                }

                if (mListener != null) mListener.onListScroll(firstVisibleItem,
                        UiUtils.clamp(firstVisibleFract, 0f, 0.99f), visibleItemCount, totalItemCount);
            }
        });

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListView.addParallaxedHeaderView(mHeaderView);
        mAdapter = new FeedItemAdapter(getActivity(), mOnFeedItemClickListener, false, false);
        mListView.setAdapter(mAdapter);

        if (!mRefreshLayout.isRefreshing()) refreshData();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.avatar:
                    onAvatarClicked(v);
                    break;
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mUserSubscribtion.unsubscribe();
        mListView = null;
        mAdapter = null;
        mFeedDesignTarget = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    void setRefreshing(boolean refresh) {
        if (refresh) {
            mRefreshCounter += 1;
            if (mRefreshCounter == 1) mRefreshLayout.setRefreshing(true);
        } else {
            if (mRefreshCounter > 0) {
                mRefreshCounter -= 1;
                if (mRefreshCounter == 0) mRefreshLayout.setRefreshing(false);
            }
        }
        if (DBG) Log.v(TAG, "setRefreshing " + refresh + " counter: " + mRefreshCounter);
    }

    public void refreshData() {
        if (DBG) Log.v(TAG, "refreshData()");
        refreshUser();
        refreshFeed();
    }

    void onAvatarClicked(View v) {
        if (mListener != null) mListener.onAvatarClicked(
                mTlogInfo == null ? null : mTlogInfo.author,
                mTlogInfo == null ? null : mTlogInfo.design);
    }

    void setupUser(User user) {
        if (user == null) {
            // XXX
        } else {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0,1).toUpperCase(Locale.getDefault()) + name.substring(1);
            ((TextView)mHeaderView.findViewById(R.id.user_name)).setText(name);

            setupAvatar(user);
        }
    }

    void setupFeedDesign() {
        if (mTlogInfo == null) return;
        TlogDesign design = mTlogInfo.design;

        if (DBG) Log.e(TAG, "Setup feed design " + design);
        mAdapter.setFeedDesign(design);
        if (mListener != null) mListener.setFeedBackgroundColor(design.getFeedBackgroundColor(getResources()));
        String backgroudUrl = design.getBackgroundUrl();
        mFeedDesignTarget = new TargetSetHeaderBackground(mHeaderView,
                design, Color.TRANSPARENT, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS);
        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(backgroudUrl)
                .into(mFeedDesignTarget);

    }

    private void setupAvatar(User user) {
        ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                (ImageView)mHeaderView.findViewById(R.id.avatar),
                R.dimen.avatar_normal_diameter
        );
    }

    public class LikesHelper extends ru.taaasty.utils.LikesHelper {

        public LikesHelper() {
            super(TlogFragment.this);
        }

        @Override
        public boolean isRatingInUpdate(long entryId) {
            return mAdapter.isRatingInUpdate(entryId);
        }

        @Override
        public void onRatingUpdateStart(long entryId) {
            mAdapter.onUpdateRatingStart(entryId);
        }

        @Override
        public void onRatingUpdateCompleted(Entry entry) {
            mAdapter.onUpdateRatingEnd(entry.getId());
        }

        @Override
        public void onRatingUpdateError(Throwable e, Entry entry) {
            mAdapter.onUpdateRatingEnd(entry.getId());
            if (mListener != null) mListener.notifyError(getText(R.string.error_vote), e);
        }
    }

    public final FeedItemAdapter.OnItemListener mOnFeedItemClickListener = new FeedItemAdapter.OnItemListener() {

        @Override
        public void onFeedItemClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            if (mTlogInfo != null) i.putExtra(ShowPostActivity.ARG_TLOG_DESIGN, mTlogInfo.design);
            startActivity(i);
        }

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            throw new IllegalStateException();
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            new LikesHelper().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            startActivity(i);
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onPostAdditionalMenuClicked postId: " + postId);
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
        }
    };

    public void refreshUser() {
        if (!mUserSubscribtion.isUnsubscribed()) {
            mUserSubscribtion.unsubscribe();
            mStopRefreshingAction.call();
        }
        setRefreshing(true);
        Observable<TlogInfo> observableCurrentUser = AndroidObservable.bindFragment(this,
                mTlogService.getUserInfo(String.valueOf(mUserId)));

        mUserSubscribtion = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
    }

    private void refreshFeed() {
        if (!mFeedSubscription.isUnsubscribed()) {
            mFeedSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        setRefreshing(true);
        Observable<Feed> observableFeed = AndroidObservable.bindFragment(this,
                mTlogService.getEntries(String.valueOf(mUserId), null, Constants.LIVE_FEED_INITIAL_LENGTH));
        mFeedSubscription = observableFeed
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mFeedObserver);
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            if (DBG) Log.v(TAG, "doOnTerminate()");
            setRefreshing(false);
        }
    };

    private final Observer<Feed> mFeedObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
            mEmptyView.setVisibility(mAdapter.isEmpty() ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            // XXX
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.setFeed(feed.entries);
        }
    };

    private final Observer<TlogInfo> mCurrentUserObserver = new Observer<TlogInfo>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            // XXX
            if (e instanceof NoSuchElementException) {
                setupUser(null);
            }
        }

        @Override
        public void onNext(TlogInfo info) {
            mTlogInfo = info;
            setupFeedDesign();
            setupUser(info.author);
            if (mListener != null) mListener.onTlogInfoLoaded(info);
        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void setFeedBackgroundColor(int color);
        public void onAvatarClicked(User user, TlogDesign design);
        public void onListScroll(int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount);
        public void onTlogInfoLoaded(TlogInfo info);
    }
}
