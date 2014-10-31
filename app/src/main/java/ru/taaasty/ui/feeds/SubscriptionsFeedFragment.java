package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

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
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.widgets.EntryBottomActionBar;
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

    private static final String FRAGMENT_TAG_SHARE_ENTRY_DIALOG = "share_entry_dialog";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private ApiMyFeeds mFeedsService;
    private Adapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
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
        View v = inflater.inflate(R.layout.fragment_list_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(this);

        mAdapter = new Adapter(getActivity(), true);
        mAdapter.onCreate();
        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.setAdapter(mAdapter);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));

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

        if (savedInstanceState != null) {
            ArrayList<Entry> entries = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
            if (entries != null) {
                mAdapter.setFeed(entries);
            }
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
            List<Entry> entries = mAdapter.getFeed();
            ArrayList<Entry> entriesArrayList = new ArrayList<>(entries);
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, entriesArrayList);
        }
        if (mTlogDesign != null) {
            outState.putParcelable(BUNDLE_KEY_FEED_DESIGN, mTlogDesign);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mCurrentUserSubscribtion.unsubscribe();
        mListView = null;
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

    class Adapter extends FeedItemAdapter {

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
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_title_subtitle, mListView, false);
            ParallaxedHeaderHolder holder = new ParallaxedHeaderHolder(child);
            ((TextView)child.findViewById(R.id.title)).setText(R.string.my_subscriptions);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId) {
            return AndroidObservable.bindFragment(SubscriptionsFeedFragment.this,
                    mFeedsService.getMyFriendsFeed(sinceEntryId, Constants.LIST_FEED_APPEND_LENGTH));
        }

        @Override
        protected void onRemoteError(Throwable e) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
        }
    }

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

    public void onFeedItemClicked(Entry entry) {
        if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + entry.getId());
        Intent i = ShowPostActivity.createShowPostIntent(getActivity(), entry.getId(), entry, null);
        startActivity(i);
    }

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            Intent i = new Intent(SubscriptionsFeedFragment.this.getActivity(), TlogActivity.class);
            i.putExtra(TlogActivity.ARG_USER_ID, entry.getAuthor().getId());
            SubscriptionsFeedFragment.this.getActivity().startActivity(i);
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            new LikesHelper().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            Intent i = ShowPostActivity.createShowPostIntent(getActivity(), entry.getId(), entry, null);
            startActivity(i);
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
        if (!mFeedSubscription.isUnsubscribed()) {
            if (DBG) Log.v(TAG, "feed subsription is not unsubscribed " + mFeedSubscription);
            mFeedSubscription.unsubscribe();
            mStopRefreshingAction.call();
        }

        setRefreshing(true);
        Observable<Feed> observableFeed = AndroidObservable.bindFragment(this,
                mFeedsService.getMyFriendsFeed(null, Constants.LIST_FEED_INITIAL_LENGTH));
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
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            if (mListener != null) mListener.notifyError(getText(R.string.server_error), e);
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
        public void onSharePostMenuClicked(Entry entry);
    }
}
