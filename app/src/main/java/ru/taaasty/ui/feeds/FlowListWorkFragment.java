package ru.taaasty.ui.feeds;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.adapters.FlowListManaged;
import ru.taaasty.recyclerview.RecyclerView;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.FlowList;
import ru.taaasty.rest.service.ApiFlows;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

/**
 * Created by alexey on 02.09.15.
 */
public class FlowListWorkFragment extends Fragment {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({FLOWS_TAB_ALL, FLOwS_TAB_MY})
    public @interface FlowTab {}
    public static final int FLOWS_TAB_ALL = R.id.flows_all;
    public static final int FLOwS_TAB_MY = R.id.flows_my;

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FlowListWorkFragment";

    private static final String BUNDLE_KEY_FLOW_LIST = "FlowListWorkFragment.BUNDLE_KEY_FLOW_LIST";
    private static final String BUNDLE_KEY_SELECTED_TAB_FRAGMENT = "FlowListWorkFragment.BUNDLE_KEY_SELECTED_TAB_FRAGMENT";

    private FlowListManaged mFlowList;

    private FlowListLoader mLoader;

    @FlowTab
    private int mSelectedTab;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        mLoader = new FlowListLoader();

        if (savedInstanceState != null) {
            // TODO СОхранять последний выбор где-нибудь в сессии?
            //noinspection ResourceType
            mSelectedTab = savedInstanceState.getInt(BUNDLE_KEY_SELECTED_TAB_FRAGMENT, FLOWS_TAB_ALL);
            mFlowList = savedInstanceState.getParcelable(BUNDLE_KEY_FLOW_LIST);
            if (mFlowList == null) mFlowList = new FlowListManaged();
        } else {
            mSelectedTab = FLOWS_TAB_ALL;
            mFlowList = new FlowListManaged();
        }

        mFlowList.setListener(new FlowListManaged.FlowChangedCallback() {
            @Override
            public void onDataSetChanged() {
                getInteractionListener().onAdapterDataSetChanged();
            }

            @Override
            public void onChanged(int location, int count) {
                getInteractionListener().onAdapterDataChanged(location, count);
            }

            @Override
            public void onInserted(int location, int count) {
                getInteractionListener().onAdapterDataInserted(location, count);
            }

            @Override
            public void onMoved(int fromLocation, int toLocation) {
                getInteractionListener().onAdapterDataItemMoved(fromLocation, toLocation);
            }

            @Override
            public void onRemoved(int location, int count) {
                getInteractionListener().onAdapterDataRemoved(location, count);
            }
        });

