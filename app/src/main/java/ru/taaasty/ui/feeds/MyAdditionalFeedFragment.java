package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.NoSuchElementException;

import it.sephiroth.android.library.picasso.RequestCreator;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.FeedList;
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
import ru.taaasty.widgets.CirclePageStaticIndicator;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;


public class MyAdditionalFeedFragment extends Fragment implements IRereshable, SwipeRefreshLayout.OnRefreshListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedFragment";

    @Retention(RetentionPolicy.CLASS)
    @IntDef({FEED_TYPE_MAIN, FEED_TYPE_FRIENDS, FEED_TYPE_FAVORITES, FEED_TYPE_PRIVATE})
    public @interface FeedType {}
    public static final int FEED_TYPE_MAIN = 0;
    public static final int FEED_TYPE_FRIENDS = 1;
    public static final int FEED_TYPE_FAVORITES = 2;
    public static final int FEED_TYPE_PRIVATE = 3;

    private static final String BUNDLE_ARG_FEED_TYPE = "BUNDLE_ARG_FEED_TYPE";
    private static final String BUNDLE_ARG_PAGE_IDX = "BUNDLE_ARG_PAGE_IDX";
    private static final String BUNDLE_ARG_PAGE_COUNT = "BUNDLE_ARG_PAGE_COUNT";

    private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.MyAdditionalFeedFragment.feed_items";
    private static final String BUNDLE_KEY_CURRENT_USER = "ru.taaasty.ui.feeds.MyAdditionalFeedFragment.curremt_user";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private ApiMyFeeds mMyFeedsService;
    private Adapter mAdapter;
    private MyFeedLoader mFeedLoader;

    private Subscription mUserSubscribtion = SubscriptionHelper.empty();

    private int mFeedType = FEED_TYPE_FAVORITES;

    private int mRefreshCounter;

    private User mCurrentUser;

    private DateIndicatorWidget mDateIndicatorView;

    public static MyAdditionalFeedFragment newInstance(@FeedType int type,
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

        Bundle args = getArguments();
        //noinspection ResourceType
        mFeedType = args.getInt(BUNDLE_ARG_FEED_TYPE, FEED_TYPE_FAVORITES);
        if (savedInstanceState != null) {
            mCurrentUser = savedInstanceState.getParcelable(BUNDLE_KEY_CURRENT_USER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);

        mEmptyView = v.findViewById(R.id.empty_view);
        mRefreshLayout.setOnRefreshListener(this);

        boolean showUserAvatar = (mFeedType == FEED_TYPE_FRIENDS) || (mFeedType == FEED_TYPE_FAVORITES);

        FeedList feed = null;
        if (savedInstanceState != null) feed = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_ITEMS);
        mAdapter = new Adapter(getActivity(), feed, showUserAvatar);
        mAdapter.onCreate();

        if (mCurrentUser != null && mCurrentUser.getDesign() != null) mAdapter.setFeedDesign(mCurrentUser.getDesign());

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.setAdapter(mAdapter);

        setupEmptyView(v);

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mDateIndicatorView.setVisibility(View.VISIBLE);
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateDateIndicator(dy > 0);
            }
        });

        mAdapter.registerAdapterDataObserver(mUpdateIndicatorObserver);
        mFeedLoader = new MyFeedLoader(mAdapter);

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
            FeedList feed = mAdapter.getFeed();
            outState.putParcelable(BUNDLE_KEY_FEED_ITEMS, feed);
        }
        outState.putParcelable(BUNDLE_KEY_CURRENT_USER, mCurrentUser);
    }

    void setupEmptyView(View root) {
        int textNoRecords;
        switch (mFeedType) {
            case FEED_TYPE_MAIN:
                textNoRecords = R.string.you_have_not_written_anything;
                break;
            case FEED_TYPE_FRIENDS:
                textNoRecords = R.string.friends_have_not_written_anything;
                break;
            case FEED_TYPE_FAVORITES:
                textNoRecords = R.string.you_have_no_favorite_records;
                break;
            case FEED_TYPE_PRIVATE:
                textNoRecords = R.string.you_have_no_private_records;
                break;
            default:
                textNoRecords = R.string.no_records;
                break;
        }

        ((TextView)root.findViewById(R.id.empty_view)).setText(textNoRecords);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUserSubscribtion.unsubscribe();
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
        refreshUser();
        refreshFeed();
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
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<Feed> observableFeed = getFeedObservable(null, requestEntries)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(mStopRefreshingAction);
        mFeedLoader.refreshFeed(observableFeed, requestEntries);
        setRefreshing(true);
    }

    public Observable<Feed> getFeedObservable(@Nullable Long sinceEntryId, @Nullable Integer limit) {
        Observable<Feed> ob = Observable.empty();
        switch (mFeedType) {
            case FEED_TYPE_MAIN:
                ob = mMyFeedsService.getMyFeed(sinceEntryId, limit);
                break;
            case FEED_TYPE_FRIENDS:
                ob = mMyFeedsService.getMyFriendsFeed(sinceEntryId, limit);
                break;
            case FEED_TYPE_FAVORITES:
                ob = mMyFeedsService.getMyFavoritesFeed(sinceEntryId, limit);
                break;
            case FEED_TYPE_PRIVATE:
                ob = mMyFeedsService.getMyPrivateFeed(sinceEntryId, limit);
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
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    public class Adapter extends FeedItemAdapter {
        private String mTitle;
        private final int mFeedName;
        private final int mFeedNameVisibility;
        private final int mFeedNameLeftDrawable;

        private final int mPageIdx;
        private final int mPageCount;

        private User mUser = User.DUMMY;

        public Adapter(Context context, FeedList feed, boolean showUserAvatar) {
            super(context, feed, showUserAvatar);

            Bundle args = getArguments();
            //noinspection ResourceType
            mPageIdx = args.getInt(BUNDLE_ARG_PAGE_IDX, 0);
            mPageCount = args.getInt(BUNDLE_ARG_PAGE_COUNT, 3);

            switch (mFeedType) {
                case FEED_TYPE_MAIN:
                    mFeedName = -1;
                    mFeedNameVisibility = View.GONE;
                    mFeedNameLeftDrawable = -1;
                    break;
                case FEED_TYPE_FRIENDS:
                    mFeedName = R.string.friends;
                    mFeedNameVisibility = View.VISIBLE;
                    mFeedNameLeftDrawable = R.drawable.ic_friends;
                    break;
                case FEED_TYPE_FAVORITES:
                    mFeedName = R.string.title_favorites;
                    mFeedNameVisibility = View.VISIBLE;
                    mFeedNameLeftDrawable = R.drawable.ic_favorites_small_normal;
                    break;
                case FEED_TYPE_PRIVATE:
                    mFeedName = R.string.title_hidden_entries;
                    mFeedNameVisibility = View.VISIBLE;
                    mFeedNameLeftDrawable = R.drawable.ic_hidden_small_normal;
                    break;
                default:
                    throw  new IllegalArgumentException();
            }
        }

        @Override
        protected boolean initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
            if (!(pHolder instanceof ListEntryBase)) return true;
            pHolder.itemView.setOnClickListener(mOnItemClickListener);

            ListEntryBase holder = (ListEntryBase)pHolder;
            holder.getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
            if (mShowUserAvatar) {
                holder.getAvatarAuthorView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Entry entry = mAdapter.getAnyEntryAtHolderPosition(pHolder);
                        if (mListener != null && entry != null) mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
                    }
                });
            }
            return true;
        }


        final View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecyclerView.ViewHolder vh = mListView.getChildViewHolder(v);
                Entry entry = getAnyEntryAtHolderPosition(vh);
                if (entry != null) onFeedItemClicked(v, entry);
            }
        };

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_user_feed, mListView, false);
            HeaderHolder holder = new HeaderHolder(child);
            holder.avatarView.setOnClickListener(mOnClickListener);
            bindTitleName(holder);
            bindPageIndicator(holder);
            return holder;
        }

        @Override
        protected void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
            HeaderHolder holder = (HeaderHolder)viewHolder;
            holder.usernameView.setText(mTitle);
            bindDesign(holder);
            bindUser(holder);
        }

        public void setTitleUser(String title, User user) {
            mTitle = title;
            mUser = user;
            notifyItemChanged(0);
        }

        void bindTitleName(HeaderHolder holder) {
            TextView feedNameView = (TextView)holder.itemView.findViewById(R.id.feed_name);
            feedNameView.setVisibility(mFeedNameVisibility);
            if (mFeedNameVisibility != View.GONE) {
                feedNameView.setText(mFeedName);
                feedNameView.setCompoundDrawablesWithIntrinsicBounds(mFeedNameLeftDrawable, 0, 0, 0);
            }
        }

        private void bindDesign(HeaderHolder holder) {
            if (mFeedDesign == null) return;
            String backgroudUrl = mFeedDesign.getBackgroundUrl();
            if (TextUtils.equals(holder.backgroundUrl, backgroudUrl)) return;
            holder.feedDesignTarget = new TargetSetHeaderBackground(holder.headerUserFeedMain,
                    mFeedDesign, Color.TRANSPARENT, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS);
            holder.backgroundUrl = backgroudUrl;
            RequestCreator rq = NetworkUtils.getInstance().getPicasso(holder.itemView.getContext())
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

        void bindPageIndicator(HeaderHolder holder) {
            CirclePageStaticIndicator pageIndicator = (CirclePageStaticIndicator) holder.itemView.findViewById(R.id.circle_page_indicator);
            if (mPageCount == 0) {
                pageIndicator.setVisibility(View.GONE);
            } else {
                pageIndicator.setCount(mPageCount);
                pageIndicator.setSelected(mPageIdx);
            }
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.avatar:
                        if (mCurrentUser == null) return;
                        if (mListener != null) mListener.onCurrentUserAvatarClicked(v, mCurrentUser, mCurrentUser.getDesign());
                        break;
                }
            }
        };
    }

    static class HeaderHolder extends ParallaxedHeaderHolder {
        View headerUserFeedMain;
        TextView usernameView;
        ImageView avatarView;

        public String backgroundUrl = null;

        // XXX: anti picasso weak ref
        private TargetSetHeaderBackground feedDesignTarget;

        public HeaderHolder(View itemView) {
            super(itemView);
            headerUserFeedMain = itemView.findViewById(R.id.header_user_feed_main);
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            usernameView = (TextView)itemView.findViewById(R.id.user_name);
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
            super(MyAdditionalFeedFragment.this);
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

    public void onFeedItemClicked(View view, Entry entry) {
        if (DBG) Log.v(TAG, "onFeedItemClicked postId: " + entry);
        new ShowPostActivity.Builder(getActivity())
                .setEntry(entry)
                .setSrcView(view)
                .startActivity();
    }

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {


        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            throw new IllegalStateException();
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked post: " + entry);
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

    class MyFeedLoader extends ru.taaasty.ui.feeds.FeedLoader {

        public MyFeedLoader(FeedItemAdapter adapter) {
            super(adapter);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return getFeedObservable(sinceEntryId, limit);
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
        /**
         * Юзер ткнул на аватарку в списке
         * @param view
         * @param user
         * @param design
         */
        public void onAvatarClicked(View view, User user, TlogDesign design);

        /**
         * Юзер ткнут на свою аватарку в заголовке списка
         * @param view
         * @param user
         * @param design
         */
        public void onCurrentUserAvatarClicked(View view, User user, TlogDesign design);

        public void onSharePostMenuClicked(Entry entry);
    }
}
