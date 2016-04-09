package ru.taaasty.ui.feeds;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.adapters.FlowListAdapter;
import ru.taaasty.adapters.HeaderTitleSubtitleViewHolder;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.events.OnCurrentUserChanged;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.FragmentStateConsumer;
import ru.taaasty.ui.FragmentWithWorkFragment;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.FabHelper;
import ru.taaasty.widgets.FeedBackgroundDrawable;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;

/**
 * Created by alexey on 30.08.15.
 */
public class FlowListFragment extends FragmentWithWorkFragment<FlowListWorkFragment> implements IFeedsFragment, FlowListWorkFragment.TargetFragmentInteraction {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FlowListFragment";

    private OnFragmentInteractionListener mListener;

    private SwipeRefreshLayout mRefreshLayout;
    private RecyclerView mListView;
    private TextView mEmptyView;

    private FabHelper.AutoHideScrollListener mHideTabbarListener;

    private FlowListAdapter mAdapter;

    private FlowListWorkFragment mWorkFragment;

    public static FlowListFragment createInstance() {
        return new FlowListFragment();
    }

    public FlowListFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
            mListener.onFragmentAttached(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_flow_list, container, false);
        mRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_widget);
        mEmptyView = (TextView)v.findViewById(R.id.empty_view);

        mRefreshLayout.setOnRefreshListener(() -> refreshData(false));

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));

        mHideTabbarListener = new FabHelper.AutoHideScrollListener(mListener.getTabbar().getFab());
        mListView.addOnScrollListener(mHideTabbarListener);
        mListView.addItemDecoration(new RecyclerView.ItemDecoration() {

            private final int mFeedHorizontalMargin = container.getResources().getDimensionPixelSize(R.dimen.feed_horizontal_margin);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                RecyclerView.ViewHolder vh = parent.getChildViewHolder(view);
                if (vh != null && vh.getItemViewType() == FlowListAdapter.VIEW_TYPE_ITEM) {
                    outRect.set(mFeedHorizontalMargin, 0, mFeedHorizontalMargin, 0);
                } else {
                    super.getItemOffsets(outRect, view, parent, state);
                }
            }
        });

        Resources resource = getResources();
        FeedBackgroundDrawable background = new FeedBackgroundDrawable(resource, null,
                resource.getDimensionPixelSize(R.dimen.header_title_subtitle_height));
        TlogDesign lightDesign = TlogDesign.createLightTheme(TlogDesign.DUMMY);
        background.setFeedDesign(resource, lightDesign);
        background.setFeedMargin(resource.getDimensionPixelSize(R.dimen.feed_horizontal_margin), 0,
                resource.getDimensionPixelSize(R.dimen.feed_horizontal_margin), 0);
        mListView.setBackgroundDrawable(background);

        return v;
    }

    @Nullable
    @Override
    public FlowListWorkFragment getWorkFragment() {
        return mWorkFragment;
    }

    @Override
    public void initWorkFragment() {
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (FlowListWorkFragment) fm.findFragmentByTag("FlowListWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new FlowListWorkFragment();
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "FlowListWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }

    @Override
    public void onWorkFragmentActivityCreatedSafe() {
        mAdapter = new FlowListAdapter(getActivity(), new FlowListAdapter.InteractionListener() {
            @Override
            public void onBindFlow(FlowListAdapter.ViewHolderItem holder, Flow flow, Relationship relationship,  int position) {
                mWorkFragment.onBindViewHolder(holder, position);
            }

            @Override
            public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent) {
                View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_title_subtitle, parent, false);
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) child.getLayoutParams();
                params.bottomMargin = 0;
                child.setLayoutParams(params);
                HeaderViewHolder holder =  new HeaderViewHolder(child);
                holder.setActivatedFlowTab(mWorkFragment.getSelectedFlowTab());
                return holder;
            }

            @Override
            public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder) {
                TlogDesign design = TlogDesign.createLightTheme(TlogDesign.DUMMY);
                CurrentUser user = Session.getInstance().getCachedCurrentUser();
                if (user.getDesign() != null) design = user.getDesign();
                ((HeaderViewHolder) viewHolder).bindDesign(design);
                ((HeaderTitleSubtitleViewHolder) viewHolder).setTitleSubtitle(R.string.title_flows, null);
                if (user.isAuthorized()) {
                    ((HeaderViewHolder) viewHolder).setActivatedFlowTab(mWorkFragment.getSelectedFlowTab());
                    ((HeaderViewHolder) viewHolder).flowsTabs.setVisibility(View.VISIBLE);
                } else {
                    ((HeaderViewHolder) viewHolder).flowsTabs.setVisibility(View.GONE);
                }

            }

            @Override
            public void initClickListeners(RecyclerView.ViewHolder holder, int type) {
                switch (type) {
                    case FlowListAdapter.VIEW_TYPE_HEADER:
                        HeaderViewHolder headerHolder = (HeaderViewHolder)holder;
                        headerHolder.flowsTabAll.setOnClickListener(mOnFlowTabClickListener);
                        headerHolder.flowsTabMy.setOnClickListener(mOnFlowTabClickListener);
                        break;
                    case FlowListAdapter.VIEW_TYPE_ITEM:
                        holder.itemView.setOnClickListener(mOnClickListener);
                        break;
                }
            }

            private final View.OnClickListener mOnFlowTabClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.flows_all: switchTab(FlowListWorkFragment.FLOWS_TAB_ALL);
                            AnalyticsHelper.getInstance().sendFlowsEvent("Переключение на все потоки");
                            break;
                        case R.id.flows_my: switchTab(FlowListWorkFragment.FLOwS_TAB_MY);
                            AnalyticsHelper.getInstance().sendFlowsEvent("Переключение на мои потоки");
                            break;
                        default:
                            throw new IllegalStateException();
                    }
                }

                private void switchTab(int newTab) {
                    if (mWorkFragment.getSelectedFlowTab() != newTab) {
                        mWorkFragment.onFlowTabSelected(newTab);
                        mAdapter.notifyItemChanged(0);
                        mRefreshLayout.setRefreshing(true);
                    }
                }
            };

            private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = mListView.getChildAdapterPosition(v);
                    if (position > 0) {
                        Flow flow = mAdapter.getFlow(position);
                        if (flow != null) {
                            TlogActivity.startTlogActivity(getActivity(), flow.getId(), v);
                        }
                    }
                }
            };

        }, mWorkFragment.getFlowList()) {
            @Override
            public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
                super.onViewAttachedToWindow(holder);
                if (holder instanceof IParallaxedHeaderHolder) {
                    if (mListener != null) mListener.onGridTopViewScroll(FlowListFragment.this, true, holder.itemView.getTop());
                }
            }

            @Override
            public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
                super.onViewDetachedFromWindow(holder);
                if (holder instanceof IParallaxedHeaderHolder) {
                    if (mListener != null) mListener.onGridTopViewScroll(FlowListFragment.this, false, 0);
                }
            }
        };
        mListView.setAdapter(mAdapter);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onWorkFragmentResumeSafe() {
        refreshData(false);
        setupLoadingState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        mWorkFragment.setTargetFragment(null, 0);
        mEmptyView = null;
        mRefreshLayout = null;
        mHideTabbarListener = null;
        mListView = null;
        mAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.onFragmentDetached(this);
        mListener = null;
    }

    @Override
    public void refreshData(boolean forceShowRefreshIndicator) {
        if (DBG) Log.v(TAG, "refreshData()");
        if (!mRefreshLayout.isRefreshing()) {
            mRefreshLayout.setRefreshing(mWorkFragment.getFlowList().isEmpty() || forceShowRefreshIndicator);
        }
        mWorkFragment.refreshFeed();
    }

    public void onEventMainThread(OnCurrentUserChanged event) {
        if (mAdapter == null) return;
        mAdapter.notifyItemChanged(0);
    }

    public void onEventMainThread(OnStatsLoaded event) {
        if (mAdapter == null) return;
        mAdapter.notifyItemChanged(0);
    }

    public void setupLoadingState() {
        if (DBG) Log.v(TAG, "setupLoadingState");
        // Здесь индикатор не ставим, только снимаем. Устанавливает индикатор либо сам виджет
        // при свайпе вверх, либо если адаптер пустой. В другом месте.
        if (mWorkFragment != null && !mWorkFragment.isLoading()) {
            mRefreshLayout.setRefreshing(false);
        }

        boolean listIsEmpty = mAdapter != null
                && mWorkFragment != null
                && mWorkFragment.isFeedEmpty();

        if (listIsEmpty) {
            mEmptyView.setVisibility(View.VISIBLE);
            if (mWorkFragment != null && mWorkFragment.getSelectedFlowTab() == FlowListWorkFragment.FLOwS_TAB_MY) {
                mEmptyView.setText(R.string.you_have_no_subscriptions);
            } else {
                mEmptyView.setText(R.string.no_flows);
            }
        } else {
            mEmptyView.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean isHeaderVisible() {
        if (mListView == null) return false;
        View v0 = mListView.getChildAt(0);
        if (v0 == null) return false;
        return mListView.getChildViewHolder(v0) instanceof HeaderTitleSubtitleViewHolder;
    }

    @Override
    public int getHeaderTop() {
        if (mListView == null) return 0;
        View v0 = mListView.getChildAt(0);
        if (v0 == null) return 0;
        return v0.getTop();
    }

    @Override
    public void onShowPendingIndicatorChanged(boolean newValue) {
        if (mAdapter != null) mAdapter.setShowPendingIndicator(newValue);
        setupLoadingState();
    }

    @Override
    public void onAdapterDataSetChanged() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAdapterDataChanged(int listLocation, int count) {
        if (mAdapter != null) mAdapter.notifyItemChanged(FlowListAdapter.getAdapterPosition(listLocation), count);
    }

    @Override
    public void onAdapterDataInserted(int listLocation, int count) {
        if (mAdapter != null) mAdapter.notifyItemInserted(FlowListAdapter.getAdapterPosition(listLocation));
    }

    @Override
    public void onAdapterDataItemMoved(int listLocation, int toLocation) {
        if (mAdapter != null) mAdapter.notifyItemMoved(FlowListAdapter.getAdapterPosition(listLocation), FlowListAdapter.getAdapterPosition(toLocation));
    }

    @Override
    public void onAdapterDataRemoved(int listLocation, int count) {
        if (mAdapter != null) mAdapter.notifyItemRangeRemoved(FlowListAdapter.getAdapterPosition(listLocation), count);
    }

    @Override
    public void onLoadError(@Nullable Throwable exception, int fallbackResId) {
        if (mListener != null) mListener.notifyError(
                FlowListFragment.this, exception, R.string.error_loading_flows);
    }

    private class HeaderViewHolder extends HeaderTitleSubtitleViewHolder {

        public final View flowsTabs;

        public final View flowsTabAll;

        public final View flowsTabMy;

        public HeaderViewHolder(View v) {
            super(v);
            flowsTabs = ((ViewStub)v.findViewById(R.id.flows_tab_layout)).inflate();
            flowsTabAll = flowsTabs.findViewById(R.id.flows_all);
            flowsTabMy = flowsTabs.findViewById(R.id.flows_my);

            setActivatedFlowTab(FlowListWorkFragment.FLOWS_TAB_ALL);
        }

        public void setActivatedFlowTab(int tabId) {
            flowsTabAll.setActivated(tabId == FlowListWorkFragment.FLOWS_TAB_ALL);
            flowsTabMy.setActivated(tabId == FlowListWorkFragment.FLOwS_TAB_MY);
        }

        @Override
        public void onScrollChanged() {
            if (FlowListFragment.this.getUserVisibleHint()) {
                super.onScrollChanged();
                if (mListener != null) mListener.onGridTopViewScroll(FlowListFragment.this, true, itemView.getTop());
            }
        }
    }

    public interface OnFragmentInteractionListener extends CustomErrorView, FragmentStateConsumer {

        void startRefreshCurrentUser();

        void onGridTopViewScroll(Fragment fragment, boolean headerVisible, int headerTop);

        TabbarFragment getTabbar();

    }
}
