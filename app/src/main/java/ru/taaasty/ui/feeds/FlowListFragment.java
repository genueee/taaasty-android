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
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.adapters.FlowListAdapter;
import ru.taaasty.adapters.HeaderTitleSubtitleViewHolder;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.events.OnCurrentUserChanged;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.Stats;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.ui.CreateFlowActivity;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.FabHelper;
import ru.taaasty.widgets.FeedBackgroundDrawable;
import ru.taaasty.widgets.LinearLayoutManagerNonFocusable;

/**
 * Created by alexey on 30.08.15.
 */
public class FlowListFragment extends Fragment implements IFeedsFragment, FlowListWorkFragment.TargetFragmentInteraction {

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

        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData(false);
            }
        });

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setLayoutManager(new LinearLayoutManagerNonFocusable(getActivity()));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.getItemAnimator().setSupportsChangeAnimations(false);

        mListView.addOnScrollListener(new FeedsHelper.StopGifOnScroll());

        mHideTabbarListener = new FabHelper.AutoHideScrollListener(mListener.getTabbar().getFab());
        mListView.addOnScrollListener(mHideTabbarListener);
        mListView.addItemDecoration(new RecyclerView.ItemDecoration() {

            private final int mFeedHorizontalMargin = container.getResources().getDimensionPixelSize(R.dimen.feed_horizontal_margin);

            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                RecyclerView.ViewHolder vh = parent.getChildViewHolder(view);
                if (vh instanceof FlowListAdapter.ViewHolderItem) {
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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
    public void onWorkFragmentActivityCreated() {
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
                if (user != null) {
                    if (user.getDesign() != null) design = user.getDesign();
                }
                ((HeaderViewHolder) viewHolder).bindDesign(design);
                ((HeaderViewHolder)viewHolder).setActivatedFlowTab(mWorkFragment.getSelectedFlowTab());

                boolean canCreateFlows = user != null && user.canCreateFlows();
                ((HeaderViewHolder)viewHolder).buttonCreateFlow.setVisibility(canCreateFlows ? View.VISIBLE : View.GONE);

                String subtitle = null;
                Stats stats = mListener == null ? null : mListener.getStats();
                if (stats != null
                        && stats.getFlowsTotal() != null
                        && stats.getFlowsTotal() > 0) {
                    int flowsTotal = stats.getFlowsTotal();
                    subtitle = getResources().getQuantityString(R.plurals.flows_total, flowsTotal, flowsTotal);
                }
                ((HeaderTitleSubtitleViewHolder) viewHolder).setTitleSubtitle(R.string.title_flows, subtitle);
            }

            @Override
            public void initClickListeners(RecyclerView.ViewHolder holder, int type) {
                switch (type) {
                    case FlowListAdapter.VIEW_TYPE_HEADER:
                        HeaderViewHolder headerHolder = (HeaderViewHolder)holder;
                        headerHolder.flowsTabAll.setOnClickListener(mOnFlowTabClickListener);
                        headerHolder.flowsTabMy.setOnClickListener(mOnFlowTabClickListener);
                        headerHolder.buttonCreateFlow.setOnClickListener(mOnFlowTabClickListener);
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
                            ((TaaastyApplication) getActivity().getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FLOWS,
                                    "Переключение на все потоки", null);
                            break;
                        case R.id.flows_my: switchTab(FlowListWorkFragment.FLOwS_TAB_MY);
                            ((TaaastyApplication) getActivity().getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FLOWS,
                                    "Переключение на мои потоки", null);
                            break;
                        case R.id.create_anonymous_post_or_flow:
                            CreateFlowActivity.startActivity(v.getContext(), v);
                            ((TaaastyApplication) getActivity().getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FLOWS,
                                    "Открыто создание потока", null);
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
                            TlogActivity.startTlogActivity(getActivity(), flow.getSlug(), v);
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
    public void onWorkFragmentResume() {
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
    public void onLoadError(CharSequence error, @Nullable Throwable exception) {
        if (mListener != null) mListener.notifyError(error, exception);
    }

    private class HeaderViewHolder extends HeaderTitleSubtitleViewHolder {

        public final View flowsTabAll;

        public final View flowsTabMy;

        public final View buttonCreateFlow;

        public HeaderViewHolder(View v) {
            super(v);
            View tabs = ((ViewStub)v.findViewById(R.id.flows_tab_layout)).inflate();
            flowsTabAll = tabs.findViewById(R.id.flows_all);
            flowsTabMy = tabs.findViewById(R.id.flows_my);
            buttonCreateFlow = v.findViewById(R.id.create_anonymous_post_or_flow);

            buttonCreateFlow.setContentDescription(v.getContext().getText(R.string.create_flow));
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

    public interface OnFragmentInteractionListener extends CustomErrorView {

        void startRefreshStats();

        void startRefreshCurrentUser();

        public @Nullable
        Stats getStats();

        void onGridTopViewScroll(Fragment fragment, boolean headerVisible, int headerTop);

        TabbarFragment getTabbar();

    }
}
