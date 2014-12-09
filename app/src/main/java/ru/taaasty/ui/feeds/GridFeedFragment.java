package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.FeedGridItemAdapter;
import ru.taaasty.adapters.grid.GridEntryBase;
import ru.taaasty.adapters.grid.GridEntryHeader;
import ru.taaasty.adapters.grid.GridImageEntry;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.Stats;
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
public class GridFeedFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "GridFeedFragment";

    private static final String ARG_FEED_TYPE = "feed_type";

    private static final String BUNDLE_KEY_FEED_ITEMS = "feed_items";

    private static final int FEED_LIVE = 0;
    private static final int FEED_BEST = 1;
    private static final int FEED_NEWS = 2;
    private static final int FEED_ANONYMOUS = 3;

    private int mFeedType = FEED_LIVE;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mGridView;

    private ApiFeeds mApiFeedsService;
    private FeedGridItemAdapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();

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
        mFeedType = getArguments().getInt(ARG_FEED_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_grid_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mGridView = (RecyclerView) mRefreshLayout.findViewById(R.id.live_feed_grid_view);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
                if (mListener != null) mListener.startRefreshStats();
            }
        });
        EventBus.getDefault().register(this);
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

        initGridView();
        mAdapter.onCreate();
        if (DBG) Log.v(TAG, "onActivityCreated " + getString(getTitle()) + " savedInstanceState: " + (savedInstanceState == null ? " null " : " not null" ));

        if (savedInstanceState != null) {
            ArrayList<Entry> entries = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
            if (entries != null) {
                mAdapter.setFeed(entries);
            }
        }

        if (mAdapter.getFeed().isEmpty() && !mRefreshLayout.isRefreshing()) refreshData();

    }

    private void initGridView() {
        final View.OnClickListener onItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mGridView != null) {
                    long postId = mGridView.getChildItemId(v);
                    if (postId != View.NO_ID) {
                        if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
                        Bitmap thumbnail = null;
                        RecyclerView.ViewHolder holder = mGridView.getChildViewHolder(v);
                        if (holder instanceof GridImageEntry) {
                            thumbnail = ((GridImageEntry)holder).getCachedImage();
                            if (thumbnail == null) {
                                if (DBG) Log.v(TAG, "thumbnail is null");
                            }
                        }

                        new ShowPostActivity.Builder(getActivity())
                                .setEntryId(postId)
                                .setEntry(mAdapter.getItemById(postId))
                                .setThumbnailBitmap(thumbnail, "GridFeedFragment-" + postId + "-")
                                .setThumbnailBitmap(thumbnail, "GridFeedFragment-" + postId + "-")
                                .setSrcView(v)
                                .startActivity();
                    }
                }
            }
        };

        mAdapter = new FeedGridItemAdapter(getActivity()) {

            @Override
            protected GridEntryBase onCreateHeaderViewHolder(ViewGroup parent) {
                View res = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_title_subtitle, parent, false);
                if (res.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                    int gridFeedPadding = parent.getResources().getDimensionPixelSize(R.dimen.grid_feed_item_margin);
                    StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams)res.getLayoutParams();
                    lp.setFullSpan(true);
                    // XXX: жесткий хак, но работает и я не знаю, как сделать лучше
                    // Нужено, чтобы по бокам сетки и между элементами были одинаковые отступы
                    lp.leftMargin = -1 * gridFeedPadding;
                    lp.rightMargin = -1 * gridFeedPadding;
                    res.setLayoutParams(lp);
                }
                return new GridEntryHeader2(parent.getContext(), res);
            }

            @Override
            protected void onBindHeaderViewHolder(GridEntryBase holder) {
                int title;
                String subtitle;

                title = getTitle();
                Stats stats = mListener == null ? null : mListener.getStats();
                if (stats == null) {
                    subtitle = null;
                } else {
                    switch(mFeedType)
                    {
                        case FEED_LIVE:
                            subtitle = getResources().getQuantityString(R.plurals.public_records_last_day, stats.publicEntriesInDayCount, stats.publicEntriesInDayCount);
                            break;
                        case FEED_BEST:
                            subtitle = getResources().getQuantityString(R.plurals.best_records_last_day, stats.bestEntriesInDayCount, stats.bestEntriesInDayCount);
                            break;
                        case FEED_ANONYMOUS:
                            subtitle = getResources().getQuantityString(R.plurals.anonymous_records_last_day, stats.anonymousEntriesInDayCount, stats.anonymousEntriesInDayCount);
                            break;
                        case FEED_NEWS:
                            int count = UiUtils.getEntriesLastDay(mAdapter.getFeed());
                            subtitle = getResources().getQuantityString(R.plurals.news_last_day, count, count);
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }

                ((GridEntryHeader) holder).setTitleSubtitle(title, subtitle);
                holder.bindEntry(null);
            }

            @Override
            protected Observable<Feed> createObservable(Long sinceEntryId) {
                return AndroidObservable.bindFragment(GridFeedFragment.this, createObservabelFeed(sinceEntryId, Constants.GRID_FEED_APPEND_LENGTH));
            }

            @Override
            protected void onRemoteError(Throwable e) {
                if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
            }

            @Override
            public void onHeaderMoved(boolean isVisible, int viewTop) {
                if (mListener != null) mListener.onGridTopViewScroll(GridFeedFragment.this, isVisible, viewTop);
            }

            @Override
            public GridEntryBase onCreateViewHolder(ViewGroup parent, int viewType) {
                GridEntryBase vh = super.onCreateViewHolder(parent, viewType);
                vh.itemView.setOnClickListener(onItemClickListener);
                return vh;
            }
        };

        final int columnCount = getResources().getInteger(R.integer.live_feed_column_count);
        StaggeredGridLayoutManager lm = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);

        lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
        // lm.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(lm);
        mGridView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> entries = mAdapter.getFeed();
            ArrayList<Entry> entriesArrayList = new ArrayList<>(entries);
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, entriesArrayList);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mGridView = null;
        EventBus.getDefault().unregister(this);
        if (mAdapter != null) {
            mAdapter.onDestroy();
            mAdapter = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public boolean isHeaderVisisble() {
        if (mGridView == null) return false;
        View v0 = mGridView.getChildAt(0);
        if (v0 == null) return false;
        return mGridView.getChildViewHolder(v0) instanceof FeedGridItemAdapter.GridEntryHeader2;
    }

    public int getHeaderTop() {
        if (mGridView == null) return 0;
        View v0 = mGridView.getChildAt(0);
        if (v0 == null) return 0;
        return v0.getTop();
    }

    public void onEventMainThread(OnStatsLoaded event) {
        if (mAdapter != null) mAdapter.notifyItemChanged(0);
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
                return tlogService.getEntries(Constants.TLOG_NEWS, sinceEntryId, length);
            default:
                throw new IllegalStateException();
        }
    }

    public void refreshData() {
        mFeedSubscription.unsubscribe();
        mRefreshLayout.setRefreshing(true);
        if (DBG) Log.v(TAG, "setRefreshing true");
        mFeedSubscription = AndroidObservable.bindFragment(this, createObservabelFeed(null, Constants.GRID_FEED_INITIAL_LENGTH))
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
            if (mAdapter != null) mAdapter.insertEntries(feed.entries);
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
        public void onGridTopViewScroll(GridFeedFragment fragment, boolean headerVisible, int headerTop);
        public void startRefreshStats();
        public @Nullable Stats getStats();
    }
}
