package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxListView;

import java.util.Locale;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.ShowPostActivity;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.CirclePageStaticIndicator;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;


public class MyAdditionalFeedFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedFragment";
    private static final String BUNDLE_ARG_FEED_TYPE = "BUNDLE_ARG_FEED_TYPE";
    private static final String BUNDLE_ARG_PAGE_IDX = "BUNDLE_ARG_PAGE_IDX";
    private static final String BUNDLE_ARG_PAGE_COUNT = "BUNDLE_ARG_PAGE_COUNT";

    private final CircleTransformation mCircleTransformation = new CircleTransformation();

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private ParallaxListView mListView;
    private View mEmptyView;
    private ViewGroup mHeaderView;

    private ApiMyFeeds mMyFeedsService;
    private FeedItemAdapter mAdapter;

    private Subscription mFeedSubscription = SubscriptionHelper.empty();
    private Subscription mUserSubscribtion = SubscriptionHelper.empty();

    private @MyAdditionalFeedActivity.FeedType
    int mFeedType = MyAdditionalFeedActivity.FEED_TYPE_FAVORITES;
    private int mRefreshCounter;

    private CurrentUser mCurrentUser;

    // XXX: anti picasso weak ref
    private TargetSetHeaderBackground mFeedDesignTarget;

    private int mPageIdx;
    private int mPageCount;

    public static MyAdditionalFeedFragment newInstance(@MyAdditionalFeedActivity.FeedType int type,
                                                       int pageIdx, int pageCount) {
        MyAdditionalFeedFragment usf = new MyAdditionalFeedFragment();
        Bundle b = new Bundle();
        b.putInt(BUNDLE_ARG_FEED_TYPE, type);
        b.putInt(BUNDLE_ARG_PAGE_IDX, pageIdx);
        b.putInt(BUNDLE_ARG_PAGE_COUNT, pageCount);
        usf.setArguments(b);
        return usf;
    }

    public MyAdditionalFeedFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMyFeedsService = NetworkUtils.getInstance().createRestAdapter().create(ApiMyFeeds.class);

        if (getArguments() != null) {
            Bundle args = getArguments();
            //noinspection ResourceType
            mFeedType = args.getInt(BUNDLE_ARG_FEED_TYPE, MyAdditionalFeedActivity.FEED_TYPE_MAIN);
            mPageIdx = args.getInt(BUNDLE_ARG_PAGE_IDX, 0);
            mPageCount = args.getInt(BUNDLE_ARG_PAGE_COUNT, 3);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mListView = (ParallaxListView) v.findViewById(R.id.list_view);
        mHeaderView = (ViewGroup) inflater.inflate(R.layout.header_user_feed, mListView, false);
        mEmptyView = v.findViewById(R.id.empty_view);
        mHeaderView.findViewById(R.id.avatar).setOnClickListener(mOnClickListener);
        mRefreshLayout.setOnRefreshListener(this);
        setupEmptyView(v);
        setupFeedName(v);
        setupPageIndicator(mHeaderView);

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

        mListView.addParallaxedHeaderView(mHeaderView);
        mAdapter = new FeedItemAdapter(getActivity(), mOnFeedItemClickListener);
        mListView.setAdapter(mAdapter);

        if (!mRefreshLayout.isRefreshing()) refreshData();
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

    void setupEmptyView(View root) {
        int textNoRecords;
        switch (mFeedType) {
            case MyAdditionalFeedActivity.FEED_TYPE_MAIN:
                textNoRecords = R.string.you_have_not_written_anything;
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FRIENDS:
                textNoRecords = R.string.friends_have_not_written_anything;
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FAVORITES:
                textNoRecords = R.string.you_have_no_favorite_records;
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_PRIVATE:
                textNoRecords = R.string.you_have_no_private_records;
                break;
            default:
                textNoRecords = R.string.no_records;
                break;
        }

        ((TextView)root.findViewById(R.id.empty_view)).setText(textNoRecords);
    }

    void setupFeedName(View root) {
        TextView name = (TextView)mHeaderView.findViewById(R.id.feed_name);
        switch (mFeedType) {
            case MyAdditionalFeedActivity.FEED_TYPE_MAIN:
                name.setVisibility(View.GONE);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FRIENDS:
                name.setText(R.string.friends);
                name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_friends_normal, 0, 0, 0);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FAVORITES:
                name.setText(R.string.title_favorites);
                name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_favorites_normal, 0, 0, 0);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_PRIVATE:
                name.setText(R.string.title_hidden_entries);
                name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_hidden_normal, 0, 0, 0);
                break;
        }
    }

    void setupPageIndicator(View root) {
        CirclePageStaticIndicator cpsi = (CirclePageStaticIndicator) root.findViewById(R.id.circle_page_indicator);
        if (mPageCount == 0) {
            cpsi.setVisibility(View.GONE);
        } else {
            cpsi.setCount(mPageCount);
            cpsi.setSelected(mPageIdx);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFeedSubscription.unsubscribe();
        mUserSubscribtion.unsubscribe();
        mListView = null;
        mAdapter = null;
        mFeedDesignTarget = null;
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
        refreshUser();
        refreshFeed();
    }

    void onAvatarClicked(View v) {
        if (mCurrentUser == null) return;
        if (mListener != null) mListener.onAvatarClicked(mCurrentUser, mCurrentUser.getDesign());
    }

    void setupUser(CurrentUser user) {
        if (user == null) {
            // XXX
        } else {
            String name = user.getName();
            if (name == null) name = "";
            name = name.substring(0,1).toUpperCase(Locale.getDefault()) + name.substring(1);
            ((TextView)mHeaderView.findViewById(R.id.user_name)).setText(name);
            setupAvatar(user);
        }
    }

    private void setupAvatar(CurrentUser user) {
        ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                (ImageView)mHeaderView.findViewById(R.id.avatar),
                R.dimen.avatar_normal_diameter
        );
    }

    public void refreshUser() {
        if (!mUserSubscribtion.isUnsubscribed()) {
            mUserSubscribtion.unsubscribe();
            mStopRefreshingAction.call();
        }
        setRefreshing(true);
        Observable<CurrentUser> observableCurrentUser = AndroidObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mUserSubscribtion = observableCurrentUser
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
        Observable<Feed> observableFeed = getFeedObservable();
        mFeedSubscription = observableFeed
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction)
                .subscribe(mFeedObserver);
    }

    public Observable<Feed> getFeedObservable() {
        Observable<Feed> ob = Observable.empty();
        switch (mFeedType) {
            case MyAdditionalFeedActivity.FEED_TYPE_MAIN:
                ob = mMyFeedsService.getMyFeed(null, null);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FRIENDS:
                ob = mMyFeedsService.getMyFeed(null, null);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_FAVORITES:
                ob = mMyFeedsService.getMyFavoritesFeed(null, null);
                break;
            case MyAdditionalFeedActivity.FEED_TYPE_PRIVATE:
                ob = mMyFeedsService.getMyPrivateFeed(null, null);
                break;
        }
        return AndroidObservable.bindFragment(this, ob);
    }

    private Action0 mStopRefreshingAction = new Action0() {
        @Override
        public void call() {
            if (DBG) Log.v(TAG, "doOnTerminate()");
            setRefreshing(false);
        }
    };

    void setupFeedDesign(TlogDesign design) {
        mAdapter.setFeedDesign(design);
        mListView.setBackgroundDrawable(new ColorDrawable(design.getFeedBackgroundColor(getResources())));
        String backgroudUrl = design.getBackgroundUrl();
        int foregroundColor = design.getTitleForegroundColor(getResources());
        mFeedDesignTarget = new TargetSetHeaderBackground(mHeaderView.findViewById(R.id.header_user_feed_main),
                design, foregroundColor, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS);
        NetworkUtils.getInstance().getPicasso(getActivity())
                .load(backgroudUrl)
                .into(mFeedDesignTarget);
    }

    public final FeedItemAdapter.OnItemListener mOnFeedItemClickListener = new FeedItemAdapter.OnItemListener() {

        @Override
        public void onFeedItemClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            startActivity(i);
        }

        @Override
        public void onFeedLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onFeedLikesClicked post: " + entry);
            new LikesHelper(MyAdditionalFeedFragment.this, mAdapter) {
                @Override
                public void notifyError(String error, Throwable e) {
                    if (mListener != null) mListener.notifyError(error, e);
                }
            }.voteUnvote(entry);
        }

        @Override
        public void onFeedCommentsClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedCommentsClicked postId: " + postId);
            Intent i = new Intent(getActivity(), ShowPostActivity.class);
            i.putExtra(ShowPostActivity.ARG_POST_ID, postId);
            startActivity(i);
        }

        @Override
        public void onFeedAdditionalMenuClicked(View view, long postId) {
            if (DBG) Log.v(TAG, "onFeedAdditionalMenuClicked postId: " + postId);
            Toast.makeText(getActivity(), R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
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
            mListener.notifyError(getString(R.string.server_error), e);
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            if (mAdapter != null) mAdapter.setFeed(feed.entries);
        }
    };

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.server_error), e);
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
        public void onAvatarClicked(User user, TlogDesign design);
    }
}
