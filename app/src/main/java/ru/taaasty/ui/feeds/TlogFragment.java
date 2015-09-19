package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import junit.framework.Assert;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.RelationshipRemoved;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.FabHelper;
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
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

public class TlogFragment extends Fragment implements IRereshable, ListFeedWorkRetainedFragment.TargetFragmentInteraction {

    public static final int FOLLOWING_STATUS_UNKNOWN = 0;
    public static final int FOLLOWING_STATUS_ME_SUBSCRIBED = 0x01;
    public static final int FOLLOWING_STATUS_ME_UNSUBSCRIBED = 0x02;
    public static final int FOLLOWING_STATUS_CHANGING = 0x80;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FOLLOWING_STATUS_UNKNOWN, FOLLOWING_STATUS_ME_SUBSCRIBED,
            FOLLOWING_STATUS_ME_UNSUBSCRIBED, FOLLOWING_STATUS_CHANGING})
    public @interface FollowingStatus {}

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogFragment";
    private static final String ARG_USER_ID = "user_id";
    private static final String ARG_USER_SLUG = "user_slug";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "avatar_thumbnail_res";

    private static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 5;

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

    private FabHelper mFabHelper;

    private FabHelper.AutoHideScrollListener mHideFabScrollListener;

    private boolean mScheduleRefreshData;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
        mFabHelper = new FabHelper(v.findViewById(R.id.post));
        mFabHelper.hideFab(false);

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

        mFabHelper.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CreatePostActivity.startCreatePostActivityForResult(getActivity(),
                        (mWorkFragment.isFlow() ? mWorkFragment.getUser().author.getId() :  null),
                        CREATE_POST_ACTIVITY_REQUEST_CODE);
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREATE_POST_ACTIVITY_REQUEST_CODE:
                if (requestCode == Activity.RESULT_OK) {
                    if (mWorkFragment != null) {
                        refreshData(true);
                    } else {
                        mScheduleRefreshData = true;
                    }
                }
                break;
        }
    }

    @Override
    public void onWorkFragmentActivityCreated() {
        reinitAdapter();
        refreshFabStatus();
    }

    @Override
    public void onWorkFragmentResume() {
        if (!mWorkFragment.isRefreshing() || mScheduleRefreshData) {
            refreshData(mScheduleRefreshData);
            mScheduleRefreshData = false;
        }
        mDateIndicatorHelper.onResume();
        updateDateIndicatorVisibility();
        refreshFabStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacksAndMessages(null);
        mDateIndicatorView = null;
        mFabHelper = null;
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
        TlogInfo tlogInfo = mWorkFragment.getUser();
        if (mListener != null) mListener.onTlogInfoLoaded(tlogInfo);

        boolean oldIsFlow = mAdapter != null && mAdapter.isFlow();
        if (oldIsFlow != tlogInfo.author.isFlow()) {
            reinitAdapter();
        }

        setupFeedDesign();
        if (mAdapter != null) mAdapter.setUser(tlogInfo.author);
        setupLoadingState();
        refreshFabStatus();
        if (isFlow()) {
            mAdapter.notifyItemChanged(0);
        }
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
                .set(mWorkFragment.getUser().author, v, user.getDesign())
                .setPreloadAvatarThumbnail(R.dimen.feed_header_avatar_normal_diameter)
                .startActivity();
    }

    @Nullable
    public Long getUserId() {
        return mWorkFragment.mUserId;
    }

    public boolean isFlow() {
        return mWorkFragment.isFlow();
    }

    @FollowingStatus
    public int getFollowingStatus() {
        return mWorkFragment.getFollowingStatus();
    }

    public void startFollow() {
        mWorkFragment.startFollow();
    }

    public void startUnfollow() {
        mWorkFragment.startUnfollow();
    }

    void setupFeedDesign() {
        TlogInfo user = mWorkFragment.getUser();
        if (user == null) return;
        TlogDesign design = user.getDesign();

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

    void setupLoadingState() {
        if (mRefreshLayout == null) return;

        if (DBG) Log.v(TAG, "setupLoadingState() ts: " + System.currentTimeMillis() + "  work fragment != null: "
                        + (mWorkFragment != null)
                        + " isRefreshing: " + (mWorkFragment != null && mWorkFragment.isRefreshing())
                        + " isLoading: " + (mWorkFragment != null && mWorkFragment.isLoading())
                        + " feed is empty: " + (mWorkFragment != null && mWorkFragment.isFeedEmpty())
                        + " adapter != null: " + (mAdapter != null)
        );

        // Здесь индикатор не ставим, только снимаем. Устанавливает индикатор либо сам виджет
        // при свайпе вверх, либо если адаптер пустой. В другом месте.
        if (mWorkFragment != null && !mWorkFragment.isLoading()) {
            if (DBG) Log.v(TAG, "mRefreshLayout.setRefreshing(false) ts: " + System.currentTimeMillis());
            mRefreshLayout.setRefreshing(false);
        }

        boolean listIsEmpty = mAdapter != null
                && mWorkFragment != null
                && mWorkFragment.isFeedEmpty();

        boolean isTlogForbidden = (mWorkFragment != null) && mWorkFragment.isTlogForbidden();

        mEmptyView.setVisibility(listIsEmpty || isTlogForbidden ? View.VISIBLE : View.GONE);
        if (isTlogForbidden) {
            mEmptyView.setText(isFlow() ? R.string.error_flow_access_denied : R.string.error_tlog_access_denied);
            mEmptyView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_private_post_indicator_activated, 0, 0);
        } else {
            mEmptyView.setText(isFlow() ? R.string.no_records : R.string.user_have_not_written_anything);
            mEmptyView.setCompoundDrawables(null, null, null, null);
        }
        updateDateIndicatorVisibility();
    }

    void onFollowingStatusChanged() {
        if (!isFlow()) return;
        if (DBG) Log.v(TAG, "onFollowingStatusChanged()");
        if (mAdapter != null) mAdapter.notifyItemChanged(0);
        refreshFabStatus();
    }

    private void reinitAdapter() {
        if (DBG) Log.v(TAG, "reinitAdapter() is first: " + (mDateIndicatorHelper == null ? "true" : "false"));
        if (mDateIndicatorHelper != null) {
            mListView.removeOnScrollListener(mDateIndicatorHelper.onScrollListener);
            mDateIndicatorHelper.onDestroy();
            mDateIndicatorHelper = null;
        }

        if (mAdapter != null) {
            mAdapter.onDestroy(mListView);
        }

        final boolean showPostAuthor;
        final boolean showFab;
        if (mWorkFragment.getUser() != null) {
            showPostAuthor = mWorkFragment.getUser().author.isFlow();
        } else {
            showPostAuthor = false;
            showFab = false;
        }

        mAdapter = new Adapter(mWorkFragment.getEntryList(), showPostAuthor);
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

    private void refreshFabStatus() {
        boolean showFab = false;
        if (mWorkFragment != null && mWorkFragment.getUser() != null) {
            // TODO нормальное определение, можно ли писать в поток
            showFab = mWorkFragment.isFlow() && Relationship.isMeSubscribed(mWorkFragment.getUser().getMyRelationship());
        }

        if (showFab) {
            if (mHideFabScrollListener == null) {
                mHideFabScrollListener = new FabHelper.AutoHideScrollListener(mFabHelper);
                mListView.addOnScrollListener(mHideFabScrollListener);
            }
            mFabHelper.showFab(false);
        } else {
            if (mHideFabScrollListener != null) {
                mListView.removeOnScrollListener(mHideFabScrollListener);
                mHideFabScrollListener = null;
            }
            mFabHelper.hideFab(false);
        }
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
        private User mUser = User.DUMMY;
        private ImageUtils.DrawableTarget mAvatarThumbnailLoadTarget;
        private ImageUtils.DrawableTarget mAvatarLoadTarget;
        public Adapter(SortedList<Entry> feed, boolean isFlow) {
            super(feed, isFlow);
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
                        if (mShowUserAvatar) {
                            ((ListEntryBase) pHolder).getAvatarAuthorView().setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Entry entry = mAdapter.getAnyEntryAtHolderPosition(pHolder);
                                    if (mListener != null && entry != null)
                                        mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
                                }
                            });
                        }
                    }
                }

                @Override
                public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                    if (isFlow()) {
                        View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_flow, mListView, false);
                        FlowHeaderHolder holder = new FlowHeaderHolder(child);
                        holder.subscribeButton.setOnClickListener(mOnClickListener);
                        holder.unsubscribeButton.setOnClickListener(mOnClickListener);
                        return holder;
                    } else {
                        View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_tlog, mListView, false);
                        TlogHeaderHolder holder = new TlogHeaderHolder(child);
                        holder.avatarView.setOnClickListener(mOnClickListener);
                        return holder;
                    }
                }

                @Override
                public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                    if (isFlow()) {
                        FlowHeaderHolder holder = (FlowHeaderHolder) viewHolder;
                        holder.titleView.setText('#' + UiUtils.capitalize(mUser.getName()));
                        holder.subtitleView.setText(mUser.getTitle());
                        bindHeaderFollowingStatus(holder);
                    } else {
                        TlogHeaderHolder holder = (TlogHeaderHolder) viewHolder;
                        holder.titleView.setText(UiUtils.capitalize(mUser.getName()));
                        bindUser(holder);
                    }
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    addEntry(event.postEntry);
                }
            });
        }

        public boolean isFlow() {
            return mShowUserAvatar;
        }

        public void setUser(User user) {
            if (DBG) Assert.assertTrue(isFlow() == user.isFlow());
            if (!Objects.equals(mUser, user)) {
                mUser = user;
                notifyItemChanged(0);
            }
        }

        private void bindUser(final TlogHeaderHolder holder) {
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

        private void bindHeaderFollowingStatus(FlowHeaderHolder holder) {
            boolean subscribeVisible = false;
            boolean unsubscribeVisible = false;
            boolean progressVisible = false;

            switch (getFollowingStatus()) {
                case FOLLOWING_STATUS_UNKNOWN:
                    break;
                case FOLLOWING_STATUS_CHANGING:
                    progressVisible = true;
                    break;
                case FOLLOWING_STATUS_ME_SUBSCRIBED:
                    unsubscribeVisible = true;
                    break;
                case FOLLOWING_STATUS_ME_UNSUBSCRIBED:
                    subscribeVisible = true;
                    break;
                default:
                    throw new IllegalStateException();
            }

            holder.subscribeButton.setVisibility(subscribeVisible ? View.VISIBLE : View.INVISIBLE);
            holder.unsubscribeButton.setVisibility(unsubscribeVisible ? View.VISIBLE : View.INVISIBLE);
            holder.subscribeProgressButton.setVisibility(progressVisible ? View.VISIBLE : View.INVISIBLE);
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.avatar:
                        onAvatarClicked(v);
                        break;
                    case R.id.header_flow_subscribe:
                        mWorkFragment.startFollow();
                        break;
                    case R.id.header_flow_unsubscribe:
                        mWorkFragment.startUnfollow();
                        break;
                }
            }
        };
    }

    public static class TlogHeaderHolder extends ParallaxedHeaderHolder {
        TextView titleView;
        ImageView avatarView;

        public TlogHeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.avatar_user_name));
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            titleView = (TextView)itemView.findViewById(R.id.user_name);
        }
    }

    public static class FlowHeaderHolder extends ParallaxedHeaderHolder {
        TextView titleView;

        TextView subtitleView;

        View subscribeButton;

        View unsubscribeButton;

        View subscribeProgressButton;

        public FlowHeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.title_subtitle_container));

            titleView = (TextView)itemView.findViewById(R.id.title);
            subtitleView = (TextView)itemView.findViewById(R.id.subtitle);
            subscribeButton = itemView.findViewById(R.id.header_flow_subscribe);
            unsubscribeButton = itemView.findViewById(R.id.header_flow_unsubscribe);
            subscribeProgressButton = itemView.findViewById(R.id.header_flow_follow_unfollow_progress);
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
                design = mWorkFragment.getUser().getDesign();
            }
            new ShowPostActivity.Builder(getActivity())
                    .setEntry(entry)
                    .setDesign(design)
                    .setSrcView(view)
                    .startActivity();
        }

        @Override
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mWorkFragment != null && mWorkFragment.getUserId() != null) {
                if (mListener != null) mListener.onSharePostMenuClicked(entry, mWorkFragment.getUserId());
            } else {
                // User ID ещё неизвестен. Может быть, если тлог открыт по слагу.
                // TODO что-нибудь делать
                // TODO запретить такой вариант, показывать посты только после того, как инфа загружена?
            }
        }
    };


    public static class WorkRetainedFragment extends Fragment {

        private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.TlogFragment.WorkRetainedFragment.BUNDLE_KEY_FEED_ITEMS";
        private static final String BUNDLE_KEY_TLOG_INFO = "ru.taaasty.ui.feeds.TlogFragment.WorkRetainedFragment.BUNDLE_KEY_TLOG_INFO";

        private FeedLoader mFeedLoader;

        private Long mUserId;

        private String mUserSlug;

        private Subscription mCurrentUserSubscription = Subscriptions.unsubscribed();

        private Subscription mFollowSubscription = Subscriptions.unsubscribed();

        private TlogInfo mUser;

        private OnFragmentInteractionListener mListener;

        private SortedList<Entry> mEntryList;


        @Override
        public void onAttach(Context context) {
            super.onAttach(context);
            try {
                mListener = (OnFragmentInteractionListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString()
                        + " must implement OnFragmentInteractionListener");
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
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
            mFollowSubscription.unsubscribe();
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

        public boolean isFlow() {
            return mUser != null && mUser.author.isFlow();
        }

        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return RestClient.getAPiTlog().getEntries(String.valueOf(mUserId), sinceEntryId, limit);
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

        @Nullable
        public Long getUserId() {
            return mUserId;
        }

        /**
         * @return Активна какая-либо из загрузок: пользователь, рефреш, подгрузка
         */
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
            Observable<TlogInfo> observableCurrentUser = RestClient.getAPiTlog().getUserInfo(mUserId == null
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

        @FollowingStatus
        public int getFollowingStatus() {
            if (!mFollowSubscription.isUnsubscribed()) {
                return FOLLOWING_STATUS_CHANGING;
            }

            if (mUser == null) return FOLLOWING_STATUS_UNKNOWN;

            if (Relationship.isMeSubscribed(mUser.getMyRelationship())) {
                return FOLLOWING_STATUS_ME_SUBSCRIBED;
            } else {
                return FOLLOWING_STATUS_ME_UNSUBSCRIBED;
            }
        }

        public void startFollow() {
            if (mUserId == null) return;
            mFollowSubscription.unsubscribe();
            ApiRelationships relApi = RestClient.getAPiRelationships();
            Observable<Relationship> observable = AppObservable.bindSupportFragment(this,
                    relApi.follow(mUserId.toString()));
            mFollowSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .finallyDo(new Action0() {
                        @Override
                        public void call() {
                            callFollowingStatusChanged();
                        }
                    })
                    .subscribe(new RelationChangedObserver());
            callFollowingStatusChanged();
        }

        public void startUnfollow() {
            if (mUserId == null) return;
            mFollowSubscription.unsubscribe();
            ApiRelationships relApi = RestClient.getAPiRelationships();
            Observable<Relationship> observable = AppObservable.bindSupportFragment(this,
                    relApi.unfollow(mUserId.toString()));
            mFollowSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .finallyDo(new Action0() {
                        @Override
                        public void call() {
                            callFollowingStatusChanged();
                        }
                    })
                    .subscribe(new RelationChangedObserver());
            callFollowingStatusChanged();
        }

        void callLoadingStateChanged(String reason) {
            if (DBG) Log.v(TAG, "callLoadingStateChanged: " + reason);
            if (getMainFragment() != null) getMainFragment().onLoadingStateChanged(reason);
        }

        void callFollowingStatusChanged() {
            if (mListener != null) mListener.onFollowingStatusChanged();
            if (getMainFragment() != null) getMainFragment().onFollowingStatusChanged();
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

        private class RelationChangedObserver implements Observer<Relationship> {

            @SuppressWarnings("ConstantConditions")
            public RelationChangedObserver() {
            }

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                if (mListener != null) mListener.notifyError(UiUtils.getUserErrorText(getResources(), e, R.string.error_follow), e);
            }

            @Override
            public void onNext(Relationship relationship) {
                mUser.setMyRelationship(relationship.getState());
                if (relationship.getId() == null) {
                    EventBus.getDefault().post(new RelationshipRemoved(relationship));
                } else {
                    EventBus.getDefault().post(new RelationshipChanged(relationship));
                }
            }
        }

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
        void onSharePostMenuClicked(Entry entry, long tlogId);
        void onListClicked();
        void onNoSuchUser();
        void onFollowingStatusChanged();
        boolean isOverlayVisible();

        /**
         * Юзер ткнул на аватарку в списке
         * @param view
         * @param user
         * @param design
         */
        void onAvatarClicked(View view, User user, TlogDesign design);
    }
}
