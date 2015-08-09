package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.Session;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.HeaderTitleSubtitleViewHolder;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.OnCurrentUserChanged;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.Stats;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.CreateAnonymousPostActivity;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.Objects;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.FeedBackgroundDrawable;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;
import rx.functions.Func1;

public class ListFeedFragment extends Fragment implements IRereshable,
        ListFeedWorkRetainedFragment.TargetFragmentInteraction{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListFeedFragment";

    private static final String ARG_FEED_TYPE = "feed_type";

    static final int FEED_LIVE = 0;
    static final int FEED_BEST = 1;
    static final int FEED_NEWS = 2;
    static final int FEED_ANONYMOUS = 3;
    static final int FEED_MY_SUBSCRIPTIONS = 4;

    private int mFeedType;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;
    private DateIndicatorWidget mDateIndicatorView;

    private Adapter mAdapter;
    private WorkRetainedFragment mWorkFragment;

    private boolean mPendingForceShowRefreshingIndicator;
    private Integer mLastPublicEntriesCount;
    private Integer mlastBestEntriesCount;
    private Integer mlastAnonymousEntriesCount;

    private Handler mHandler;

    private FeedsHelper.DateIndicatorUpdateHelper mDateIndicatorHelper;

    private TabbarFragment.AutoHideScrollListener mHideTabbarListener;

    public static ListFeedFragment createLiveFeedInstance() {
        return newInstance(FEED_LIVE);
    }

    public static ListFeedFragment createBestFeedInstance() {
        return newInstance(FEED_BEST);
    }

    public static ListFeedFragment createAnonymousFeedInstance() {
        return newInstance(FEED_ANONYMOUS);
    }

    public static ListFeedFragment createNewsFeedInstance() {
        return newInstance(FEED_NEWS);
    }

    public static ListFeedFragment createMySubscriptionsFeedInstance() {
        return newInstance(FEED_MY_SUBSCRIPTIONS);
    }

    private static ListFeedFragment newInstance(int type) {
        ListFeedFragment fragment = new ListFeedFragment();
        Bundle args = new Bundle(1);
        args.putInt(ARG_FEED_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    public ListFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFeedType = getArguments().getInt(ARG_FEED_TYPE, FEED_LIVE);
        mPendingForceShowRefreshingIndicator = false;
        mHandler = new Handler();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_list_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false);
            }
        });

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        //mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.getItemAnimator().setSupportsChangeAnimations(false);
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), isUserAvatarVisibleOnPost()));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mListView.addOnScrollListener(new FeedsHelper.StopGifOnScroll());

        mHideTabbarListener = new TabbarFragment.AutoHideScrollListener(mListener.getTabbar());
        mListView.addOnScrollListener(mHideTabbarListener);

        Resources resource = getResources();
        FeedBackgroundDrawable background = new FeedBackgroundDrawable(resource, null,
                resource.getDimensionPixelSize(R.dimen.header_title_subtitle_height));
        TlogDesign lightDesign = TlogDesign.createLightTheme(TlogDesign.DUMMY);
        background.setFeedDesign(resource, lightDesign);
        background.setFeedMargin(resource.getDimensionPixelSize(R.dimen.feed_horizontal_margin), 0,
                resource.getDimensionPixelSize(R.dimen.feed_horizontal_margin), 0);
        mListView.setBackgroundDrawable(background);

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("ListFeedWorkFragment-" + mFeedType);
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            Bundle args = new Bundle(1);
            args.putInt(ARG_FEED_TYPE, mFeedType);
            mWorkFragment.setArguments(args);
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "ListFeedWorkFragment-" + mFeedType).commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }

    @Override
    public void onWorkFragmentActivityCreated() {
        mAdapter = new Adapter(mWorkFragment.getEntryList(), isUserAvatarVisibleOnPost());
        mAdapter.onCreate();
        mListView.setAdapter(mAdapter);

        mDateIndicatorHelper = new FeedsHelper.DateIndicatorUpdateHelper(mListView, mDateIndicatorView,mAdapter);
        mAdapter.registerAdapterDataObserver(mDateIndicatorHelper.adapterDataObserver);
        mListView.addOnScrollListener(mDateIndicatorHelper.onScrollListener);

        setupFeedDesign();
        setupAdapterPendingIndicator();
        onLoadingStateChanged("onWorkFragmentActivityCreated()");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onWorkFragmentResume() {
        mDateIndicatorHelper.onResume();
        if (!mWorkFragment.isRefreshing()) refreshData(mPendingForceShowRefreshingIndicator);
        mPendingForceShowRefreshingIndicator = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        mWorkFragment.setTargetFragment(null, 0);
        mDateIndicatorView = null;
        mEmptyView = null;
        mRefreshLayout = null;
        if (mDateIndicatorHelper != null) {
            mDateIndicatorHelper.onDestroy();
            mDateIndicatorHelper = null;
        }
        if (mAdapter != null) {
            mAdapter.onDestroy(mListView);
            mAdapter = null;
        }
        mHideTabbarListener = null;
        mListView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        if (mAdapter == null) return;
        switch (mFeedType) {
            case FEED_LIVE:
                if (!Objects.equals(mLastPublicEntriesCount, event.stats.getPublicEntriesInDayCount())) {
                    mLastPublicEntriesCount = event.stats.getPublicEntriesInDayCount();
                    mAdapter.notifyItemChanged(0);
                }
                break;
            case FEED_BEST:
                if (!Objects.equals(mlastBestEntriesCount, event.stats.getBestEntriesInDayCount())) {
                    mlastBestEntriesCount = event.stats.getBestEntriesInDayCount();
                    mAdapter.notifyItemChanged(0);
                }
                break;
            case FEED_ANONYMOUS:
                if (!Objects.equals(mlastAnonymousEntriesCount, event.stats.getAnonymousEntriesInDayCount())) {
                    mlastAnonymousEntriesCount = event.stats.getAnonymousEntriesInDayCount();
                    mAdapter.notifyItemChanged(0);
                }
                break;
            case FEED_MY_SUBSCRIPTIONS:
            case FEED_NEWS:
                break;
            default:
                throw new IllegalStateException();
        }
    }

    public void onEventMainThread(OnCurrentUserChanged event) {
        setupFeedDesign();
    }

    boolean isUserAvatarVisibleOnPost() {
        return mFeedType != FEED_ANONYMOUS;
    }

    public void refreshData(boolean forceShowRefreshingIndicator) {
        if (DBG) Log.v(TAG, "refreshData()");
        if (!mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(mWorkFragment.getEntryList().isEmpty() || forceShowRefreshingIndicator);
        }
        mWorkFragment.refreshData();
    }

    @Nullable
    private TlogDesign getDesign() {
        CurrentUser user = Session.getInstance().getCachedCurrentUser();
        if (user == null) return null;
        return user.getDesign();
    }

    void setupFeedDesign() {
        TlogDesign design = getDesign();
        if (mWorkFragment == null || design == null) return;
        if (DBG) Log.e(TAG, "Setup feed design " + design);
        design = TlogDesign.createLightTheme(design); // Подписки всегда светлые
        mAdapter.setFeedDesign(design);
    }

    public void onHeaderMoved(boolean isVisible, int viewTop) {
        if (mListener != null) mListener.onGridTopViewScroll(this, isVisible, viewTop);
    }

    @Override
    public void onShowPendingIndicatorChanged(boolean newValue) {
        if (mAdapter != null) mAdapter.setLoading(newValue);
    }

    @Override
    public void onDesignChanged() { throw new IllegalStateException(); }

    @Override
    public void onCurrentUserChanged() { throw new IllegalStateException(); }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    public void onLoadingStateChanged(String reason) {
        mHandler.removeCallbacks(mRefreshLoadingState);
        mHandler.postDelayed(mRefreshLoadingState, 16);
    }

    private final Runnable mRefreshLoadingState = new Runnable() {
        @Override
        public void run() {
            if (mListView == null) return;
            setupLoadingState();
            setupAdapterPendingIndicator();
        }
    };

    private void setupAdapterPendingIndicator() {
        if (mAdapter == null) return;
        boolean pendingIndicatorShown = mWorkFragment != null
                && mWorkFragment.isPendingIndicatorShown();

        if (DBG) Log.v(TAG, "setupAdapterPendingIndicator() shown: " + pendingIndicatorShown);

        mAdapter.setLoading(pendingIndicatorShown);
    }

    public void setupLoadingState() {
        if (mRefreshLayout == null) return;

        if (DBG) Log.v(TAG, "setupLoadingState() work fragment != null: "
                        + (mWorkFragment != null)
                        + " isRefreshing: " + (mWorkFragment != null && mWorkFragment.isRefreshing())
                        + " isLoading: " + (mWorkFragment != null && mWorkFragment.isLoading())
                        + " feed is empty: " + (mWorkFragment != null && mWorkFragment.isFeedEmpty())
                        + " adapter != null: " + (mAdapter != null)
        );

        // Здесь индикатор не ставим, только снимаем. Устанавливает индикатор либо сам виджет
        // при свайпе вверх, либо если адаптер пустой. В другом месте.
        boolean isRefreshing = mWorkFragment == null || mWorkFragment.isRefreshing();
        if (!isRefreshing) mRefreshLayout.setRefreshing(false);

        boolean listIsEmpty = mAdapter != null
                && mWorkFragment != null
                && mWorkFragment.isFeedEmpty();

        mEmptyView.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        if (mWorkFragment.getEntryList().isEmpty()) mDateIndicatorView.setVisibility(View.INVISIBLE);
    }

    class Adapter extends FeedItemAdapterLite {

        public Adapter(SortedList<Entry> list, boolean showUserAvatar) {
            super(list, showUserAvatar);
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    if (mWorkFragment != null) {
                        mWorkFragment.onBindViewHolder(viewHolder, position);
                    }
                }

                @Override
                public void initClickListeners(RecyclerView.ViewHolder holder, int type) {
                    // Все посты
                    if (holder instanceof ListEntryBase) {
                        setPostClickListener((ListEntryBase) holder);
                    }
                }

                @Override
                public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                    View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_title_subtitle, parent, false);
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                    params.bottomMargin = 0;
                    child.setLayoutParams(params);

                    if (mFeedType == FEED_ANONYMOUS) {
                        View anonymousButton = child.findViewById(R.id.create_anonymous_post);
                        anonymousButton.setVisibility(View.VISIBLE);
                        anonymousButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CreateAnonymousPostActivity.startActivity(v.getContext(), v);
                                ((TaaastyApplication) getActivity().getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FEEDS,
                                        "Открыто создание анонимки", null);
                            }
                        });
                    }

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
                public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                    int title;
                    String subtitle = null;

                    title = getTitle();
                    Stats stats = mListener == null ? null : mListener.getStats();
                    if (stats != null) {
                        switch (mFeedType) {
                            case FEED_LIVE:
                                if (stats.getPublicEntriesInDayCount() != null) {
                                    mLastPublicEntriesCount = stats.getPublicEntriesInDayCount();
                                    subtitle = getResources().getQuantityString(R.plurals.public_records_last_day, mLastPublicEntriesCount, mLastPublicEntriesCount);
                                }
                                break;
                            case FEED_BEST:
                                if (stats.getBestEntriesInDayCount() != null) {
                                    mlastBestEntriesCount = stats.getBestEntriesInDayCount();
                                    subtitle = getResources().getQuantityString(R.plurals.best_records_last_day, mlastBestEntriesCount, mlastBestEntriesCount);
                                }
                                break;
                            case FEED_ANONYMOUS:
                                if (stats.getAnonymousEntriesInDayCount() != null) {
                                    mlastAnonymousEntriesCount = stats.getAnonymousEntriesInDayCount();
                                    subtitle = getResources().getQuantityString(R.plurals.anonymous_records_last_day, mlastAnonymousEntriesCount, mlastAnonymousEntriesCount);
                                }
                                break;
                            case FEED_MY_SUBSCRIPTIONS:
                            case FEED_NEWS:
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                    }

                    ((HeaderTitleSubtitleViewHolder) viewHolder).setTitleSubtitle(title, subtitle);
                    ((HeaderTitleSubtitleViewHolder) viewHolder).bindDesign(mFeedDesign);
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    // TODO это должно быть в work fragment'е
                    if (hasEntry(event.postEntry.getId())) {
                        // Запись уже есть в списке. Обновляем её
                        addEntry(event.postEntry);
                    } else {
                        // Новая запись. Не добавляем её в список, т.к. непонятно, должна быть она в этом фиде или нет.
                        // Просто обновляемся с индикатором
                        if (isResumed()) {
                            refreshData(true);
                        } else {
                            mPendingForceShowRefreshingIndicator = true;
                        }
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
                        case FEED_MY_SUBSCRIPTIONS:
                            return R.string.title_my_subscriptions;
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
                                if (mListener != null && entry != null)
                                    mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
                            }
                        });
                    }

                    // Клики на картинках
                    FeedsHelper.setupListEntryClickListener(Adapter.this, pHolder);
                }
            });
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
    }

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            LikesHelper.getInstance().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            TlogDesign design;
            if (entry.getDesign() != null) {
                design = entry.getDesign();
            } else {
                design = getDesign();
                if (mFeedType == FEED_ANONYMOUS) {
                    // Анонимки обычно светлые
                    design = TlogDesign.createLightTheme(design != null ? design : TlogDesign.DUMMY);
                }
            }
            new ShowPostActivity.Builder(getActivity())
                    .setEntry(entry)
                    .setSrcView(view)
                    .setDesign(design)
                    .startActivity();
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
        }
    };


    public static class WorkRetainedFragment extends ListFeedWorkRetainedFragment {

        private int mFeedType = FEED_LIVE;

        private FeedsHelper.PreMeasureFeedFunc mPreMeasureFunc;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mFeedType = getArguments().getInt(ARG_FEED_TYPE);
            mPreMeasureFunc = new FeedsHelper.PreMeasureFeedFunc(getActivity());
        }

        @Override
        protected String getKeysSuffix() {
            return "ListFeedFragment-" + String.valueOf(mFeedType);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            switch (mFeedType) {
                case FEED_LIVE:
                    return RestClient.getAPiFeeds().getLiveFeed(sinceEntryId, limit);
                case FEED_BEST:
                    return RestClient.getAPiFeeds().getBestFeed(sinceEntryId, limit, null, null, null, null);
                case FEED_ANONYMOUS:
                    return RestClient.getAPiFeeds().getAnonymousFeed(sinceEntryId, limit);
                case FEED_NEWS:
                    return  RestClient.getAPiTlog().getEntries(Constants.TLOG_NEWS, sinceEntryId, limit);
                case FEED_MY_SUBSCRIPTIONS:
                    return RestClient.getAPiMyFeeds().getMyFriendsFeed(sinceEntryId, limit);
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        protected boolean isUserRefreshEnabled() {
            return false;
        }

        @Override
        public TlogDesign getTlogDesign() {
            throw new IllegalStateException();
        }

        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            this.onBindViewHolder(position);
            if (holder instanceof ListTextEntry) {
                ListTextEntry te = (ListTextEntry)holder;
                mPreMeasureFunc.setPaints(te.mTitle.getPaint(), te.mText.getPaint());
            }
        }

        @Override
        protected Func1<Feed, Feed> getPostCacheFunc() {
            return mPreMeasureFunc;
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
        /**
         * Юзер ткнул на аватарку в заголовке записи списка
         * @param view
         * @param user
         * @param design
         */
        void onAvatarClicked(View view, User user, TlogDesign design);

        void onSharePostMenuClicked(Entry entry);

        void startRefreshStats();

        void startRefreshCurrentUser();

        public @Nullable
        Stats getStats();

        void onGridTopViewScroll(Fragment fragment, boolean headerVisible, int headerTop);

        TabbarFragment getTabbar();

    }
}
