package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
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

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.FeedAdapter;
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
import ru.taaasty.ui.FragmentWithWorkFragment;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.FabHelper;
import ru.taaasty.utils.FeedBackground;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;

/**
 * Мои записи
 */
public class MyFeedFragment extends FragmentWithWorkFragment<FeedWorkFragment> implements IRereshable,
        FeedWorkFragment.TargetFragmentInteraction {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedFragment";

    public static final int REQUEST_CODE_LOGIN = 1;

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private Adapter mAdapter;

    private DateIndicatorWidget mDateIndicatorView;

    private WorkRetainedFragment mWorkFragment;

    private Handler mHandler;

    private FeedsHelper.DateIndicatorUpdateHelper mDateIndicatorHelper;

    private FeedBackground mFeedBackground;

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
        mHandler = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_my_feed, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false);
            }
        });

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        //mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), false));

        mDateIndicatorView = (DateIndicatorWidget) v.findViewById(R.id.date_indicator);

        mFeedBackground = new FeedBackground(mListView, null, R.dimen.feed_header_height);

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

    @Nullable
    @Override
    public FeedWorkFragment getWorkFragment() {
        return mWorkFragment;
    }

    @Override
    public void initWorkFragment() {
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("MyFeedWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "MyFeedWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }

    @Override
    public void onWorkFragmentActivityCreatedSafe() {
        mAdapter = new Adapter(mWorkFragment.getEntryList());
        mAdapter.onCreate();
        mListView.setAdapter(mAdapter);
        mListView.addOnScrollListener(new FabHelper.AutoHideScrollListener(mListener.getTabbar().getFab()));

        mDateIndicatorHelper = new FeedsHelper.DateIndicatorUpdateHelper(mListView, mDateIndicatorView, mAdapter);
        mAdapter.registerAdapterDataObserver(mDateIndicatorHelper.adapterDataObserver);
        mListView.addOnScrollListener(mDateIndicatorHelper.onScrollListener);

        mListView.addOnScrollListener(new FeedsHelper.WatchHeaderScrollListener() {
            @Override
            void onScrolled(RecyclerView recyclerView, int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
                mFeedBackground.setHeaderVisibleFraction(firstVisibleItem == 0 ? firstVisibleFract : 0);
            }
        });

        setupFeedDesign();
        setupUser();
        setupAdapterPendingIndicator();
        onLoadingStateChanged("onWorkFragmentActivityCreated()");
    }

    @Override
    public void onWorkFragmentResumeSafe() {
        if (!mWorkFragment.isRefreshing()) refreshData(false);
        mDateIndicatorHelper.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDateIndicatorView = null;
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

    void onAdditionalMenuButtonClicked(View v) {
        if (mListener != null) mListener.onShowAdditionalMenuClicked();
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
        if (mWorkFragment.getEntryList().isEmpty())
            mDateIndicatorView.setVisibility(View.INVISIBLE);
    }

    private final Runnable mRefreshLoadingState = new Runnable() {
        @Override
        public void run() {
            if (mListView == null) return;
            setupLoadingState();
            setupAdapterPendingIndicator();
        }
    };

    class Adapter extends FeedAdapter {
        private String mTitle;

        public Adapter(SortedList<Entry> feed) {
            super(feed, false);
            setFeedId(String.valueOf(Session.getInstance().getCurrentUserId()));
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    if (mWorkFragment != null) mWorkFragment.onBindViewHolder(position);
                }

                @Override
                public void initClickListeners(RecyclerView.ViewHolder holder, int type) {
                    // Все посты
                    if (holder instanceof ListEntryBase) {
                        ((ListEntryBase) holder).setEntryClickListener(mOnFeedItemClickListener);
                        // Клики на картинках
                        FeedsHelper.setupListEntryClickListener(Adapter.this, (ListEntryBase) holder);
                    }
                }

                @Override
                public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                    if (DBG) Log.v(TAG, "onCreateHeaderViewHolder");
                    View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_my_feed, mListView, false);
                    HeaderHolder holder = new HeaderHolder(child);
                    holder.avatarView.setOnClickListener(mOnClickListener);
                    child.findViewById(R.id.additional_menu).setOnClickListener(mOnClickListener);
                    return holder;
                }

                @Override
                public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                    if (DBG) Log.v(TAG, "onBindHeaderViewHolder");
                    HeaderHolder holder = (HeaderHolder) viewHolder;
                    holder.titleView.setText(mTitle);
                    bindUser(holder);
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    addEntry(event.postEntry);
                }

                @Override
                public void onVoteError(long entryId, int errResId, Throwable error) {
                    LikesHelper.showCannotVoteError(getView(), MyFeedFragment.this, REQUEST_CODE_LOGIN,
                            errResId, error);
                }
            });
        }

        public void setTitleUser(String title, User user) {
            if (!TextUtils.equals(mTitle, title)) {
                mTitle = title;
                notifyItemChanged(0);
            }
        }

        private void bindUser(HeaderHolder holder) {
            User user = mWorkFragment.getCurrentUser();
            if (user == null) user = CurrentUser.DUMMY;
            ImageUtils.getInstance()
                    .loadAvatarToImageView(user, R.dimen.feed_header_avatar_normal_diameter, holder.avatarView);
        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.additional_menu:
                        onAdditionalMenuButtonClicked(v);
                        break;
                    case R.id.avatar:
                        CurrentUser user = mWorkFragment.getCurrentUser();
                        if (mListener != null)
                            mListener.onCurrentUserAvatarClicked(v, mWorkFragment.getCurrentUser(),
                                    mWorkFragment.getTlogDesign());
                        break;
                }
            }
        };
    }

    static class HeaderHolder extends ParallaxedHeaderHolder {
        TextView titleView;
        ImageView avatarView;

        public HeaderHolder(View itemView) {
            super(itemView, itemView.findViewById(R.id.avatar_user_name));
            avatarView = (ImageView) itemView.findViewById(R.id.avatar);
            titleView = (TextView) itemView.findViewById(R.id.user_name);
        }
    }

    final ListEntryBase.OnEntryClickListener mOnFeedItemClickListener = new ListEntryBase.OnEntryClickListener() {

        @Override
        public void onPostLikesClicked(ListEntryBase holder, View view, boolean canVote) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            if (canVote) {
                LikesHelper.getInstance().voteUnvote(entry, getActivity());
            } else {
                LikesHelper.showCannotVoteError(getView(), MyFeedFragment.this, REQUEST_CODE_LOGIN);
            }
        }

        @Override
        public void onPostCommentsClicked(ListEntryBase holder, View view) {
            Entry entry = mAdapter.getAnyEntryAtHolderPosition(holder);
            if (entry == null) return;
            if (DBG) Log.v(TAG, "onPostCommentsClicked postId: " + entry.getId());
            TlogDesign design = entry.getDesign();
            if (design == null && mWorkFragment.getTlogDesign() != null)
                design = mWorkFragment.getTlogDesign();
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

        @Override
        protected String getKeysSuffix() {
            return "MyFeedWorkFragment";
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return RestClient.getAPiMyFeeds().getMyFeed(sinceEntryId, limit);
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
        void onShowAdditionalMenuClicked();

        void onCurrentUserAvatarClicked(View view, User user, TlogDesign design);

        void onSharePostMenuClicked(Entry entry);

        TabbarFragment getTabbar();
    }
}