        mFlowList.onCreate();
        mLoader.onCreate();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!mFlowList.isEmpty()) {
            outState.putParcelable(BUNDLE_KEY_FLOW_LIST, mFlowList);
        }
        outState.putInt(BUNDLE_KEY_SELECTED_TAB_FRAGMENT, mSelectedTab);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((RetainedFragmentCallbacks)getTargetFragment()).onWorkFragmentActivityCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((RetainedFragmentCallbacks)getTargetFragment()).onWorkFragmentResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFlowList.onDestroy();
        mLoader.onDestroy();
    }

    public TargetFragmentInteraction getInteractionListener() {
        return getTargetFragment() != null ? (TargetFragmentInteraction) getTargetFragment() : DUMMY_INTERACTION;
    }

    void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int feedLocation) {
        mLoader.onBindViewHolder(viewHolder, feedLocation, mFlowList.size());
    }

    public FlowListManaged getFlowList() {
        return mFlowList;
    }

    @FlowTab
    public int getSelectedFlowTab() {
        return mSelectedTab;
    }

    public void refreshFeed() {
        mLoader.refreshFeed(false);
    }

    /**
     * @return Активно обновление, либо подгрузка
     */
    public boolean isLoading() {
        return mLoader.isLoading();
    }

    /**
     * @return лента полностью загружена с сервера и она пустая
     */
    public boolean isFeedEmpty() {
        return !mLoader.mKeepOnAppending && mFlowList.isEmpty();
    }


    void onNewListPendingIndicatorStatus(boolean isShown) {
        getInteractionListener().onShowPendingIndicatorChanged(isShown);
    }

    void onFlowTabSelected(@FlowTab int newTab) {
        mSelectedTab = newTab;
        if (newTab == FLOwS_TAB_MY) {
            mFlowList.grepMyFlows();
        }
        mLoader.refreshFeed(true);
    }

    private class FlowListLoader {

        public static final int ENTRIES_TO_TRIGGER_APPEND = 3;

        public static final int LOAD_LIMIT = 15;

        private final Handler mHandler;

        /**
         * Лента загружена не до конца, продолжаем подгружать
         */
        private boolean mKeepOnAppending;

        /**
         * Подгрузка нотификаций
         */
        private Subscription mAppendSubscription;

        /**
         * Обновление нотификаций
         */
        private Subscription mRefreshSubscription;

        private final ApiFlows mApiFlows;

        private int mNextPage;

        public FlowListLoader()  {
            mHandler = new Handler();
            mKeepOnAppending = true;
            mAppendSubscription = Subscriptions.unsubscribed();
            mRefreshSubscription = Subscriptions.unsubscribed();
            mApiFlows = RestClient.getAPiFlows();
            mNextPage = 1;
        }

        protected Observable<FlowList> createObservable(int page) {
            switch (getSelectedFlowTab()) {
                case FLOWS_TAB_ALL:
                    return mApiFlows.getFlows(page, LOAD_LIMIT);
                case FLOwS_TAB_MY:
                    return mApiFlows.getMyFlows(page, LOAD_LIMIT, false);
                default:
                    throw new IllegalStateException();
            }
        }

        public void refreshFeed(boolean forceReplace) {
            if (forceReplace) {
                mRefreshSubscription.unsubscribe();
                mAppendSubscription.unsubscribe();
            } else {
                if (!mRefreshSubscription.isUnsubscribed()) {
                    return;
                }
            }
            mKeepOnAppending = true;
            mNextPage = 1;
            Observable<FlowList> observable = createObservable(mNextPage);
            mRefreshSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .finallyDo(mFinallySetupLoadingState)
                    .subscribe(new LoadObserver(true, forceReplace));
        }

        public boolean isLoading() {
            return !mAppendSubscription.isUnsubscribed() || !mRefreshSubscription.isUnsubscribed();
        }

        public boolean isRefreshing() {
            return !mRefreshSubscription.isUnsubscribed();
        }

        public void onCreate() {
        }

        public void onDestroy() {
            mAppendSubscription.unsubscribe();
            mRefreshSubscription.unsubscribe();
        }

        private void setKeepOnAppending(boolean newValue) {
            mKeepOnAppending = newValue;
            if (!newValue) onNewListPendingIndicatorStatus(false);
        }

        private void startLoadNextPage() {
            if (DBG) Log.v(TAG, "startLoadNextPage() page: " + mNextPage);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isLoading()) return;
                    mAppendSubscription.unsubscribe();
                    onNewListPendingIndicatorStatus(true);
                    mAppendSubscription = createObservable(mNextPage)
                            .observeOn(AndroidSchedulers.mainThread())
                            .finallyDo(mFinallySetupLoadingState)
                            .subscribe(new LoadObserver(false, false));
                }
            });
        }

        private void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize) {
            if (!mKeepOnAppending
                    || feedSize == 0
                    || isLoading()) return;
            if (position >= feedSize - ENTRIES_TO_TRIGGER_APPEND) startLoadNextPage();
        }

        private Action0 mFinallySetupLoadingState = new Action0() {
            @Override
            public void call() {
                FlowListFragment target = (FlowListFragment)getTargetFragment();
                if (target != null) {
                    // TODO заменить на observable?
                    target.setupLoadingState();
                }
            }
        };

        public class LoadObserver implements Observer<FlowList> {

            private final boolean mIsRefresh;

            private final boolean mForceReplace;

            public LoadObserver(boolean isRefresh, boolean forceReplace) {
                mIsRefresh = isRefresh;
                mForceReplace = forceReplace;
            }

            @Override
            public void onCompleted() {
                if (DBG) Log.v(TAG, "onCompleted()");
            }

            @Override
            public void onError(Throwable e) {
                if (DBG) Log.e(TAG, "onError", e);
                onNewListPendingIndicatorStatus(false);
                getInteractionListener().onLoadError(
                        UiUtils.getUserErrorText(getResources(), e, R.string.error_loading_flows), e);
            }

            @Override
            public void onNext(FlowList list) {
                boolean keepOnAppending = (list != null) && (list.hasMore);

                onNewListPendingIndicatorStatus(false);
                if (list != null) {
                    if (mIsRefresh) {
                        if (mForceReplace) {
                            mFlowList.reset(list.items);
                        } else {
                            mFlowList.refreshOrAddItems(list.items);
                        }
                    } else {
                        mFlowList.appendEntries(list.items);
                    }
                    mNextPage = list.nextPage;
                }
                setKeepOnAppending(keepOnAppending);
            }
        }
    }

    public interface TargetFragmentInteraction extends RetainedFragmentCallbacks {
        // TODO заменить на observable?
        void onShowPendingIndicatorChanged(boolean newValue);
        void onAdapterDataSetChanged();
        void onAdapterDataChanged(int listLocation, int count);
        void onAdapterDataInserted(int listLocation, int count);
        void onAdapterDataItemMoved(int listLocation, int count);
        void onAdapterDataRemoved(int listLocation, int count);
        void onLoadError(CharSequence error, @Nullable Throwable exception);
    }

    private static final TargetFragmentInteraction DUMMY_INTERACTION = new TargetFragmentInteraction() {
        @Override public void onShowPendingIndicatorChanged(boolean newValue) {}
        @Override public void onAdapterDataSetChanged() {}
        @Override public void onAdapterDataChanged(int listLocation, int count) {}
        @Override public void onAdapterDataInserted(int listLocation, int count) {}
        @Override public void onAdapterDataItemMoved(int listLocation, int count) {}
        @Override public void onAdapterDataRemoved(int listLocation, int count) {}
        @Override public void onLoadError(CharSequence error, @Nullable Throwable exception) {}
        @Override public void onWorkFragmentActivityCreated() {}
        @Override public void onWorkFragmentResume() {}
    };
}
