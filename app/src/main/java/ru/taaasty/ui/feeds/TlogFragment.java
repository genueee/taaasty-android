package ru.taaasty.ui.feeds;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.TlogInfo;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiTlog;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
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
    private static final String ARG_USER_SLUG = "user_slug";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "avatar_thumbnail_res";

    private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.TlogFragment.feed_items";
    private static final String BUNDLE_KEY_TLOG_INFO = "ru.taaasty.ui.feeds.TlogFragment.tlog_info";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;
    private DateIndicatorWidget mDateIndicatorView;

    private ApiTlog mTlogService;
    private Adapter mAdapter;
    private MyFeedLoader mFeedLoader;

    private Subscription mUserSubscribtion = SubscriptionHelper.empty();

    @Nullable
    private Long mUserId;

    @Nullable
    private String mUserSlug;

    @Nullable
    private TlogInfo mTlogInfo;

    private String mBackgroundBitmapKey;

    private boolean mForceShowRefreshingIndicator;

    private int mAvatarThumbnailRes;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args.containsKey(ARG_USER_ID)) {
            mUserId = args.getLong(ARG_USER_ID);
            mUserSlug = null;
        } else {
            mUserId = null;
            mUserSlug = args.getString(ARG_USER_SLUG);
        }
        mAvatarThumbnailRes = args.getInt(ARG_AVATAR_THUMBNAIL_RES, 0);

        mTlogService = NetworkUtils.getInstance().createRestAdapter().create(ApiTlog.class);
        if (savedInstanceState != null) {
            mTlogInfo = savedInstanceState.getParcelable(BUNDLE_KEY_TLOG_INFO);
        }
        mForceShowRefreshingIndicator = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_tlog, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);

        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener () {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                View child = recyclerView.getChildAt(0);
                float firstVisibleFract;
                updateDateIndicator(dy > 0);
                if (mListener == null) return;
                if (child == null) {
                    mListener.onListScroll(dy, 0, 0, 0, 0);
                } else {
                    firstVisibleFract = -1f * (float) child.getTop() / (float) child.getHeight();
                    int visibleItemCount = recyclerView.getChildCount();
                    int totalItemCount = mAdapter.getItemCount();
                    mListener.onListScroll(dy, recyclerView.getChildPosition(child),
                            UiUtils.clamp(firstVisibleFract, 0f, 0.99f), visibleItemCount, totalItemCount);

                }
            }
        });

        LinearLayoutManager lm = new LinearLayoutManagerNonFocusable(getActivity());
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), false));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mDateIndicatorView.setAuthoShow(false);

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

        mListView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gd.onTouchEvent(e);
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

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
        mAdapter = new Adapter(getActivity(), feed);
        mAdapter.onCreate();
        mAdapter.registerAdapterDataObserver(mUpdateIndicatorObserver);
        if (mTlogInfo != null) {
            mAdapter.setUser(mTlogInfo.author);
            setupFeedDesign();
        }
        if (savedInstanceState != null) mDateIndicatorView.setVisibility(mAdapter.getFeed().isEmpty() ? View.INVISIBLE : View.VISIBLE);

        mListView.setAdapter(mAdapter);
        mFeedLoader = new MyFeedLoader(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> feed = mAdapter.getFeed().getItems();
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, new ArrayList<>(feed));
        }
        outState.putParcelable(BUNDLE_KEY_TLOG_INFO, mTlogInfo);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isLoading()) refreshData(mAdapter.getFeed().isEmpty());
        updateDateIndicator(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUserSubscribtion.unsubscribe();

        mDateIndicatorView = null;
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
        refreshData(true);
    }

    public void onOverlayVisibilityChanged(boolean visible) {
        updateDateIndicatorVisibility(visible);
    }

    boolean isLoading() {
        return mFeedLoader.isRefreshing() || !mUserSubscribtion.isUnsubscribed();
    }

    void setupRefreshingIndicator() {
        if (mAdapter == null) return;
        boolean showIndicator = mAdapter.getFeed().isEmpty() || mForceShowRefreshingIndicator;
        mRefreshLayout.setRefreshing(showIndicator && isLoading());

        if (!isLoading()) mForceShowRefreshingIndicator = false;
    }

    public void refreshData(boolean showIndicator) {
        if (DBG) Log.v(TAG, "refreshData()");
        if (showIndicator) mForceShowRefreshingIndicator = true;
        refreshUser();
        if (mUserId != null) refreshFeed();
    }

    void onAvatarClicked(View v) {
        if (mTlogInfo == null || mTlogInfo.author == null) return;
        new UserInfoActivity.Builder(getActivity())
                .set(mTlogInfo.author, v, mTlogInfo.design)
                .setPreloadAvatarThumbnail(R.dimen.avatar_normal_diameter)
                .setBackgroundThumbnailKey(mBackgroundBitmapKey)
                .startActivity();
    }

    @Nullable
    public Long getUserId() {
        return mUserId;
    }

    void setupFeedDesign() {
        if (mTlogInfo == null) return;
        TlogDesign design = mTlogInfo.design;

        if (DBG) Log.e(TAG, "Setup feed design " + design);
        mAdapter.setFeedDesign(design);
        if (mListener != null) mListener.setFeedBackgroundColor(design.getFeedBackgroundColor(getResources()));

    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
        updateDateIndicatorVisibility();
    }

    void updateDateIndicatorVisibility() {
        updateDateIndicatorVisibility(mListener.isOverlayVisible());
    }

    private void updateDateIndicatorVisibility(boolean overlayVisible) {
        boolean dayIndicatorVisible = overlayVisible && !mAdapter.getFeed().isEmpty();
        if (dayIndicatorVisible  && mDateIndicatorView.getVisibility() != View.VISIBLE) {
            // Show
            ObjectAnimator animator = ObjectAnimator.ofFloat(mDateIndicatorView, "alpha", 0f, 1f)
                    .setDuration(getResources().getInteger(R.integer.longAnimTime));
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (mDateIndicatorView != null) mDateIndicatorView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mDateIndicatorView != null) mDateIndicatorView.setAlpha(1f);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (mDateIndicatorView != null) mDateIndicatorView.setAlpha(1f);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            animator.start();
        } else if (!dayIndicatorVisible && mDateIndicatorView.getVisibility() == View.VISIBLE) {
            // Hide
            ObjectAnimator animator = ObjectAnimator.ofFloat(mDateIndicatorView, "alpha", 1f, 0f)
                    .setDuration(getResources().getInteger(R.integer.longAnimTime));
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mDateIndicatorView == null) return;
                    mDateIndicatorView.setAlpha(1f);
                    mDateIndicatorView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    if (mDateIndicatorView == null) return;
                    mDateIndicatorView.setAlpha(1f);
                    mDateIndicatorView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {}
            });
            animator.start();
        }
    }

    public class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private User mUser = User.DUMMY;
        private ImageUtils.DrawableTarget mAvatarThumbnailLoadTarget;
        private ImageUtils.DrawableTarget mAvatarLoadTarget;

        public Adapter(Context context, List<Entry> feed) {
            super(context, feed, false);
        }

        @Override
        protected boolean initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
            // Все посты
            if (pHolder instanceof ListEntryBase) {
                ((ListEntryBase)pHolder).getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
                FeedsHelper.setupListEntryClickListener(this, (ListEntryBase)pHolder);
                return true;
            }

            return false;
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_tlog, mListView, false);
            HeaderHolder holder = new HeaderHolder(child);
            holder.avatarView.setOnClickListener(mOnClickListener);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
            HeaderHolder holder = (HeaderHolder)viewHolder;
            holder.titleView.setText(mTitle);
            bindDesign(holder);
            bindUser(holder);
        }

        public void setUser(User user) {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0, 1).toUpperCase(Locale.getDefault()) + name.substring(1);
            setTitleUser(name, user);
        }

        private void setTitleUser(String title, User user) {
            mTitle = title;
            mUser = user;
            notifyItemChanged(0);
        }

        private void bindDesign(HeaderHolder holder) {
            if (mTlogInfo == null) return;
            TlogDesign design = mTlogInfo.design;
            String backgroudUrl = design.getBackgroundUrl();
            if (TextUtils.equals(holder.backgroundUrl, backgroudUrl)) return;
            holder.feedDesignTarget = new TargetSetHeaderBackground(holder.itemView,
                    design, Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS) {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    super.onBitmapLoaded(bitmap, from);
                    mBackgroundBitmapKey = "TlogFragment-header";
                    ImageUtils.getInstance().putBitmapToCache(mBackgroundBitmapKey, bitmap);
                }
            };
            holder.backgroundUrl = backgroudUrl;
            RequestCreator rq = Picasso.with(holder.itemView.getContext())
                    .load(backgroudUrl);
            if (holder.itemView.getWidth() > 1 && holder.itemView.getHeight() > 1) {
                rq.resize(holder.itemView.getWidth() / 2, holder.itemView.getHeight() / 2)
                        .centerCrop();
            }
            rq.into(holder.feedDesignTarget);
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
                    R.dimen.avatar_normal_diameter
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

        public String backgroundUrl = null;

        // XXX: anti picasso weak ref
        private TargetSetHeaderBackground feedDesignTarget;

        public HeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.avatar_user_name));
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            titleView = (TextView)itemView.findViewById(R.id.user_name);
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
            updateIndicatorDelayed();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            updateIndicatorDelayed();
        }

        private void updateIndicatorDelayed() {
            if (mListView != null) {
                mListView.removeCallbacks(mUpdateIndicatorRunnable);
                mListView.postDelayed(mUpdateIndicatorRunnable, 64);
            }
        }
    };

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            throw new IllegalStateException();
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            LikesHelper.getInstance().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            TlogDesign design = entry.getDesign();
            if (design == null && mTlogInfo != null) design = mTlogInfo.design;
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

    public void refreshUser() {
        if (!mUserSubscribtion.isUnsubscribed()) {
            mUserSubscribtion.unsubscribe();
            mStopRefreshingAction.call();
        }
        Observable<TlogInfo> observableCurrentUser = AndroidObservable.bindFragment(this,
                mTlogService.getUserInfo(mUserId == null ? mUserSlug : mUserId.toString()));

        mUserSubscribtion = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
        setupRefreshingIndicator();
    }

    private void refreshFeed() {
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<Feed> observableFeed = mFeedLoader.createObservable(null, requestEntries)
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction);
        mFeedLoader.refreshFeed(observableFeed, requestEntries);
        setupRefreshingIndicator();
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            if (DBG) Log.v(TAG, "doOnTerminate()");
            setupRefreshingIndicator();
        }
    };

    class MyFeedLoader extends ru.taaasty.ui.feeds.FeedLoaderLite {

        public MyFeedLoader(FeedItemAdapterLite adapter) {
            super(adapter);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return AndroidObservable.bindFragment(TlogFragment.this,
                    mTlogService.getEntries(String.valueOf(mUserId), sinceEntryId, limit));
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

    private final Observer<TlogInfo> mCurrentUserObserver = new Observer<TlogInfo>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            // XXX
            if (e instanceof NoSuchElementException) { //TODO Исправить, когда сервер будет отдавать нормальную ошибку
                if (mListener != null) mListener.onNoSuchUser();
            }
        }

        @Override
        public void onNext(TlogInfo info) {
            mTlogInfo = info;
            if (mUserId == null) {
                mUserId = info.author.getId();
                refreshFeed();
            }
            setupFeedDesign();
            if (mAdapter != null) mAdapter.setUser(info.author);
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
        public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount);
        public void onTlogInfoLoaded(TlogInfo info);
        public void onSharePostMenuClicked(Entry entry);
        public void onListClicked();
        public void onNoSuchUser();
        public boolean isOverlayVisible();
    }
}
