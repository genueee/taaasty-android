package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
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
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LikesHelper;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DateIndicatorWidget;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;
import rx.Observable;


public class MyAdditionalFeedFragment extends Fragment implements IRereshable,
        ListFeedWorkRetainedFragment.TargetFragmentInteraction{

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

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private View mEmptyView;

    private Adapter mAdapter;

    private int mFeedType = FEED_TYPE_FAVORITES;

    private DateIndicatorWidget mDateIndicatorView;

    private WorkRetainedFragment mWorkFragment;

    private Handler mHandler;

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
        mListView.addItemDecoration(new DividerFeedListInterPost(getActivity(), isUserAvatarShown()));

        setupEmptyView(v);

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
        mAdapter.registerAdapterDataObserver(mUpdateIndicatorObserver);
        mListView.setAdapter(mAdapter);

        setupFeedDesign();
        setupUser();
        setupAdapterPendingIndicator();
        onLoadingStateChanged("onWorkFragmentActivityCreated()");
    }

    @Override
    public void onWorkFragmentResume() {
        if (!mWorkFragment.isRefreshing()) refreshData(false);
        updateDateIndicatorDelayed();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWorkFragment.setTargetFragment(null, 0);
        mDateIndicatorView = null;
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

        ((TextView)root.findViewById(R.id.empty_view)).setText(textNoRecords);
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
        mListView.setBackgroundDrawable(new ColorDrawable(mWorkFragment.getTlogDesign().getFeedBackgroundColor(getResources())));
    }

    void updateDateIndicator(boolean animScrollUp) {
        FeedsHelper.updateDateIndicator(mListView, mDateIndicatorView, mAdapter, animScrollUp);
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
                        + " feed is empty: " + (mWorkFragment != null && mWorkFragment.getEntryList().isEmpty())
                        + " adapter != null: " + (mAdapter != null)
        );

        // Здесь индикатор не ставим, только снимаем. Устанавливает индикатор либо сам виджет
        // при свайпе вверх, либо если адаптер пустой. В другом месте.
        boolean isRefreshing = mWorkFragment == null || mWorkFragment.isRefreshing();
        if (!isRefreshing) mRefreshLayout.setRefreshing(false);

        boolean listIsEmpty = mAdapter != null
                && mWorkFragment != null
                && !mWorkFragment.isLoading()
                && mWorkFragment.getEntryList().isEmpty();

        mEmptyView.setVisibility(listIsEmpty ? View.VISIBLE : View.GONE);
        if (listIsEmpty) mDateIndicatorView.setVisibility(View.INVISIBLE);
    }

    private final Runnable mRefreshLoadingState = new Runnable() {
        @Override
        public void run() {
            if (mListView == null) return;
            setupLoadingState();
            setupAdapterPendingIndicator();
        }
    };

    public class Adapter extends FeedItemAdapterLite {
        private String mTitle;
        private final int mFeedName;
        private final int mFeedNameVisibility;
        private final int mFeedNameLeftDrawable;

        public Adapter(SortedList<Entry> feed, boolean showUserAvatar) {
            super(feed, showUserAvatar);

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
            setInteractionListener(new InteractionListener() {
                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize) {
                    if (mWorkFragment != null) mWorkFragment.onBindViewHolder(position);
                }
            });
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
            if (!TextUtils.equals(mTitle, title)) {
                mTitle = title;
                notifyItemChanged(0);
            }
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
            User user = mWorkFragment.getCurrentUser();
            if (user == null) user = CurrentUser.DUMMY;
            ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                    holder.avatarView,
                    R.dimen.avatar_normal_diameter
            );
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

        @Override
        public void onEventMainThread(EntryChanged event) {
            if(!event.postEntry.isFavorited() && (mFeedType == FEED_TYPE_FAVORITES)) {
                mAdapter.removeEntry(event.postEntry);
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

        @Override
        public void onChanged() {
            updateDateIndicatorDelayed();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            updateDateIndicatorDelayed();
        }
    };

    void updateDateIndicatorDelayed() {
        if (mListView != null) {
            mListView.removeCallbacks(mUpdateIndicatorRunnable);
            mListView.postDelayed(mUpdateIndicatorRunnable, 64);
        }
    }

    private Runnable mUpdateIndicatorRunnable = new Runnable() {
        @Override
        public void run() {
            updateDateIndicator(true);
        }
    };

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
        private int mFeedType;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mFeedsService = RestClient.getAPiMyFeeds();
            mFeedType = getArguments().getInt(BUNDLE_ARG_FEED_TYPE);
        }

        @Override
        protected String getKeysSuffix() {
            return "MyAdditionalWorkFragment"+mFeedType;
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            switch (mFeedType) {
                case FEED_TYPE_MAIN:
                    return mFeedsService.getMyFeed(sinceEntryId, limit);
                case FEED_TYPE_FRIENDS:
                    return mFeedsService.getMyFriendsFeed(sinceEntryId, limit);
                case FEED_TYPE_FAVORITES:
                    return mFeedsService.getMyFavoritesFeed(sinceEntryId, limit);
                case FEED_TYPE_PRIVATE:
                    return mFeedsService.getMyPrivateFeed(sinceEntryId, limit);
                default:
                    throw new IllegalStateException();
            }
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
    }
}
