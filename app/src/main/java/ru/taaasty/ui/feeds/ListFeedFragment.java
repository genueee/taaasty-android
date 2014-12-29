package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.HeaderTitleSubtitleViewHolder;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.Stats;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiFeeds;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class ListFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListFeedFragment";

    private static final String BUNDLE_KEY_FEED_ITEMS = "feed_items";
    private static final String BUNDLE_KEY_FEED_DESIGN = "feed_design";

    private static final String ARG_FEED_TYPE = "feed_type";

    private static final int FEED_LIVE = 0;
    private static final int FEED_BEST = 1;
    private static final int FEED_NEWS = 2;
    private static final int FEED_ANONYMOUS = 3;

    private int mFeedType = FEED_LIVE;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;
    private DateIndicatorWidget mDateIndicatorView;

    private Adapter mAdapter;
    private FeedLoader mFeedLoader;

    private Subscription mCurrentUserSubscribtion = SubscriptionHelper.empty();

    private int mRefreshCounter;

    private TlogDesign mTlogDesign;

    public static ListFeedFragment createLiveFeedInstance() {
        ListFeedFragment fragment = new ListFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_LIVE);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFeedFragment createBestFeedInstance() {
        ListFeedFragment fragment = new ListFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_BEST);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFeedFragment createAnonymousFeedInstance() {
        ListFeedFragment fragment = new ListFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_ANONYMOUS);
        fragment.setArguments(args);
        return fragment;
    }

    public static ListFeedFragment createNewsFeedInstance() {
        ListFeedFragment fragment = new ListFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, FEED_NEWS);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static SubscriptionsFeedFragment newInstance() {
        return new SubscriptionsFeedFragment();
    }

    public ListFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedType = getArguments().getInt(ARG_FEED_TYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_list_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), isUserAvatarVisibleOnPost()));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicator(dy > 0);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int childCount = mListView.getChildCount();
                    for (int i = 0; i < childCount; ++i) {
                        RecyclerView.ViewHolder vh = mListView.getChildViewHolder(mListView.getChildAt(i));
                        if (vh instanceof ListImageEntry) ((ListImageEntry) vh).onStopScroll();
                    }
                } else {
                    int childCount = mListView.getChildCount();
                    for (int i = 0; i < childCount; ++i) {
                        RecyclerView.ViewHolder vh = mListView.getChildViewHolder(mListView.getChildAt(i));
                        if (vh instanceof ListImageEntry) ((ListImageEntry) vh).onStartScroll();
                    }
                }
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

        List<Entry> feed = null;
        if (savedInstanceState != null) feed = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
        mAdapter = new Adapter(getActivity(), feed, isUserAvatarVisibleOnPost());
        mAdapter.onCreate();
        mAdapter.registerAdapterDataObserver(mUpdateIndicatorObserver);
        mListView.setAdapter(mAdapter);
        mFeedLoader = new FeedLoader(mAdapter);

        if (savedInstanceState != null) {
            TlogDesign design = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_DESIGN);
            if (design != null) {
                mTlogDesign = design;
                setupFeedDesign();
            }
        }

        if (!mRefreshLayout.isRefreshing()) refreshData();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> feed = mAdapter.getFeed().getItems();
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, new ArrayList<>(feed));
        }
        if (mTlogDesign != null) {
            outState.putParcelable(BUNDLE_KEY_FEED_DESIGN, mTlogDesign);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDateIndicator(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        mCurrentUserSubscribtion.unsubscribe();
        mDateIndicatorView = null;
        mEmptyView = null;
        mRefreshLayout = null;
        if (mFeedLoader != null) {
            mFeedLoader.onDestroy();
            mFeedLoader = null;
        }
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mUpdateIndicatorObserver);
            mAdapter.onDestroy(mListView);
            mAdapter = null;
        }
        mListView = null;
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

    public boolean isHeaderVisisble() {
        if (mListView == null) return false;
        View v0 = mListView.getChildAt(0);
        if (v0 == null) return false;
        return mListView.getChildViewHolder(v0) instanceof HeaderTitleSubtitleViewHolder;
    }

    public int getHeaderTop() {
        if (mListView == null) return 0;
        View v0 = mListView.getChildAt(0);
        if (v0 == null) return 0;
        return v0.getTop();
    }

    public void onEventMainThread(OnStatsLoaded event) {
        if (mAdapter != null) mAdapter.notifyItemChanged(0);
    }

    boolean isUserAvatarVisibleOnPost() {
        return mFeedType != FEED_ANONYMOUS;
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
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    public void onHeaderMoved(boolean isVisible, int viewTop) {
        if (mListener != null) mListener.onGridTopViewScroll(this, isVisible, viewTop);
    }

    class Adapter extends FeedItemAdapterLite {

        public Adapter(Context context, List<Entry> feed, boolean showUserAvatar) {
            super(context, feed, showUserAvatar);
        }

        @Override
        protected boolean initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
            // Все посты
            if (pHolder instanceof ListEntryBase) {
                setPostClickListener((ListEntryBase)pHolder);
                return true;
            }
            return false;
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_title_subtitle, parent, false);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams)child.getLayoutParams();
            params.bottomMargin = 0;
            child.setLayoutParams(params);
            return new HeaderTitleSubtitleViewHolder(child) {
                @Override
                public void onScrollChanged() {
                    if (ListFeedFragment.this.getUserVisibleHint()) {
                        super.onScrollChanged();
                        onHeaderMoved(true, itemView.getTop());
                    }
                }
            };
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
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
                        subtitle = null;
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }

            ((HeaderTitleSubtitleViewHolder) viewHolder).setTitleSubtitle(title, subtitle);
            ((HeaderTitleSubtitleViewHolder) viewHolder).bindDesign(mFeedDesign);
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof IParallaxedHeaderHolder) {
                if (DBG) Log.v(TAG, "header attached to window ittemView: " + holder.itemView);
                onHeaderMoved(true, holder.itemView.getTop());
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            if (holder instanceof IParallaxedHeaderHolder) {
                if (DBG) Log.v(TAG, "header detached from window itemView: " + holder.itemView);
                onHeaderMoved(false, 0);
            }
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

        private void setPostClickListener(final ListEntryBase pHolder) {
            // Клики по элементам панельки снизу
            pHolder.getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);

            // Клик по аватарке в заголовке
            if (mShowUserAvatar) {
                pHolder.getAvatarAuthorView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Entry entry = getAnyEntryAtHolderPosition(pHolder);
                        if (mListener != null && entry != null) mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
                    }
                });
            }

            // Клики на картинках
            FeedsHelper.setupListEntryClickListener(this, pHolder);
        }
    }

    class FeedLoader extends ru.taaasty.ui.feeds.FeedLoaderLite {

        private final ApiFeeds mApiFeedsService;
        private final ApiTlog mApiTlogService;

        public FeedLoader(FeedItemAdapterLite adapter) {
            super(adapter);
            mApiFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiFeeds.class);
            mApiTlogService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            final Observable<Feed> observable;
            switch (mFeedType) {
                case FEED_LIVE:
                    return mApiFeedsService.getLiveFeed(sinceEntryId, limit);
                case FEED_BEST:
                    return mApiFeedsService.getBestFeed(sinceEntryId, limit, null, null,null, null);
                case FEED_ANONYMOUS:
                    return mApiFeedsService.getAnonymousFeed(sinceEntryId, limit);
                case FEED_NEWS:
                    return mApiTlogService.getEntries(Constants.TLOG_NEWS, sinceEntryId, limit);
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        public void onLoadCompleted(boolean isRefresh, int entriesRequested) {
            if (DBG) Log.v(TAG, "onCompleted()");
            if (isRefresh) {
                mEmptyView.setVisibility(mAdapter.getFeed().isEmpty() ? View.VISIBLE : View.GONE);
                if (mAdapter.getFeed().isEmpty()) mDateIndicatorView.setVisibility(View.INVISIBLE);
            }
        }

        @Override
        protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
            super.onLoadError(isRefresh, entriesRequested, e);
            if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
        }

        protected void onFeedIsUnsubscribed(boolean isRefresh) {
            if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
            if (isRefresh) {
                mStopRefreshingAction.call();
            }
        }
    }

    final RecyclerView.AdapterDataObserver mUpdateIndicatorObserver = new RecyclerView.AdapterDataObserver() {
        private Runnable mUpdateIndicatorRunnable = new Runnable() {
            @Override
            public void run() {
                updateDateIndicator(true);
            }
        };

        @Override
        public void onChanged() {
            if (DBG) Log.v(TAG, "onChanged");
            updateIndicatorDelayed();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (DBG) Log.v(TAG, "onItemRangeInserted");
            updateIndicatorDelayed();
        }

        private void updateIndicatorDelayed() {
            if (mListView != null) {
                mListView.removeCallbacks(mUpdateIndicatorRunnable);
                mListView.postDelayed(mUpdateIndicatorRunnable, 64);
            }
        }
    };

    public class LikesHelper extends ru.taaasty.utils.LikesHelper {

        public LikesHelper() {
            super(ListFeedFragment.this);
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

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            TlogActivity.startTlogActivity(getActivity(), entry.getAuthor().getId(), view);
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            new LikesHelper().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            new ShowPostActivity.Builder(getActivity())
                    .setEntry(entry)
                    .setSrcView(view)
                    .setDesign(mTlogDesign)
                    .startActivity();
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
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
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<Feed> observableFeed = mFeedLoader.createObservable(null, requestEntries)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction);
        mFeedLoader.refreshFeed(observableFeed, requestEntries);
        setRefreshing(true);
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            if (DBG) Log.v(TAG, "doOnTerminate()");
            setRefreshing(false);
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
            mTlogDesign = new TlogDesign(currentUser.getDesign());
            mTlogDesign.setIsLightTheme(true); // Прямой эфир всегда светлый
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
        /**
         * Юзер ткнул на аватарку в заголовке записи списка
         * @param view
         * @param user
         * @param design
         */
        public void onAvatarClicked(View view, User user, TlogDesign design);

        public void onSharePostMenuClicked(Entry entry);

        public void startRefreshStats();

        public @Nullable
        Stats getStats();

        public void onGridTopViewScroll(Fragment fragment, boolean headerVisible, int headerTop);

    }
}
