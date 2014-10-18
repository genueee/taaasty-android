package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.EndlessFeedGridItemAdapter;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.Stats;
import ru.taaasty.service.ApiApp;
import ru.taaasty.service.ApiFeeds;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link GridFeedFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link GridFeedFragment#createLiveFeedInstance} factory method to
 * create an instance of this fragment.
 */
public class GridFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "GridFeedFragment";

    private static final String ARG_FEED_TYPE = "feed_type";

    private static final String BUNDLE_KEY_FEED_ITEMS = "feed_items";
    private static final String BUNDLE_KEY_FEED_STATS = "feed_stats";

    private static final int FEED_LIVE = 0;
    private static final int FEED_BEST = 1;
    private static final int FEED_NEWS = 2;
    private static final int FEED_ANONYMOUS = 3;
    private static final String TLOG_NEWS = "news";

    private int mFeedType = FEED_LIVE;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private StaggeredGridView mGridView;

    private ApiFeeds mApiFeedsService;
    private ApiApp mApiStatsService;
    private FeedAdapter mAdapter;

    private View mHeaderView;
    private View mEndlessLoadingIndicatorView;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
    private Subscription mStatsSubscription = SubscriptionHelper.empty();

    private Stats mStats;

    public static GridFeedFragment createLiveFeedInstance() {
        GridFeedFragment fragment = new GridFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_LIVE);
        fragment.setArguments(args);
        return fragment;
    }

    public static GridFeedFragment createBestFeedInstance() {
        GridFeedFragment fragment = new GridFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_BEST);
        fragment.setArguments(args);
        return fragment;
    }

    public static GridFeedFragment createAnonymousFeedInstance() {
        GridFeedFragment fragment = new GridFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_ANONYMOUS);
        fragment.setArguments(args);
        return fragment;
    }

    public static GridFeedFragment createNewsFeedInstance() {
        GridFeedFragment fragment = new GridFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_NEWS);
        fragment.setArguments(args);
        return fragment;
    }

    public GridFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApiFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiFeeds.class);
        mApiStatsService = NetworkUtils.getInstance().createRestAdapter().create(ApiApp.class);
        mFeedType = getArguments().getInt(ARG_FEED_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grid_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mGridView = (StaggeredGridView) mRefreshLayout.findViewById(R.id.live_feed_grid_view);
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
        mHeaderView = LayoutInflater.from(mGridView.getContext()).inflate(R.layout.header_title_subtitle, mGridView, false);
        mEndlessLoadingIndicatorView = LayoutInflater.from(mGridView.getContext()).inflate(R.layout.endless_loading_indicator, mGridView, false);
        mEndlessLoadingIndicatorView.setVisibility(View.INVISIBLE);

        setupFeedTitle();

        if (DBG) Log.v(TAG, "onActivityCreated " + getString(getTitle()) + " savedInstanceState: " + (savedInstanceState == null ? " null " : " not null" ));

        // mGridView.addParallaxedHeaderView(headerView);
        mGridView.addHeaderView(mHeaderView, null, false);
        mGridView.addFooterView(mEndlessLoadingIndicatorView);

        mAdapter = new FeedAdapter(getActivity());
        mGridView.setAdapter(mAdapter);

        if (savedInstanceState != null) {
            ArrayList<Entry> entries = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
            if (entries != null) {
                mAdapter.setFeed(entries);
            }

            mStats = (Stats)savedInstanceState.getParcelable(BUNDLE_KEY_FEED_STATS);
            refreshFeedDescription();
        }

        if (mAdapter.getFeed().isEmpty() && !mRefreshLayout.isRefreshing()) refreshData();

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long postId) {
                if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
                Intent i = new Intent(getActivity(), ShowPostActivity.class);
                i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
                startActivity(i);
            }
        });

        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (mListener != null) {
                    View topView = view.getChildAt(0);
                    if (topView != null) {
                        int top = topView.getTop();
                        mListener.onGridTopViewScroll(GridFeedFragment.this, firstVisibleItem == 0, top);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> entries = mAdapter.getFeed();
            ArrayList<Entry> entriesArrayList = new ArrayList<>(entries);
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, entriesArrayList);
        }

        if(mStats != null) {
            outState.putParcelable(BUNDLE_KEY_FEED_STATS, mStats);
        }
    }

    @Override
    public void onDestroyView() {
        mFeedSubscription.unsubscribe();
        mStatsSubscription.unsubscribe();
        mAdapter.onDestroy();
        super.onDestroyView();
        mGridView = null;
        mAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public boolean isFirstChildVisisble() {
        if (mGridView == null) return false;
        View v0 = mGridView.getChildAt(0);
        if (v0 == null) return false;
        return mGridView.getFirstVisiblePosition() == 0;
    }

    public int getFirstChildTop() {
        if (mGridView == null) return 0;
        View v0 = mGridView.getChildAt(0);
        if (v0 == null) return 0;
        return v0.getTop();
    }

    @Override
    public void onRefresh() {
        refreshData();
    }

    private Observable<Feed> createObservabelFeed(@Nullable Long sinceEntryId, int length) {
        switch (mFeedType) {
            case FEED_LIVE:
                return mApiFeedsService.getLiveFeed(sinceEntryId, length);
            case FEED_BEST:
                return mApiFeedsService.getBestFeed(sinceEntryId, length, null, null,null, null);
            case FEED_ANONYMOUS:
                return mApiFeedsService.getAnonymousFeed(sinceEntryId, length);
            case FEED_NEWS:
                ApiTlog tlogService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
                return tlogService.getEntries(TLOG_NEWS, sinceEntryId, length);
            default:
                throw new IllegalStateException();
        }
    }

    public void refreshData() {
        mFeedSubscription.unsubscribe();
        mStatsSubscription.unsubscribe();
        mRefreshLayout.setRefreshing(true);
        if (DBG) Log.v(TAG, "setRefreshing true");
        mFeedSubscription = AndroidObservable.bindFragment(this, createObservabelFeed(null, Constants.LIVE_FEED_INITIAL_LENGTH))
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        if (DBG) Log.v(TAG, "finallyDo()");
                        mRefreshLayout.setRefreshing(false);
                        if (DBG) Log.v(TAG, "setRefreshing false");
                    }
                })
                .subscribe(mFeedObserver);

        mStatsSubscription = AndroidObservable.bindFragment( this, mApiStatsService.getStats()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(mStatsObserver);
    }

    private void setupFeedTitle() {
        TextView titleView = (TextView)mHeaderView.findViewById(R.id.title);
        titleView.setText(getTitle());
    }

    private int getTitle() {
        switch (mFeedType) {
            case FEED_LIVE:
                return R.string.title_live_feed;
            case FEED_BEST:
                return R.string.title_best_feed;
            case FEED_NEWS:
                return R.string.title_news;
            case FEED_ANONYMOUS:
                return R.string.title_anonymous_feed;
            default:
                throw new IllegalStateException();
        }
    }

    private void refreshFeedDescription() {
        TextView descView = (TextView)mHeaderView.findViewById(R.id.subtitle);

        if (mAdapter == null || mStats == null) {
            descView.setVisibility(View.INVISIBLE);
            return;
        }

        int title = getTitle();

        String entries = "";

        switch(title)
        {
            case R.string.title_live_feed:
                entries = getResources().getQuantityString(R.plurals.public_records_last_day, mStats.publicEntriesInDayCount, mStats.publicEntriesInDayCount);
                break;
            case R.string.title_best_feed:
                entries = getResources().getQuantityString(R.plurals.best_records_last_day, mStats.bestEntriesInDayCount, mStats.bestEntriesInDayCount);
                break;
            case R.string.title_anonymous_feed:
                entries = getResources().getQuantityString(R.plurals.anonymous_records_last_day, mStats.anonymousEntriesInDayCount, mStats.anonymousEntriesInDayCount);
                break;
            default:
                {
                    // Новости
                    int count = UiUtils.getEntriesLastDay(mAdapter.getFeed());
                    entries = getResources().getQuantityString(R.plurals.news_last_day, count, count);
                }
                break;
        }
        descView.setText(entries);
        descView.setVisibility(View.VISIBLE);
    }

    private final Observer<Feed> mFeedObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.setFeed(feed.entries);
        }
    };

    private final Observer<Stats> mStatsObserver = new Observer<Stats>() {
        @Override
        public void onCompleted() {
            refreshFeedDescription();
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public void onNext(Stats st) {
            mStats = st;
        }
    };

    public class FeedAdapter extends EndlessFeedGridItemAdapter {

        public FeedAdapter(Context context) {
            super(context);
        }

        @Override
        public void onLoadingStarted() {
            if (mEndlessLoadingIndicatorView != null) mEndlessLoadingIndicatorView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onLoadingCompleted() {
            if (mEndlessLoadingIndicatorView != null) mEndlessLoadingIndicatorView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onRemoteError(Throwable e) {
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public Observable<Feed> createObservable(Long sinceEntryId) {
            return AndroidObservable.bindFragment(GridFeedFragment.this,
                    createObservabelFeed(sinceEntryId, Constants.LIVE_FEED_INITIAL_LENGTH));
        }
    }

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
        // TODO: Update argument type and name
        public void onFeedButtonClicked(Uri uri);
        public void onGridTopViewScroll(Fragment fragment, boolean firstChildVisible, int firstItemTop);
    }
}
