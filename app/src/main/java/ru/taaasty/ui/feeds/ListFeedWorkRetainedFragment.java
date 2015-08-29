package ru.taaasty.ui.feeds;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.ArrayList;
import java.util.NoSuchElementException;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.Session;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

public abstract class ListFeedWorkRetainedFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListFeedWorkFrgmnt";

    private static final String BUNDLE_KEY_FEED_ITEMS = "ru.taaasty.ui.feeds.ListFeedWorkRetainedFragment.BUNDLE_KEY_FEED_ITEMS";
    private static final String BUNDLE_KEY_FEED_DESIGN = "ru.taaasty.ui.feeds.ListFeedWorkRetainedFragment.BUNDLE_KEY_FEED_DESIGN";

    private FeedLoader mFeedLoader;

    private Subscription mCurrentUserSubscription = Subscriptions.unsubscribed();

    private CurrentUser mCurrentUser;

    private CustomErrorView mListener;

    private TlogDesign mTlogDesign;

    private SortedList<Entry> mEntryList;

    /**
     * @return Суффикс к ключам в onSaveInstnceState/onRestoreInstanceState
     */
    protected abstract String getKeysSuffix();

    protected abstract Observable<Feed> createObservable(Long sinceEntryId, Integer limit);

    /**
     * Обновлять ли текущего юзера при загрузке.
     * @return
     */
    protected abstract boolean isUserRefreshEnabled();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CustomErrorView) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CustomErrorView");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mEntryList = new FeedSortedList(new FeedSortedList.IAdapterProvider() {
            @Nullable
            @Override
            public RecyclerView.Adapter getTargetAdapter() {
                return getMainFragment() == null ? null : getMainFragment().getAdapter();
            }
        });
        mFeedLoader = new FeedLoader(mEntryList);

        mCurrentUser = Session.getInstance().getCachedCurrentUser();
        if (savedInstanceState != null) {
            ArrayList<Entry> feed = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_FEED_ITEMS + getKeysSuffix());
            if (feed != null) mEntryList.resetItems(feed);

            TlogDesign design = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_DESIGN + getKeysSuffix());
            if (design != null) {
                mTlogDesign = design;
            }
        }
        if (mTlogDesign == null && mCurrentUser != null) mTlogDesign = mCurrentUser.getDesign();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!mEntryList.isEmpty()) {
            outState.putParcelableArrayList(BUNDLE_KEY_FEED_ITEMS + getKeysSuffix(), new ArrayList<>(mEntryList.getItems()));
        }

        if (mTlogDesign != null) {
            outState.putParcelable(BUNDLE_KEY_FEED_DESIGN + getKeysSuffix(), mTlogDesign);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentActivityCreated();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCurrentUserSubscription.unsubscribe();
        if (mFeedLoader != null) {
            mFeedLoader.onDestroy();
            mFeedLoader = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Nullable
    public TargetFragmentInteraction getMainFragment() {
        return (TargetFragmentInteraction) getTargetFragment();
    }

    public void refreshData() {
        if (DBG) Log.v(TAG, "refreshData()");
        if (isUserRefreshEnabled()) refreshUser();
        refreshFeed();
    }

    public SortedList<Entry> getEntryList() {
        return mEntryList;
    }

    @Nullable
    public TlogDesign getTlogDesign() {
        if (DBG && !isUserRefreshEnabled()) throw new IllegalStateException();
        return mTlogDesign;
    }

    public CurrentUser getCurrentUser() {
        if (DBG && !isUserRefreshEnabled()) throw new IllegalStateException();
        return mCurrentUser;
    }

    /**
     * @return Активна какая-либо из загрузок: пользователь, рефреш, подгрузка
     */
    public boolean isLoading() {
        return !mCurrentUserSubscription.isUnsubscribed() || mFeedLoader.isLoading();
    }

    /**
     * @return лента полностью загружена с сервера и она пустая
     */
    public boolean isFeedEmpty() {
        return !mFeedLoader.isKeepOnAppending() && mEntryList.isEmpty();
    }

    public boolean isPendingIndicatorShown() {
        return mFeedLoader.isPendingIndicatorShown();
    }

    public boolean isRefreshing() {
        return mFeedLoader.isRefreshing();
    }

    public void onBindViewHolder(int feedLocation) {
        mFeedLoader.onBindViewHolder(feedLocation);
    }

    public void refreshUser() {
        if (DBG && !isUserRefreshEnabled()) throw new IllegalStateException();
        mCurrentUserSubscription.unsubscribe();
        Observable<CurrentUser> observableCurrentUser = AppObservable.bindSupportFragment(this,
                Session.getInstance().getCurrentUser());

        mCurrentUserSubscription = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        callLoadingStateChanged("refreshUser() doOnUnsubscribe");
                    }
                })
                .subscribe(mCurrentUserObserver);
        callLoadingStateChanged("refreshUser start");
    }

    public void refreshFeed() {
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<Feed> observableFeed = mFeedLoader.createObservable(null, requestEntries);
        mFeedLoader.refreshFeed(observableFeed, requestEntries);
        callLoadingStateChanged("refreshFeed() start");
    }

    protected Func1<Feed, Feed> getPostCacheFunc() {
        return null;
    }

    void callLoadingStateChanged(String reason) {
        if (DBG) Log.v(TAG, "callLoadingStateChanged: " + reason);
        if (getMainFragment() != null) getMainFragment().onLoadingStateChanged(reason);
    }

    private final Observer<CurrentUser> mCurrentUserObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "refresh author error", e);
            // XXX
            if (e instanceof NoSuchElementException) {
            }
        }

        @Override
        public void onNext(CurrentUser currentUser) {
            if (!Objects.equals(mCurrentUser, currentUser)) {
                mCurrentUser = currentUser;
                if (getMainFragment() != null) getMainFragment().onCurrentUserChanged();
            }

            if (!Objects.equals(mTlogDesign, currentUser.getDesign())) {
                mTlogDesign = currentUser.getDesign();
                if (getMainFragment() != null) getMainFragment().onDesignChanged();
            }
        }
    };

    class FeedLoader extends ru.taaasty.ui.feeds.FeedLoader {

        public FeedLoader(SortedList<Entry> list) {
            super(list);
        }

        @Override
        protected Observable<Feed> createObservable(Long sinceEntryId, Integer limit) {
            return ListFeedWorkRetainedFragment.this.createObservable(sinceEntryId, limit);
        }

        @Nullable
        protected Func1<Feed, Feed> getPostCacheFunc() {
            return ListFeedWorkRetainedFragment.this.getPostCacheFunc();
        }

        @Override
        protected void onKeepOnAppendingChanged(boolean newValue) {
            if (DBG) Log.v(TAG, "onKeepOnAppendingChanged() keepOn: " + newValue);
        }

        @Override
        protected void onShowPendingIndicatorChanged(boolean newValue) {
            if (DBG) Log.v(TAG, "onShowPendingIndicatorChanged() show: " + newValue);
            if (getMainFragment() != null) getMainFragment().onShowPendingIndicatorChanged(newValue);
        }

        @Override
        protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
            super.onLoadError(isRefresh, entriesRequested, e);
            if (mListener != null)
                mListener.notifyError(
                        UiUtils.getUserErrorText(getResources(), e, R.string.error_append_feed), e);
        }

        protected void onFeedIsUnsubscribed(boolean isRefresh) {
            if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
            callLoadingStateChanged("onFeedIsUnsubscribed refresh: " + isRefresh);
        }
    }

    public interface TargetFragmentInteraction extends RetainedFragmentCallbacks {
        void onShowPendingIndicatorChanged(boolean newValue);
        void onLoadingStateChanged(String reason);
        void onDesignChanged();
        void onCurrentUserChanged();
        RecyclerView.Adapter getAdapter();
    }
}
