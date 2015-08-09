package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class TlogFragment extends Fragment implements IRereshable, ListFeedWorkRetainedFragment.TargetFragmentInteraction {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogFragment";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_SLUG = "user_slug";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "avatar_thumbnail_res";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private TextView mEmptyView;
    private DateIndicatorWidget mDateIndicatorView;

    private Adapter mAdapter;

    private int mAvatarThumbnailRes;

    private WorkRetainedFragment mWorkFragment;

    private Handler mHandler;

    private FeedsHelper.DateIndicatorUpdateHelper mDateIndicatorHelper;

    public static TlogFragment newInstance(long userId) {
        return newInstance(userId, 0);
    }

    public static TlogFragment newInstance(long userId, int avatarThumbnailRes) {
        TlogFragment f = new  TlogFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_USER_ID, userId);
        if (avatarThumbnailRes > 0) b.putInt(ARG_AVATAR_THUMBNAIL_RES, avatarThumbnailRes);
        f.setArguments(b);
        return f;
    }

    public static TlogFragment newInstance(String userSlug) {
        TlogFragment f = new  TlogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_USER_SLUG, userSlug);
        f.setArguments(b);
        return f;
    }

    public TlogFragment() {
        // Required empty public constructor
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAvatarThumbnailRes = getArguments().getInt(ARG_AVATAR_THUMBNAIL_RES, 0);
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_tlog, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);

        mEmptyView = (TextView)v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false);
            }
        });
        mListView.addOnScrollListener(new FeedsHelper.StopGifOnScroll());
        mListView.addOnScrollListener(new FeedsHelper.WatchHeaderScrollListener() {

            @Override
            void onScrolled(RecyclerView recyclerView, int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
                if (mListener == null) return;
                mListener.onListScroll(dy, firstVisibleItem, firstVisibleFract, visibleCount, totalCount);
            }
        });

        LinearLayoutManager lm = new LinearLayoutManagerNonFocusable(getActivity());
        //mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.getItemAnimator().setSupportsChangeAnimations(false);
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), false));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mDateIndicatorView.setAutoShow(false);

        final GestureDetector gd = new GestureDetector(getActivity(), new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) { return false; }

            @Override
            public void onShowPress(MotionEvent e) { }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mListener != null) mListener.onListClicked();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }

            @Override
            public void onLongPress(MotionEvent e) { }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return false; }
        });

        mListView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gd.onTouchEvent(e);
                return false;
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("TlogFragmentWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            mWorkFragment.setArguments(getArguments());
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "TlogFragmentWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }

    @Override
    public void onWorkFragmentActivityCreated() {
        mAdapter = new Adapter(mWorkFragment.getEntryList());
        mAdapter.onCreate();

        mDateIndicatorHelper = new FeedsHelper.DateIndicatorUpdateHelper(mListView, mDateIndicatorView, mAdapter);
        mAdapter.registerAdapterDataObserver(mDateIndicatorHelper.adapterDataObserver);
        mListView.addOnScrollListener(mDateIndicatorHelper.onScrollListener);

        if (mWorkFragment.getUser() != null) {
            mAdapter.setUser(mWorkFragment.getUser().author);
            setupFeedDesign();
            mListener.onTlogInfoLoaded(mWorkFragment.getUser());
        }

        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onWorkFragmentResume() {
        if (!mWorkFragment.isRefreshing()) refreshData(false);
        mDateIndicatorHelper.onResume();
        updateDateIndicatorVisibility();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacksAndMessages(null);
        mDateIndicatorView = null;
        mWorkFragment.setTargetFragment(null, 0);
        if (mDateIndicatorHelper != null) {
            mDateIndicatorHelper.onDestroy();
            mDateIndicatorHelper = null;
        }
        if (mAdapter != null) {
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
    public void onShowPendingIndicatorChanged(boolean newValue) {
        if (mAdapter != null) mAdapter.setLoading(newValue);
    }

    @Override
    public void onLoadingStateChanged(String reason) {
        mHandler.removeCallbacks(mRefreshLoadingState);
        mHandler.postDelayed(mRefreshLoadingState, 16);
    }

    @Override
    public void onDesignChanged() {
    }

    @Override
    public void onCurrentUserChanged() {
        if (mListener != null) mListener.onTlogInfoLoaded(mWorkFragment.getUser());
        setupFeedDesign(); // TODO проверить. Возможно, здесь не нужно.
        if (mAdapter != null) mAdapter.setUser(mWorkFragment.getUser().author);
        setupLoadingState();
    }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }


    public void onOverlayVisibilityChanged(boolean visible) {
        updateDateIndicatorVisibility(visible);
    }

    public void refreshData(boolean forceShowRefreshingIndicator) {
        if (!mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(mWorkFragment.getEntryList().isEmpty() || forceShowRefreshingIndicator);
        }
        mWorkFragment.refreshData();
    }

    void onAvatarClicked(View v) {
        TlogInfo user = mWorkFragment.getUser();
        if (user == null || user.author == null) return;
        new UserInfoActivity.Builder(getActivity())
                .set(mWorkFragment.getUser().author, v, user.design)
                .setPreloadAvatarThumbnail(R.dimen.feed_header_avatar_normal_diameter)
                .startActivity();
    }

    @Nullable
    public Long getUserId() {
        return mWorkFragment.mUserId;
    }

    void setupFeedDesign() {
        TlogInfo user = mWorkFragment.getUser();
        if (user == null) return;
        TlogDesign design = user.design;

        if (DBG) Log.e(TAG, "Setup feed design " + design);
        mAdapter.setFeedDesign(design);
    }

    void updateDateIndicatorVisibility() {
        if (mListener == null) return;
        updateDateIndicatorVisibility(mListener.isOverlayVisible());
    }

    private void updateDateIndicatorVisibility(boolean overlayVisible) {
        boolean dayIndicatorVisible = overlayVisible && !mAdapter.getFeed().isEmpty();
        if (dayIndicatorVisible) {
            mDateIndicatorView.showIndicatorSmoothly();
        } else {
            // Hide
            mDateIndicatorView.hideIndicatorSmoothly();
        }
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

        boolean isTlogForbidden = (mWorkFragment != null) && mWorkFragment.isTlogForbidden();

        mEmptyView.setVisibility(listIsEmpty || isTlogForbidden ? View.VISIBLE : View.GONE);
        if (isTlogForbidden) {
            mEmptyView.setText(R.string.error_tlog_access_denied);
            mEmptyView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_private_post_indicator_activated, 0, 0);
        } else {
            mEmptyView.setText(R.string.user_have_not_written_anything);
            mEmptyView.setCompoundDrawables(null, null, null, null);
        }
        updateDateIndicatorVisibility();
    }

    private void setupAdapterPendingIndicator() {
        if (mAdapter == null) return;
        boolean pendingIndicatorShown = mWorkFragment != null
                && mWorkFragment.isPendingIndicatorShown();

        if (DBG) Log.v(TAG, "setupAdapterPendingIndicator() shown: " + pendingIndicatorShown);

        mAdapter.setLoading(pendingIndicatorShown);
    }

    private final Runnable mRefreshLoadingState = new Runnable() {
        @Override
        public void run() {
            if (mListView == null) return;
            setupLoadingState();
            setupAdapterPendingIndicator();
        }
    };

    public class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private User mUser = User.DUMMY;
        private ImageUtils.DrawableTarget mAvatarThumbnailLoadTarget;
        private ImageUtils.DrawableTarget mAvatarLoadTarget;

        public Adapter(SortedList<Entry> feed) {
            super(feed, false);
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    if (mWorkFragment != null) mWorkFragment.onBindViewHolder(position);
                }

                @Override
                public void initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
                    // Все посты
                    if (pHolder instanceof ListEntryBase) {
                        ((ListEntryBase) pHolder).getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
                        FeedsHelper.setupListEntryClickListener(Adapter.this, (ListEntryBase) pHolder);
                    }
                }

                @Override
                public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                    View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_tlog, mListView, false);
                    child.setBackgroundDrawable(null);
                    HeaderHolder holder = new HeaderHolder(child);
                    holder.avatarView.setOnClickListener(mOnClickListener);
                    return holder;
                }

                @Override
                public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                    HeaderHolder holder = (HeaderHolder) viewHolder;
                    holder.titleView.setText(mTitle);
                    bindUser(holder);
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    addEntry(event.postEntry);
                }
            });
        }

        public void setUser(User user) {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
            setTitleUser(name, user);
        }

        private void setTitleUser(String title, User user) {
            if (!TextUtils.equals(mTitle, title) && !Objects.equals(mUser, user)) {
                mTitle = title;
                mUser = user;
                notifyItemChanged(0);
            }
        }

        private void bindUser(final HeaderHolder holder) {
            if (mAvatarThumbnailRes > 0) {
                mAvatarThumbnailLoadTarget = new ImageUtils.ImageViewTarget(holder.avatarView, false);
                ImageUtils.getInstance().loadAvatar(
                        holder.itemView.getContext(),
                        mUser.getUserpic(),
                        mUser.getName(),
                        mAvatarThumbnailLoadTarget,
                        mAvatarThumbnailRes
                );
            } else {
                mAvatarThumbnailLoadTarget = null;
            }

            mAvatarLoadTarget = new ImageUtils.ImageViewTarget(holder.avatarView, false) {

                final Picasso picasso = Picasso.with(holder.itemView.getContext());

                @Override
                public void onDrawableReady(Drawable drawable) {
                    super.onDrawableReady(drawable);
                    cancelLoadAvatar();
                }

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    super.onBitmapLoaded(bitmap, from);
                    cancelLoadAvatar();
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    super.onBitmapFailed(errorDrawable);
                    cancelLoadAvatar();
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // Не ставим placeholoder, если загружаем аватарку меньшего размера
                    if (mAvatarThumbnailRes <= 0) super.onPrepareLoad(placeHolderDrawable);

                }

                void cancelLoadAvatar() {
                    if (mAvatarThumbnailLoadTarget != null) {
                        picasso.cancelRequest(mAvatarThumbnailLoadTarget);
                        mAvatarThumbnailLoadTarget = null;
                    }
                }
            };

            ImageUtils.getInstance().loadAvatar(
                    holder.itemView.getContext(),
                    mUser.getUserpic(),
                    mUser.getName(),
                    mAvatarLoadTarget,
                    R.dimen.feed_header_avatar_normal_diameter
            );
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
    }

    public static class HeaderHolder extends ParallaxedHeaderHolder {
        TextView titleView;
        ImageView avatarView;

        public HeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.avatar_user_name));
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            titleView = (TextView)itemView.findViewById(R.id.user_name);
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
            TlogDesign design = entry.getDesign();
            if (design == null && mWorkFragment != null && mWorkFragment.getUser() != null)  {
                design = mWorkFragment.getUser().design;
            }
            new ShowPostActivity.Builder(getActivity())
                    .setEntry(entry)
                    .setDesign(design)
                    .setSrcView(view)
                    .startActivity();
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
        }
    };


    public static class WorkRetainedFragment extends Fragment {

        private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.TlogFragment.WorkRetainedFragment.BUNDLE_KEY_FEED_ITEMS";
        private static final String BUNDLE_KEY_TLOG_INFO = "ru.taaasty.ui.feeds.TlogFragment.WorkRetainedFragment.BUNDLE_KEY_TLOG_INFO";

        private FeedLoader mFeedLoader;

        private ApiTlog mTlogService;

        private Long mUserId;

        private String mUserSlug;

        private Subscription mCurrentUserSubscription = Subscriptions.unsubscribed();

        private TlogInfo mUser;

        private OnFragmentInteractionListener mListener;

        private SortedList<Entry> mEntryList;


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
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mTlogService = RestClient.getAPiTlog();
            Bundle args = getArguments();
            if (args.containsKey(ARG_USER_ID)) {
                mUserId = args.getLong(ARG_USER_ID);
                mUserSlug = null;
            } else {
                mUserId = null;
                mUserSlug = args.getString(ARG_USER_SLUG);
            }
            mEntryList = new FeedSortedList(new FeedSortedList.IAdapterProvider() {
                @Nullable
                @Override
                public RecyclerView.Adapter getTargetAdapter() {
                    return getMainFragment() == null ? null : getMainFragment().getAdapter();
                }
            });
            mFeedLoader = new FeedLoader(mEntryList);
            mUser = null;
            if (savedInstanceState != null) {
                ArrayList<Entry> feed = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS + getKeysSuffix());
                if (feed != null) mEntryList.resetItems(feed);

                TlogInfo info = savedInstanceState.getParcelable(BUNDLE_KEY_TLOG_INFO + getKeysSuffix());
                if (info != null) {
                    mUser = info;
                }
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (!mEntryList.isEmpty()) {
                outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS + getKeysSuffix(), new ArrayList<>(mEntryList.getItems()));
            }

            if (mUser != null) {
                outState.putParcelable(BUNDLE_KEY_TLOG_INFO + getKeysSuffix(), mUser);
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentActivityCreated();
        }

        @Override
        public void onResume() {
            super.onResume();
            ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mCurrentUserSubscription.unsubscribe();
            if (mFeedLoader != null) {
                mFeedLoader.onDestroy();
                mFeedLoader = null;
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mListener = null;
        }

        protected String getKeysSuffix() {
            return "TlogFeed";
        }

        public boolean isTlogForbidden() {
            return (mFeedLoader != null)
                    && (mFeedLoader.isLastErrorForbidden());
        }

        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return mTlogService.getEntries(String.valueOf(mUserId), sinceEntryId, limit);
        }

        @Nullable
        public TlogFragment getMainFragment() {
            return (TlogFragment) getTargetFragment();
        }

        public void refreshData() {
            if (DBG) Log.v(TAG, "refreshData()");
            refreshUser();
            if (mUserId != null) refreshFeed();
        }

        public SortedList<Entry> getEntryList() {
            return mEntryList;
        }

        @Nullable
        public TlogInfo getUser() {
            return mUser;
        }

        public boolean isLoading() {
            return !mCurrentUserSubscription.isUnsubscribed() || mFeedLoader.isLoading();
        }

        public boolean isPendingIndicatorShown() {
            return mFeedLoader.isPendingIndicatorShown();
        }

        public boolean isRefreshing() {
            return mFeedLoader.isRefreshing();
        }

        public boolean isFeedEmpty() {
            return !mFeedLoader.isKeepOnAppending() && mEntryList.isEmpty();
        }

        public void onBindViewHolder(int feedLocation) {
            mFeedLoader.onBindViewHolder(feedLocation);
        }

        public void refreshUser() {
            mCurrentUserSubscription.unsubscribe();
            Observable<TlogInfo> observableCurrentUser = mTlogService.getUserInfo(mUserId == null
                    ? mUserSlug : mUserId.toString());

            mCurrentUserSubscription = observableCurrentUser
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            callLoadingStateChanged("refreshUser() doOnUnsubscribe");
                        }
                    })
                    .subscribe(mCurrentUserObserver);
            callLoadingStateChanged("refreshUser start");
        }

        public void refreshFeed() {
            int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
            Observable<Feed> observableFeed = mFeedLoader.createObservable(null, requestEntries);
            mFeedLoader.refreshFeed(observableFeed, requestEntries);
            callLoadingStateChanged("refreshFeed() start");
        }

        void callLoadingStateChanged(String reason) {
            if (DBG) Log.v(TAG, "callLoadingStateChanged: " + reason);
            if (getMainFragment() != null) getMainFragment().onLoadingStateChanged(reason);
        }

        private final Observer<TlogInfo> mCurrentUserObserver = new Observer<TlogInfo>() {

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable exception) {
                if (DBG) Log.e(TAG, "refresh author error", exception);
                if (exception instanceof ApiErrorException
                    && ((ApiErrorException)exception).isError404NotFound()) {
                    if (mListener != null) mListener.onNoSuchUser();
                } else {
                    if (mListener != null) mListener.notifyError(
                            UiUtils.getUserErrorText(getResources(), exception, R.string.error_loading_user),
                            exception);
                }
            }

            @Override
            public void onNext(TlogInfo currentUser) {
                if (Objects.equals(mUser, currentUser)) return;
                mUser = currentUser;
                if (mUserId == null) {
                    mUserId = mUser.author.getId();
                    refreshFeed();
                }
                if (getMainFragment() != null) getMainFragment().onCurrentUserChanged();
            }
        };

        class FeedLoader extends ru.taaasty.ui.feeds.FeedLoader {

            private boolean mLastErrorForbidden = false;

            public FeedLoader(SortedList<Entry> list) {
                super(list);
            }

            public boolean isLastErrorForbidden() {
                return mLastErrorForbidden;
            }

            @Override
            public void refreshFeed(Observable<Feed> observable, int entriesRequested) {
                mLastErrorForbidden = false;
                super.refreshFeed(observable, entriesRequested);
            }

            @Override
            protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
                return WorkRetainedFragment.this.createObservable(sinceEntryId, limit);
            }

            @Override
            protected void onKeepOnAppendingChanged(boolean newValue) {
                if (DBG) Log.v(TAG, "onKeepOnAppendingChanged() keepOn: " + newValue);
            }

            @Override
            protected void onShowPendingIndicatorChanged(boolean newValue) {
                if (DBG) Log.v(TAG, "onShowPendingIndicatorChanged() show: " + newValue);
                if (getMainFragment() != null) getMainFragment().onShowPendingIndicatorChanged(newValue);
            }

            @Override
            protected void onLoadNext(boolean isRefresh, int entriesRequested, Feed feed) {
                mLastErrorForbidden = false;
                super.onLoadNext(isRefresh, entriesRequested, feed);
            }

            @Override
            protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable exception) {
                super.onLoadError(isRefresh, entriesRequested, exception);
                if (exception instanceof ApiErrorException
                        && ((ApiErrorException)exception).isError403Forbidden()) {
                    // тлог закрыт.
                    mLastErrorForbidden = true;
                } else {
                    if (mListener != null)
                        mListener.notifyError(
                                UiUtils.getUserErrorText(getResources(), exception,
                                        R.string.error_append_feed), exception);
                }
            }

            protected void onFeedIsUnsubscribed(boolean isRefresh) {
                if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
                callLoadingStateChanged("onFeedIsUnsubscribed refresh: " + isRefresh);
            }
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
        void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount);
        void onTlogInfoLoaded(TlogInfo info);
        void onSharePostMenuClicked(Entry entry);
        void onListClicked();
        void onNoSuchUser();
        boolean isOverlayVisible();
    }
}
