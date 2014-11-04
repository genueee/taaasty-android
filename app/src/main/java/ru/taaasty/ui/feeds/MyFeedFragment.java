package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import it.sephiroth.android.library.picasso.RequestCreator;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
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

    private ApiMyFeeds mFeedsService;
    private Adapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
    private Subscription mCurrentUserSubscription = SubscriptionHelper.empty();

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
        mFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiMyFeeds.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

        mAdapter = new Adapter(getActivity(), false);
        mAdapter.onCreate();

        if (savedInstanceState != null) {
            List<Entry> feed = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
            if (feed != null && !feed.isEmpty()) mAdapter.setFeed(feed);
        }

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.setAdapter(mAdapter);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mDateIndicatorView.setVisibility(View.VISIBLE);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicator(dy > 0);
            }
        });

        mAdapter.registerAdapterDataObserver(mUpdateIndicatorObserver);

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
        if (!mRefreshLayout.isRefreshing()) refreshData();
        updateDateIndicator(true);
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
        mCurrentUserSubscription.unsubscribe();
        mListView = null;
        mDateIndicatorView = null;
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

    void onAdditionalMenuButtonClicked(View v) {
        if (mListener != null) mListener.onShowAdditionalMenuClicked();
    }

    void onAvatarClicked(View v) {
        if (mListener != null) mListener.onAvatarClicked(mCurrentUser,
                mCurrentUser == null ? null : mCurrentUser.getDesign());
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
        mListView.setBackgroundDrawable(new ColorDrawable(design.getFeedBackgroundColor(getResources())));
        mAdapter.setFeedDesign(design);
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    class Adapter extends FeedItemAdapter {
        private String mTitle;
        private User mUser = User.DUMMY;

        public Adapter(Context context, boolean showUserAvatar) {
            super(context, showUserAvatar);
        }

        @Override
        protected void initClickListeners(final ListEntryBase holder) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long postId = mListView.getChildItemId(v);
                    onFeedItemClicked(mAdapter.getItemById(postId));

                }
            });
            holder.getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_my_feed, mListView, false);
            HeaderHolder holder = new HeaderHolder(child);
            holder.avatarView.setOnClickListener(mOnClickListener);
            child.findViewById(R.id.additional_menu).setOnClickListener(mOnClickListener);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
            HeaderHolder holder = (HeaderHolder)viewHolder;
            holder.titleView.setText(mTitle);
            bindDesign(holder);
            bindUser(holder);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId) {
            return AndroidObservable.bindFragment(MyFeedFragment.this,
                    mFeedsService.getMyFeed(sinceEntryId, Constants.LIST_FEED_APPEND_LENGTH));
        }

        @Override
        protected void onRemoteError(Throwable e) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
        }

        public void setTitleUser(String title, User user) {
            mTitle = title;
            mUser = user;
            notifyItemChanged(0);
        }

        private void bindDesign(HeaderHolder holder) {
            if (mFeedDesign == null) return;
            String backgroudUrl = mFeedDesign.getBackgroundUrl();
            if (TextUtils.equals(holder.backgroundUrl, backgroudUrl)) return;
            holder.feedDesignTarget = new TargetSetHeaderBackground(holder.itemView,
                    mFeedDesign, Color.TRANSPARENT, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS);
            holder.backgroundUrl = backgroudUrl;
            RequestCreator rq =  NetworkUtils.getInstance().getPicasso(holder.itemView.getContext())
                    .load(backgroudUrl);
            if (holder.itemView.getWidth() > 1 && holder.itemView.getHeight() > 1) {
                rq.resize(holder.itemView.getWidth() / 2, holder.itemView.getHeight() / 2, true)
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
                        onAvatarClicked(v);
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
            super(itemView);
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            titleView = (TextView)itemView.findViewById(R.id.user_name);
        }
    }

    class LikesHelper extends ru.taaasty.utils.LikesHelper {

        public LikesHelper() {
            super(MyFeedFragment.this);
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

    public void onFeedItemClicked(Entry entry) {
        if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + entry.getId());
        Intent i = ShowPostActivity.createShowPostIntent(getActivity(), entry.getId(), entry,
                mCurrentUser == null ? null : mCurrentUser.getDesign());
        startActivity(i);
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

    final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            throw new IllegalStateException();
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            new LikesHelper().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            Intent i = ShowPostActivity.createShowPostIntent(getActivity(), entry.getId(), entry,
                    mCurrentUser == null ? null : mCurrentUser.getDesign());
            startActivity(i);
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
        setRefreshing(true);
        Observable<CurrentUser> observableCurrentUser = AndroidObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mCurrentUserSubscription = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
    }

    private void refreshFeed() {
        if (!mFeedSubscription.isUnsubscribed()) {
            mFeedSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        setRefreshing(true);
        Observable<Feed> observableFeed = AndroidObservable.bindFragment(this,
                mFeedsService.getMyFeed(null, Constants.LIST_FEED_INITIAL_LENGTH));
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
            mDateIndicatorView.setVisibility(mAdapter.isEmpty() ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            // XXX
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.refreshItems(feed.entries);
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
        public void onAvatarClicked(User user, TlogDesign design);
        public void onCurrentUserLoaded(User user, TlogDesign design);
        public void onSharePostMenuClicked(Entry entry);
    }
}
