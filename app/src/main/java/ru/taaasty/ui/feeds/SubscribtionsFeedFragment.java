package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxListView;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.ShowPostActivity;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;


public class SubscribtionsFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedFragment";

    private static final String BUNDLE_KEY_FEED_ITEMS = "feed_items";
    private static final String BUNDLE_KEY_FEED_DESIGN = "feed_design";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private ParallaxListView mListView;
    private View mEmptyView;
    private ViewGroup mHeaderView;

    private ApiMyFeeds mFeedsService;
    private FeedItemAdapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
    private Subscription mCurrentUserSubscribtion = SubscriptionHelper.empty();

    private int mRefreshCounter;

    private TlogDesign mTlogDesign;

    // XXX: anti picasso weak ref
    private TargetSetHeaderBackground mFeedDesignTarget;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static SubscribtionsFeedFragment newInstance() {
        return new SubscribtionsFeedFragment();
    }

    public SubscribtionsFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiMyFeeds.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (ParallaxListView) v.findViewById(R.id.list_view);
        mHeaderView = (ViewGroup) inflater.inflate(R.layout.header_title_subtitle, mListView, false);
        ((TextView)mHeaderView.findViewById(R.id.title)).setText(R.string.my_subscriptions);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

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
        mAdapter = new FeedItemAdapter(getActivity(), mOnFeedItemClickListener);
        mListView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            ArrayList<Entry> entries = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
            if (entries != null) {
                mAdapter.setFeed(entries);
            }
            TlogDesign design = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_DESIGN);
            if (design != null) {
                mTlogDesign = design;
                setupFeedDesign();
            }
        }

        if (!mRefreshLayout.isRefreshing()) refreshData();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> entries = mAdapter.getFeed();
            ArrayList<Entry> entriesArrayList = new ArrayList<>(entries);
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, entriesArrayList);
        }
        if (mTlogDesign != null) {
            outState.putParcelable(BUNDLE_KEY_FEED_DESIGN, mTlogDesign);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mCurrentUserSubscribtion.unsubscribe();
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

    void setupFeedDesign() {
        if (DBG) Log.e(TAG, "Setup feed design " + mTlogDesign);

        if (mTlogDesign == null) return;

        mAdapter.setFeedDesign(mTlogDesign);
        mListView.setBackgroundDrawable(new ColorDrawable(mTlogDesign.getFeedBackgroundColor(getResources())));
        String backgroudUrl = mTlogDesign.getBackgroundUrl();
        int foregroundColor = mTlogDesign.getTitleForegroundColor(getResources());
        mFeedDesignTarget = new TargetSetHeaderBackground(mHeaderView, mTlogDesign, foregroundColor, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS);
        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(backgroudUrl)
                .into(mFeedDesignTarget);

    }

    public final FeedItemAdapter.OnItemListener mOnFeedItemClickListener = new FeedItemAdapter.OnItemListener() {

        @Override
        public void onFeedItemClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            startActivity(i);
        }

        @Override
        public void onFeedLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onFeedLikesClicked entry: " + entry);
            new LikesHelper(SubscribtionsFeedFragment.this, mAdapter) {
                @Override
                public void notifyError(String error, Throwable e) {
                    if (mListener != null) mListener.notifyError(error, e);
                }
            }.voteUnvote(entry);
        }

        @Override
        public void onFeedCommentsClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedCommentsClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            startActivity(i);
        }

        @Override
        public void onFeedAdditionalMenuClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedAdditionalMenuClicked postId: " + postId);
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
        }
    };

    public void refreshUser() {
        if (!mCurrentUserSubscribtion.isUnsubscribed()) {
            if (DBG) Log.v(TAG, "current user subscription is not unsubscribed " + mCurrentUserSubscribtion);
            mCurrentUserSubscribtion.unsubscribe();
            mStopRefreshingAction.call();
        }
        setRefreshing(true);
        Observable<CurrentUser> observableCurrentUser = AndroidObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mCurrentUserSubscribtion = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
    }

    private void refreshFeed() {
        if (!mFeedSubscription.isUnsubscribed()) {
            if (DBG) Log.v(TAG, "feed subsription is not unsubscribed " + mFeedSubscription);
            mFeedSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        setRefreshing(true);
        Observable<Feed> observableFeed = AndroidObservable.bindFragment(this,
                mFeedsService.getMyFriendsFeed(null, Constants.LIVE_FEED_INITIAL_LENGTH));
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

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            // XXX
            if (e instanceof NoSuchElementException) {
            }
        }

        @Override
        public void onNext(CurrentUser currentUser) {
            mTlogDesign = currentUser.getDesign();
            setupFeedDesign();
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
    }
}