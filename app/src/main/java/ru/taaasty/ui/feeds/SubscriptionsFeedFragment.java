package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.FeedList;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.ParallaxedHeaderHolderTitleSubtitle;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.SmartTextSwitcher;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;


/**
 * Мои подписки
 */
public class SubscriptionsFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SubscriptionsFeedFragment";

    private static final String BUNDLE_KEY_FEED_ITEMS = "feed_items";
    private static final String BUNDLE_KEY_FEED_DESIGN = "feed_design";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;
    private DateIndicatorWidget mDateIndicatorView;

    private ApiMyFeeds mFeedsService;
    private Adapter mAdapter;
    private FeedLoader mFeedLoader;

    private Subscription mCurrentUserSubscribtion = SubscriptionHelper.empty();

    private int mRefreshCounter;

    private TlogDesign mTlogDesign;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static SubscriptionsFeedFragment newInstance() {
        return new SubscriptionsFeedFragment();
    }

    public SubscriptionsFeedFragment() {
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
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_list_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), true));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicator(dy > 0);
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

        FeedList feed = null;
        if (savedInstanceState != null) feed = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_ITEMS);
        mAdapter = new Adapter(getActivity(), feed);
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
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            FeedList feed = mAdapter.getFeed();
            outState.putParcelable(BUNDLE_KEY_FEED_ITEMS, feed);
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
        mCurrentUserSubscribtion.unsubscribe();
        mListView = null;
        mDateIndicatorView = null;
        if (mFeedLoader != null) {
            mFeedLoader.onDestroy();
            mFeedLoader = null;
        }
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mUpdateIndicatorObserver);
            mAdapter.onDestroy();
            mAdapter = null;
        }
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
        mListView.setBackgroundDrawable(new ColorDrawable(mTlogDesign.getFeedBackgroundColor(getResources())));
        mAdapter.setFeedDesign(mTlogDesign);
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    class Adapter extends FeedItemAdapter {

        public Adapter(Context context, FeedList feed) {
            super(context, feed, true);
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
            ParallaxedHeaderHolder holder = new ParallaxedHeaderHolderTitleSubtitle(child);
            ((SmartTextSwitcher)child.findViewById(R.id.title)).setText(R.string.my_subscriptions);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
        }

        private void setPostClickListener(final ListEntryBase pHolder) {
            // Клик на записи
            pHolder.itemView.setOnClickListener(mOnItemClickListener);
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
        }

        final View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecyclerView.ViewHolder vh = mListView.getChildViewHolder(v);
                Entry entry = getAnyEntryAtHolderPosition(vh);
                Bitmap thumbnail = null;
                if (vh instanceof ListImageEntry) thumbnail = ((ListImageEntry) vh).getCachedImage();
                if (entry != null) {
                    if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + entry.getId());
                    new ShowPostActivity.Builder(getActivity())
                            .setEntry(entry)
                            .setThumbnailBitmap(thumbnail, "SubscriptionsFeedFragment-" + entry.getId() + "-")
                            .setSrcView(v)
                            .startActivity();
                }
            }
        };
    }

    class FeedLoader extends ru.taaasty.ui.feeds.FeedLoader {

        public FeedLoader(FeedItemAdapter adapter) {
            super(adapter);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return AndroidObservable.bindFragment(SubscriptionsFeedFragment.this,
                    mFeedsService.getMyFriendsFeed(sinceEntryId, limit));
        }

        @Override
        public void onLoadCompleted(boolean isRefresh, int entriesRequested) {
            if (DBG) Log.v(TAG, "onCompleted()");
            if (isRefresh) {
                mEmptyView.setVisibility(mAdapter.getFeed().isEmpty() ? View.VISIBLE : View.GONE);
                mDateIndicatorView.setVisibility(mAdapter.getFeed().isEmpty() ? View.INVISIBLE : View.VISIBLE);
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
            if (mListView != null) {
                mListView.removeCallbacks(mUpdateIndicatorRunnable);
                mListView.postDelayed(mUpdateIndicatorRunnable, 64);
            }
        }
    };

    public class LikesHelper extends ru.taaasty.utils.LikesHelper {

        public LikesHelper() {
            super(SubscriptionsFeedFragment.this);
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
            mTlogDesign.setIsLightTheme(true); // Мои подписки всегда светлые
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

    }
}
