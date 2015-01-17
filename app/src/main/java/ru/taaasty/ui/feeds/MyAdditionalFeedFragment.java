package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
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

    private boolean mForceShowRefreshingIndicator;

    private User mCurrentUser;

    private DateIndicatorWidget mDateIndicatorView;

    public static MyAdditionalFeedFragment newInstance(@FeedType int type) {
        MyAdditionalFeedFragment usf = new MyAdditionalFeedFragment();
        Bundle b = new Bundle();
        b.putInt(BUNDLE_ARG_FEED_TYPE, type);
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
        mForceShowRefreshingIndicator = false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);

        mEmptyView = v.findViewById(R.id.empty_view);
        mRefreshLayout.setOnRefreshListener(this);

        boolean showUserAvatar = (mFeedType == FEED_TYPE_FRIENDS) || (mFeedType == FEED_TYPE_FAVORITES);

        List<Entry> feed = null;
        if (savedInstanceState != null) feed = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS);
        mAdapter = new Adapter(getActivity(), feed, showUserAvatar);
        mAdapter.onCreate();

        if (mCurrentUser != null && mCurrentUser.getDesign() != null) mAdapter.setFeedDesign(mCurrentUser.getDesign());

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), showUserAvatar));
        mListView.setAdapter(mAdapter);


        setupEmptyView(v);

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
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
        if (!isLoading()) refreshData(false);
        updateDateIndicator(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            List<Entry> feed = mAdapter.getFeed().getItems();
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS, new ArrayList<Parcelable>(feed));
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
        return mFeedLoader.isRefreshing() || !mUserSubscribtion.isUnsubscribed();
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
        if (showIndicator) mForceShowRefreshingIndicator = true;
        refreshUser();
        refreshFeed();
    }

    void setupUser(CurrentUser user) {
        if (user == null) {
            // XXX
        } else {
            String name = UiUtils.capitalize(user.getName());
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
        Observable<CurrentUser> observableCurrentUser = AndroidObservable.bindFragment(this,
                UserManager.getInstance().getCurrentUser());

        mUserSubscribtion = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction)
                .subscribe(mCurrentUserObserver);
        setupRefreshingIndicator();
    }

    private void refreshFeed() {
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<Feed> observableFeed = getFeedObservable(null, requestEntries)
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(mStopRefreshingAction);
        mFeedLoader.refreshFeed(observableFeed, requestEntries);
        setupRefreshingIndicator();
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
            setupRefreshingIndicator();
        }
    };

    void setupFeedDesign(TlogDesign design) {
        mAdapter.setFeedDesign(design);
        mListView.setBackgroundDrawable(new ColorDrawable(design.getFeedBackgroundColor(getResources())));
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
    }

    public class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private final int mFeedName;
        private final int mFeedNameVisibility;
        private final int mFeedNameLeftDrawable;

        private User mUser = User.DUMMY;

        public Adapter(Context context, List<Entry> feed, boolean showUserAvatar) {
            super(context, feed, showUserAvatar);

            Bundle args = getArguments();
            //noinspection ResourceType

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

            // Все посты
            if (pHolder instanceof ListEntryBase) {
                ((ListEntryBase)pHolder).getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
                if (mShowUserAvatar) {
                    ((ListEntryBase)pHolder).getAvatarAuthorView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Entry entry = mAdapter.getAnyEntryAtHolderPosition(pHolder);
                            if (mListener != null && entry != null) mListener.onAvatarClicked(v, entry.getAuthor(), entry.getAuthor().getDesign());
                        }
                    });
                }
                // Клики на картинках
                FeedsHelper.setupListEntryClickListener(this, (ListEntryBase)pHolder);
                return true;
            }

            return false;
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
            View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_user_feed, mListView, false);
            HeaderHolder holder = new HeaderHolder(child);
            holder.avatarView.setOnClickListener(mOnClickListener);
            bindTitleName(holder);
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
                    mFeedDesign, Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES, Constants.FEED_TITLE_BACKGROUND_BLUR_RADIUS) {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    super.onBitmapLoaded(bitmap, from);
                    ImageUtils.getInstance().putBitmapToCache(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY, bitmap);
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
                    case R.id.avatar:
                        if (mCurrentUser == null) return;
                        if (mListener != null) mListener.onCurrentUserAvatarClicked(v, mCurrentUser, mCurrentUser.getDesign());
                        break;
                }
            }
        };

        @Override
        public void onEventMainThread(EntryChanged event) {
            if(!event.postEntry.isFavorited() && (mFeedType == FEED_TYPE_FAVORITES)) {
                mAdapter.removeEntry(event.postEntry.getId());
            }
            else
                mAdapter.addEntry(event.postEntry);
        }
    }

    static class HeaderHolder extends ParallaxedHeaderHolder {
        View headerUserFeedMain;
        TextView usernameView;
        ImageView avatarView;

        public String backgroundUrl = null;

        // XXX: anti picasso weak ref
        private TargetSetHeaderBackground feedDesignTarget;

        public HeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.header_user_feed_main));
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
            if (DBG) Log.v(TAG, "onChanged");
            updateIndicatorDelayed();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            if (DBG) Log.v(TAG, "onItemRangeInserted");
            updateIndicatorDelayed();
        }

        private void updateIndicatorDelayed() {
            if (mListView != null) {
                mListView.removeCallbacks(mUpdateIndicatorRunnable);
                mListView.postDelayed(mUpdateIndicatorRunnable, 64);
            }
        }
    };

    public void onFeedItemClicked(View view, Entry entry) {

    }

    public final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {


        @Override
        public void onPostUserInfoClicked(View view, Entry entry) {
            throw new IllegalStateException();
        }

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked post: " + entry);
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

    class MyFeedLoader extends ru.taaasty.ui.feeds.FeedLoaderLite {

        public MyFeedLoader(FeedItemAdapterLite adapter) {
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
