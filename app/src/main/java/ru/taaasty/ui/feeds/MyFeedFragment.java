package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
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

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
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
import ru.taaasty.rest.service.ApiMyFeeds;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerFeedListInterPost;
import ru.taaasty.ui.post.ShowPostActivity;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;

public class MyFeedFragment extends Fragment implements IRereshable,
        ListFeedWorkRetainedFragment.TargetFragmentInteraction {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedFragment";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private Adapter mAdapter;

    private DateIndicatorWidget mDateIndicatorView;

    private WorkRetainedFragment mWorkFragment;

    private Handler mHandler;

    private FeedsHelper.DateIndicatorUpdateHelper mDateIndicatorHelper;

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
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.getItemAnimator().setSupportsChangeAnimations(false);
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), false));

        mDateIndicatorView = (DateIndicatorWidget)v.findViewById(R.id.date_indicator);

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
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    public void onWorkFragmentActivityCreated() {
        mAdapter = new Adapter(mWorkFragment.getEntryList());
        mAdapter.onCreate();
        mListView.setAdapter(mAdapter);
        mListView.addOnScrollListener(new TabbarFragment.AutoHideScrollListener(mListener.getTabbar()));

        mDateIndicatorHelper = new FeedsHelper.DateIndicatorUpdateHelper(mListView, mDateIndicatorView, mAdapter);
        mAdapter.registerAdapterDataObserver(mDateIndicatorHelper.adapterDataObserver);
        mListView.addOnScrollListener(mDateIndicatorHelper.onScrollListener);

        setupFeedDesign();
        setupUser();
        setupAdapterPendingIndicator();
        onLoadingStateChanged("onWorkFragmentActivityCreated()");
    }

    @Override
    public void onWorkFragmentResume() {
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
        mListView.setBackgroundResource(mWorkFragment.getTlogDesign().getFeedBackgroundDrawable());
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
        boolean isRefreshing = mWorkFragment == null || mWorkFragment.isRefreshing();
        if (!isRefreshing) mRefreshLayout.setRefreshing(false);

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

    class Adapter extends FeedItemAdapterLite {
        private String mTitle;

        public Adapter(SortedList<Entry> feed) {
            super(feed, false);
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
                    if (mWorkFragment != null) mWorkFragment.onBindViewHolder(position);
                }

                @Override
                public void initClickListeners(RecyclerView.ViewHolder holder, int type) {
                    // Все посты
                    if (holder instanceof ListEntryBase) {
                        ((ListEntryBase) holder).getEntryActionBar().setOnItemClickListener(mOnFeedItemClickListener);
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
                    bindDesign(holder);
                    bindUser(holder);
                }

                @Override
                public void onEntryChanged(EntryChanged event) {
                    addEntry(event.postEntry);
                }
            });
        }

        public void setTitleUser(String title, User user) {
            if (!TextUtils.equals(mTitle, title)) {
                mTitle = title;
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
            User user = mWorkFragment.getCurrentUser();
            if (user == null) user = CurrentUser.DUMMY;
            ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                    holder.avatarView,
                    R.dimen.feed_header_avatar_normal_diameter
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
                        CurrentUser user = mWorkFragment.getCurrentUser();
                        if (mListener != null) mListener.onCurrentUserAvatarClicked(v, mWorkFragment.getCurrentUser(),
                                mWorkFragment.getTlogDesign());
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

    final EntryBottomActionBar.OnEntryActionBarListener mOnFeedItemClickListener = new EntryBottomActionBar.OnEntryActionBarListener() {

        @Override
        public void onPostLikesClicked(View view, Entry entry) {
            if (DBG) Log.v(TAG, "onPostLikesClicked entry: " + entry);
            LikesHelper.getInstance().voteUnvote(entry);
        }

        @Override
        public void onPostCommentsClicked(View view, Entry entry) {
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
        public void onPostAdditionalMenuClicked(View view, Entry entry) {
            if (mListener != null) mListener.onSharePostMenuClicked(entry);
        }
    };

    public static class WorkRetainedFragment extends ListFeedWorkRetainedFragment {

        private ApiMyFeeds mFeedsService;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mFeedsService = RestClient.getAPiMyFeeds();
        }

        @Override
        protected String getKeysSuffix() {
            return "MyFeedWorkFragment";
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return mFeedsService.getMyFeed(sinceEntryId, limit);
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
    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onShowAdditionalMenuClicked();
        void onCurrentUserAvatarClicked(View view, User user, TlogDesign design);
        void onSharePostMenuClicked(Entry entry);
        TabbarFragment getTabbar();
    }
}
