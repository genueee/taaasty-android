package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
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
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class MyFeedFragment extends Fragment implements IRereshable, SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedFragment";

    private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.MyFeedFragment.feed_items";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private Adapter mAdapter;
    private MyFeedLoader mFeedLoader;

    private Subscription mCurrentUserSubscription = SubscriptionHelper.empty();

    private boolean mForceShowRefreshingIndicator;

    private CurrentUser mCurrentUser;

    private DateIndicatorWidget mDateIndicatorView;

    private int mRefreshCounter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static MyFeedFragment newInstance() {
        return new MyFeedFragment();
    }

    public MyFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.getItemAnimator().setSupportsChangeAnimations(false);
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), false));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicator(dy > 0);
            }
        });
        mForceShowRefreshingIndicator = false;

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
    public void onResume() {
        super.onResume();
        if (!isLoading()) refreshData(false);
        updateDateIndicator(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> feed = mAdapter.getFeed().getItems();
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, new ArrayList<Entry>(feed));
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

        mListView.setAdapter(mAdapter);
        mFeedLoader = new MyFeedLoader(mAdapter);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mCurrentUserSubscription.unsubscribe();
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
        refreshData();
    }


    boolean isLoading() {
        return mFeedLoader.isRefreshing() || !mCurrentUserSubscription.isUnsubscribed();
    }

    void setupRefreshingIndicator() {
        if (mAdapter == null) return;
        boolean showIndicator = mAdapter.getFeed().isEmpty() || mForceShowRefreshingIndicator;
        mRefreshLayout.setRefreshing(showIndicator && isLoading());

        if (!isLoading()) mForceShowRefreshingIndicator = false;
    }


    public void refreshData() {
        refreshData(true);
    }

    public void refreshData(boolean showIndicator) {
        if (DBG) Log.v(TAG, "refreshData()");
        if (showIndicator) mForceShowRefreshingIndicator = true;
        refreshUser();
        refreshFeed();
    }

    void onAdditionalMenuButtonClicked(View v) {
        if (mListener != null) mListener.onShowAdditionalMenuClicked();
    }

    void setupUser(CurrentUser user) {
        if (user == null) {
            // XXX
        } else {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0,1).toUpperCase(Locale.getDefault()) + name.substring(1);
            if (mAdapter != null) {
                mAdapter.setTitleUser(name, user);
            }
        }
    }

    void setupFeedDesign(TlogDesign design) {
        if (DBG) Log.e(TAG, "Setup feed design " + design);
        mAdapter.setFeedDesign(design);
        mListView.setBackgroundDrawable(new ColorDrawable(design.getFeedBackgroundColor(getResources())));
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private User mUser = User.DUMMY;

        public Adapter(Context context, List<Entry> feed) {
            super(context, feed, false);
        }

        @Override
        protected boolean initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
            // Все посты
            if (pHolder instanceof ListEntryBase) {
                ((ListEntryBase)pHolder).getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
                // Клики на картинках
                FeedsHelper.setupListEntryClickListener(this, (ListEntryBase)pHolder);
                return true;
            }


            return false;
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            if (DBG) Log.v(TAG, "onCreateHeaderViewHolder");
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_my_feed, mListView, false);
            HeaderHolder holder = new HeaderHolder(child);
            holder.avatarView.setOnClickListener(mOnClickListener);
            child.findViewById(R.id.additional_menu).setOnClickListener(mOnClickListener);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
            if (DBG) Log.v(TAG, "onBindHeaderViewHolder");
            HeaderHolder holder = (HeaderHolder)viewHolder;
            holder.titleView.setText(mTitle);
            bindDesign(holder);
            bindUser(holder);
        }

        @Override
        public void onEventMainThread(EntryChanged update) {
            addEntry(update.postEntry);
        }

        public void setTitleUser(String title, User user) {
            if (!TextUtils.equals(mTitle, title) && !Objects.equals(mUser, user)) {
                mTitle = title;
                mUser = user;
                notifyItemChanged(0);
            }
        }

        private void bindDesign(HeaderHolder holder) {
            if (mFeedDesign == null) return;
            String backgroudUrl = mFeedDesign.getBackgroundUrl();
            if (TextUtils.equals(holder.backgroundUrl, backgroudUrl)) return;
            holder.feedDesignTarget = new TargetSetHeaderBackground(holder.itemView,
                    mFeedDesign, Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS) {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    super.onBitmapLoaded(bitmap, from);
                    ImageUtils.getInstance().putBitmapToCache(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY, bitmap);
                }
            };
            holder.backgroundUrl = backgroudUrl;
            RequestCreator rq =  Picasso.with(holder.itemView.getContext())
                    .load(backgroudUrl);
            if (holder.itemView.getWidth() > 1 && holder.itemView.getHeight() > 1) {
                rq.resize(holder.itemView.getWidth() / 2, holder.itemView.getHeight() / 2)
                        .centerCrop();
            }
            rq.into(holder.feedDesignTarget);
        }

        private void bindUser(HeaderHolder holder) {
            ImageUtils.getInstance().loadAvatar(mUser.getUserpic(), mUser.getName(),
                    holder.avatarView,
                    R.dimen.avatar_normal_diameter
            );
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.additional_menu:
                        onAdditionalMenuButtonClicked(v);
                        break;
                    case R.id.avatar:
                        if (mListener != null) mListener.onCurrentUserAvatarClicked(v, mCurrentUser,
                                mCurrentUser == null ? null : mCurrentUser.getDesign());
                        break;
                }
            }
        };
    }

    static class HeaderHolder extends ParallaxedHeaderHolder {
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

    final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

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
            if (design == null && mCurrentUser != null) design = mCurrentUser.getDesign();
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

    public void refreshUser() {
        if (!mCurrentUserSubscription.isUnsubscribed()) {
            mCurrentUserSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }
        Observable<CurrentUser> observableCurrentUser = AppObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mCurrentUserSubscription = observableCurrentUser
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

    class MyFeedLoader extends FeedLoaderLite {

        private final ApiMyFeeds mFeedsService;

        public MyFeedLoader(FeedItemAdapterLite adapter) {
            super(adapter);
            mFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiMyFeeds.class);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return AppObservable.bindFragment(MyFeedFragment.this,
                    mFeedsService.getMyFeed(sinceEntryId, limit));
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

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

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
        public void onNext(CurrentUser currentUser) {
            mCurrentUser = currentUser;
            setupFeedDesign(currentUser.getDesign());
            setupUser(currentUser);
            if (mListener != null) mListener.onCurrentUserLoaded(currentUser, currentUser.getDesign());
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
        public void onShowAdditionalMenuClicked();
        public void onCurrentUserAvatarClicked(View view, User user, TlogDesign design);
        public void onCurrentUserLoaded(CurrentUser user, TlogDesign design);
        public void onSharePostMenuClicked(Entry entry);
    }
}
