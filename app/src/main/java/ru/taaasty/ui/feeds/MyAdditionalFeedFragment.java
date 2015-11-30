package ru.taaasty.ui.feeds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.adapters.ParallaxedHeaderHolder;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.utils.FeedBackground;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;


public class MyAdditionalFeedFragment extends Fragment implements IRereshable,
        FeedWorkFragment.TargetFragmentInteraction {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedFragment";

    @Retention(RetentionPolicy.CLASS)
    @IntDef({FEED_TYPE_MAIN, FEED_TYPE_FRIENDS, FEED_TYPE_FAVORITES, FEED_TYPE_PRIVATE})
    public @interface FeedType {}

    /**
     * Основная моя лента
     */
    public static final int FEED_TYPE_MAIN = 0;

    /**
     * Лента моих друзей
     */
    public static final int FEED_TYPE_FRIENDS = 1;

    /**
     * Мои избранные записи
     */
    public static final int FEED_TYPE_FAVORITES = 2;

    /**
     * Мои скрытые записи
     */
    public static final int FEED_TYPE_PRIVATE = 3;

    private static final String BUNDLE_ARG_FEED_TYPE = "BUNDLE_ARG_FEED_TYPE";

    private static final int REQUEST_CODE_LOGIN = 1;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private Adapter mAdapter;

    @FeedType
    private int mFeedType = FEED_TYPE_FAVORITES;

    private DateIndicatorWidget mDateIndicatorView;

    private WorkRetainedFragment mWorkFragment;

    private Handler mHandler;

    private FeedsHelper.DateIndicatorUpdateHelper mDateIndicatorHelper;

    private FeedBackground mFeedBackground;

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

        Bundle args = getArguments();
        //noinspection ResourceType
        mFeedType = args.getInt(BUNDLE_ARG_FEED_TYPE, FEED_TYPE_FAVORITES);
        mHandler = new Handler();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);
        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);
        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);


        mFeedBackground = new FeedBackground(mListView, null, R.dimen.feed_header_height);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false);
            }
        });

        //mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), isUserAvatarShown()));

        setupEmptyView(v);


        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("SubscriptionListWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            mWorkFragment.setTargetFragment(this, 0);
            mWorkFragment.setArguments(new Bundle(getArguments()));
            fm.beginTransaction().add(mWorkFragment, "SubscriptionListWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }


    @Override
    public void onWorkFragmentActivityCreated() {
        mAdapter = new Adapter(mWorkFragment.getEntryList(), isUserAvatarShown());
        mAdapter.onCreate();
        mListView.setAdapter(mAdapter);

        mDateIndicatorHelper = new FeedsHelper.DateIndicatorUpdateHelper(mListView, mDateIndicatorView, mAdapter);
        mAdapter.registerAdapterDataObserver(mDateIndicatorHelper.adapterDataObserver);
        mListView.addOnScrollListener(mDateIndicatorHelper.onScrollListener);
        mListView.addOnScrollListener(new FeedsHelper.WatchHeaderScrollListener() {
            @Override
            void onScrolled(RecyclerView recyclerView, int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
                mFeedBackground.setHeaderVisibleFraction(firstVisibleItem == 0 ? firstVisibleFract : 0);
            }
        });
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mListener != null && mListener.getFragmentScrollListener() != null) {
                    mListener.getFragmentScrollListener().onScrollStateChanged(recyclerView, newState);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mListener != null && mListener.getFragmentScrollListener() != null) {
                    mListener.getFragmentScrollListener().onScrolled(recyclerView, dx, dy);
                }
            }
        });

        setupFeedDesign();
        setupUser();
        setupAdapterPendingIndicator();
        onLoadingStateChanged("onWorkFragmentActivityCreated()");
    }

    @Override
    public void onWorkFragmentResume() {
        if (!mWorkFragment.isRefreshing()) {
            refreshData(false);
        }
        mDateIndicatorHelper.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWorkFragment.setTargetFragment(null, 0);
        mDateIndicatorView = null;
        if (mDateIndicatorHelper != null) {
            mDateIndicatorHelper.onDestroy();
            mDateIndicatorHelper = null;
        }
        if (mAdapter != null) {
            mAdapter.onDestroy(mListView);
            mAdapter = null;
        }
        mListView = null;
        mFeedBackground = null;
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
        setupFeedDesign();
    }

    @Override
    public void onCurrentUserChanged() {
        setupUser();
    }

    @Override
    public RecyclerView.Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void refreshData(boolean forceShowRefreshingIndicator) {
        if (!mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(mWorkFragment.getEntryList().isEmpty() || forceShowRefreshingIndicator);
        }
        mWorkFragment.refreshData();
    }

    private boolean isUserAvatarShown() {
        return (mFeedType == FEED_TYPE_FRIENDS) || (mFeedType == FEED_TYPE_FAVORITES);
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

        TextView emptyView = (TextView)root.findViewById(R.id.empty_view);
        emptyView.setText(textNoRecords);

        int paddingTop = getResources().getDimensionPixelSize(R.dimen.feed_header_height);
        if (isFeedNameVisible(mFeedType)) {
            paddingTop += getResources().getDimensionPixelSize(R.dimen.feed_header_name_height);
        }

        emptyView.setPadding(emptyView.getPaddingLeft(),
                paddingTop, emptyView.getPaddingRight(), emptyView.getCompoundPaddingBottom());
    }

    void setupUser() {
        if (mWorkFragment == null || mWorkFragment.getCurrentUser() == null) return;
        CurrentUser user = mWorkFragment.getCurrentUser();
        String name = UiUtils.capitalize(user.getName());
        if (mAdapter != null) {
            mAdapter.setTitleUser(name, user);
        }
    }

    void setupFeedDesign() {
        if (mWorkFragment == null || mWorkFragment.getTlogDesign() == null) return;
        if (DBG) Log.e(TAG, "Setup feed design " + mWorkFragment.getTlogDesign());
        mAdapter.setFeedDesign(mWorkFragment.getTlogDesign());
        mFeedBackground.setTlogDesign(mWorkFragment.getTlogDesign());
    }

    private void setupAdapterPendingIndicator() {
        if (mAdapter == null) return;
        boolean pendingIndicatorShown = mWorkFragment != null
                && mWorkFragment.isPendingIndicatorShown();

        if (DBG) Log.v(TAG, "setupAdapterPendingIndicator() shown: " + pendingIndicatorShown);

        mAdapter.setLoading(pendingIndicatorShown);
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
        if (mWorkFragment != null && !mWorkFragment.isLoading()) {
            mRefreshLayout.setRefreshing(false);
        }

        boolean listIsEmpty = mAdapter != null
                && mWorkFragment != null
                && mWorkFragment.isFeedEmpty();

        mEmptyView.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        if (mWorkFragment.getEntryList().isEmpty()) mDateIndicatorView.setVisibility(View.INVISIBLE);
    }

    private final Runnable mRefreshLoadingState = new Runnable() {
        @Override
        public void run() {
            if (mListView == null) return;
            setupLoadingState();
            setupAdapterPendingIndicator();
        }
    };

    private static boolean isFeedNameVisible(@FeedType int feedType) {
        return feedType != FEED_TYPE_MAIN;
    }

    public class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private final int mFeedName;
        private final int mFeedNameLeftDrawable;

        public Adapter(SortedList<Entry> feed, boolean showUserAvatar) {
            super(feed, showUserAvatar);

            Bundle args = getArguments();
            //noinspection ResourceType

            setFeedId(null); // XXX показываем как ов общем фиде, без репостов

            switch (mFeedType) {
                case FEED_TYPE_MAIN:
                    mFeedName = -1;
                    mFeedNameLeftDrawable = -1;
                    break;
                case FEED_TYPE_FRIENDS:
                    mFeedName = R.string.friends;
                    mFeedNameLeftDrawable = R.drawable.ic_friends;
                    break;
                case FEED_TYPE_FAVORITES:
                    mFeedName = R.string.title_favorites;
                    mFeedNameLeftDrawable = R.drawable.ic_favorites_small_normal;
                    break;
                case FEED_TYPE_PRIVATE:
                    mFeedName = R.string.title_hidden_entries;
                    mFeedNameLeftDrawable = R.drawable.ic_hidden_small_normal;
                    break;
                default:
                    throw  new IllegalArgumentException();
            }
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    if (mWorkFragment != null) mWorkFragment.onBindViewHolder(position);
                }

                @Override
                public void initClickListeners(final RecyclerView.ViewHolder pHolder, int pViewType) {
                    // Все посты
                    if (pHolder instanceof ListEntryBase) {
                        ((ListEntryBase) pHolder).setEntryClickListener(mOnFeedItemClickListener);
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
                        // Клики на картинках
                        FeedsHelper.setupListEntryClickListener(Adapter.this, (ListEntryBase) pHolder);
                    }
                }

                @Override
                public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                    View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_user_feed, mListView, false);
                    HeaderHolder holder = new HeaderHolder(child);
                    holder.avatarView.setOnClickListener(mOnClickListener);
                    bindTitleName(holder);
                    return holder;
                }

                @Override
                public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                    HeaderHolder holder = (HeaderHolder) viewHolder;
                    holder.usernameView.setText(mTitle);
                    bindUser(holder);
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    if(!event.postEntry.isFavorited() && (mFeedType == FEED_TYPE_FAVORITES)) {
                        mAdapter.removeEntry(event.postEntry);
                    }
                    else
                        mAdapter.addEntry(event.postEntry);
                }

                @Override
                public void onVoteError(long entryId, int errResId, Throwable error) {
                    LikesHelper.showCannotVoteError(getView(), MyAdditionalFeedFragment.this, REQUEST_CODE_LOGIN,
                            errResId, error);
                }

                private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (v.getId()) {
                            case R.id.avatar:
                                if (mWorkFragment.getCurrentUser() == null) return;
                                if (mListener != null) mListener.onCurrentUserAvatarClicked(v,
                                        mWorkFragment.getCurrentUser(), mWorkFragment.getTlogDesign());
                                break;
                        }
                    }
                };
            });
        }

        public void setTitleUser(String title, User user) {
            if (!TextUtils.equals(mTitle, title)) {
                mTitle = title;
                notifyItemChanged(0);
            }
        }

        @SuppressLint("WrongConstant")
        void bindTitleName(HeaderHolder holder) {
            TextView feedNameView = (TextView)holder.itemView.findViewById(R.id.feed_name);
            feedNameView.setVisibility(isFeedNameVisible(mFeedType) ? View.VISIBLE : View.GONE);
            if (isFeedNameVisible(mFeedType)) {
                feedNameView.setText(mFeedName);
                feedNameView.setCompoundDrawablesWithIntrinsicBounds(mFeedNameLeftDrawable, 0, 0, 0);
            }
        }

        private void bindUser(HeaderHolder holder) {
            User user = mWorkFragment.getCurrentUser();
            if (user == null) user = CurrentUser.DUMMY;
            ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                    holder.avatarView,
                    R.dimen.feed_header_avatar_normal_diameter
            );
        }
    }

    static class HeaderHolder extends ParallaxedHeaderHolder {
        View headerUserFeedMain;
        TextView usernameView;
        ImageView avatarView;

        public HeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.header_user_feed_main));
            headerUserFeedMain = itemView.findViewById(R.id.header_user_feed_main);
            avatarView = (ImageView)itemView.findViewById(R.id.avatar);
            usernameView = (TextView)itemView.findViewById(R.id.user_name);
        }
    }

    public final ListEntryBase.OnEntryClickListener mOnFeedItemClickListener = new ListEntryBase.OnEntryClickListener() {

        @Override
        public void onPostLikesClicked(ListEntryBase holder, View view, boolean canVote) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            if (DBG) Log.v(TAG, "onPostLikesClicked post: " + entry);
            if (canVote) {
                LikesHelper.getInstance().voteUnvote(entry);
            } else {
                Toast.makeText(view.getContext(), R.string.user_can_not_post, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onPostCommentsClicked(ListEntryBase holder,View view) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            TlogDesign design = entry.getDesign();
            if (design == null && mWorkFragment.getTlogDesign() != null) design = mWorkFragment.getTlogDesign();
            new ShowPostActivity.Builder(getActivity())
                    .setEntry(entry)
                    .setSrcView(view)
                    .setDesign(design)
                    .startActivity();
        }

        @Override
        public void onPostAdditionalMenuClicked(ListEntryBase holder, View view) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
        }

        @Override
        public void onPostFlowHeaderClicked(ListEntryBase holder, View view) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            TlogActivity.startTlogActivity(getActivity(), entry.getTlog().id, view);
        }
    };

    public static class WorkRetainedFragment extends FeedWorkFragment {

        private int mFeedType;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            mFeedType = getArguments().getInt(BUNDLE_ARG_FEED_TYPE);
            super.onCreate(savedInstanceState);
        }

        @Override
        protected String getKeysSuffix() {
            return "MyAdditionalWorkFragment"+mFeedType;
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            switch (mFeedType) {
                case FEED_TYPE_MAIN:
                    return RestClient.getAPiMyFeeds().getMyFeed(sinceEntryId, limit);
                case FEED_TYPE_FRIENDS:
                    return RestClient.getAPiMyFeeds().getMyFriendsFeed(sinceEntryId, limit);
                case FEED_TYPE_FAVORITES:
                    return RestClient.getAPiMyFeeds().getMyFavoritesFeed(sinceEntryId, limit);
                case FEED_TYPE_PRIVATE:
                    return RestClient.getAPiMyFeeds().getMyPrivateFeed(sinceEntryId, limit);
                default:
                    throw new IllegalStateException();
            }
        }

        @Override
        protected boolean isUserRefreshEnabled() {
            return true;
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
    public interface OnFragmentInteractionListener {
        /**
         * Юзер ткнул на аватарку в списке
         * @param view
         * @param user
         * @param design
         */
        void onAvatarClicked(View view, User user, TlogDesign design);

        /**
         * Юзер ткнут на свою аватарку в заголовке списка
         * @param view
         * @param user
         * @param design
         */
        void onCurrentUserAvatarClicked(View view, User user, TlogDesign design);

        void onSharePostMenuClicked(Entry entry);

        RecyclerView.OnScrollListener getFragmentScrollListener();
    }
}
