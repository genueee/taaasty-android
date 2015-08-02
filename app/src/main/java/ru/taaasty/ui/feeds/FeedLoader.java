package ru.taaasty.ui.feeds;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TimingLogger;

import java.util.concurrent.atomic.AtomicBoolean;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Feed;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * подгрузчик записей
 */
public abstract class FeedLoader {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedLoader";
    public static final int ENTRIES_TO_TRIGGER_APPEND = 7;

    private final SortedList<Entry> mList;

    /**
     * Лента загружена не до конца, продолжаем подгружать
     */
    private AtomicBoolean mKeepOnAppending;

    /**
     * Подгрузка ленты
     */
    private Subscription mFeedAppendSubscription;

    /**
     * Обновление ленты
     */
    private Subscription mFeedRefreshSubscription;

    protected abstract Observable<Feed> createObservable(Long sinceEntryId, Integer limit);

    protected abstract void onKeepOnAppendingChanged(boolean newValue);

    protected abstract void onShowPendingIndicatorChanged(boolean newValue);

    private Handler mHandler;

    private boolean mActivateCacheQueued;

    public FeedLoader(SortedList<Entry> list)  {
        mList = list;
        mKeepOnAppending = new AtomicBoolean(true);
        mFeedAppendSubscription = Subscriptions.unsubscribed();
        mFeedRefreshSubscription = Subscriptions.unsubscribed();
        mActivateCacheQueued = false;
        mHandler = new Handler();
    }

    public void refreshFeed(Observable<Feed> observable, int entriesRequested) {
        if (!mFeedRefreshSubscription.isUnsubscribed()) {
            // Не используем doOnUnsubscribe. Реакция нужна только в данном случае, во всех остальных случаях
            // на unsubscribe ничего не должно происходить
            onFeedIsUnsubscribed(true);
            mFeedRefreshSubscription.unsubscribe();
        }

        mFeedRefreshSubscription = observable
                .map(mInitFeedSpannedText)
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        if (DBG) Log.v(TAG, "refreshFeed() finallyDo");
                        onFeedIsUnsubscribed(true);
                    }
                })
                .subscribe(new FeedLoadObserver(true, entriesRequested));
    }

    public boolean isLoading() {
        return !mFeedAppendSubscription.isUnsubscribed() || !mFeedRefreshSubscription.isUnsubscribed();
    }

    public boolean isRefreshing() {
        return !mFeedRefreshSubscription.isUnsubscribed();
    }

    public boolean isPendingIndicatorShown() {
        return mFeedRefreshSubscription.isUnsubscribed() && !mFeedAppendSubscription.isUnsubscribed();
    }

    public void onCreate() {

    }

    public void onDestroy() {
        mHandler.removeCallbacksAndMessages(null);
        mFeedAppendSubscription.unsubscribe();
        mFeedRefreshSubscription.unsubscribe();
    }

    private void setKeepOnAppending(boolean newValue) {
        if (mKeepOnAppending.getAndSet(newValue) != newValue) {
            onKeepOnAppendingChanged(newValue);
        }
    }

    private void activateCacheInBackground() {
        if (DBG) Log.v(TAG, "activateCacheInBackground()");
        final Entry lastEntry = mList.getLastEntry();
        if (lastEntry == null) return;
        if (!mKeepOnAppending.get()) return;
        if (isLoading()) return;

        int requestEntries = Constants.LIST_FEED_APPEND_LENGTH;
        Observable<Feed> observable = createObservable(lastEntry.getId(), requestEntries)
                .map(mInitFeedSpannedText);

        Func1<Feed,Feed > postCacheFunc = getPostCacheFunc();
        if (postCacheFunc != null) {
            observable = observable.map(postCacheFunc);
        }

        mFeedAppendSubscription = observable.observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        onShowPendingIndicatorChanged(false);
                        onFeedIsUnsubscribed(false);
                    }
                })
                .subscribe(new FeedLoadObserver(false, requestEntries));
        onShowPendingIndicatorChanged(true);
    }

    @Nullable
    protected Func1<Feed, Feed> getPostCacheFunc() {
        return null;
    }

    protected void onLoadCompleted(boolean isRefresh, int entriesRequested) {
        if (DBG) Log.v(TAG, "onCompleted()");
    }

    protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
        if (DBG) Log.e(TAG, "onError", e);
    }

    protected void onLoadNext(boolean isRefresh, int entriesRequested, Feed feed) {
        boolean keepOnAppending = (feed != null) && (feed.entries.length >= 0);

        if (feed != null) {
            // XXX Сравнивать lastEntry?
            int sizeBefore = mList.size();
            mList.addOrUpdateItems(feed.entries);
            if (!isRefresh && entriesRequested != 0 && sizeBefore == mList.size())
                keepOnAppending = false;
        }
        setKeepOnAppending(keepOnAppending);
    }

    protected void onFeedIsUnsubscribed(boolean isRefresh) {
        if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
    }

    public void onBindViewHolder(int feedLocation) {
        if (!mKeepOnAppending.get() || mList.isEmpty() || isLoading()) return;
        if (!mActivateCacheQueued && feedLocation >= mList.size() - ENTRIES_TO_TRIGGER_APPEND) queueActivateCacheInBachground();
    }

    private void queueActivateCacheInBachground() {
        if (mActivateCacheQueued) return;
        mHandler.postDelayed(mCallActivateCacheInBackground, 16);
    }

    private Runnable mCallActivateCacheInBackground = new Runnable() {
        @Override
        public void run() {
            activateCacheInBackground();
            mActivateCacheQueued = false;
        }
    };

    private final  Func1<Feed, Feed> mInitFeedSpannedText = new Func1<Feed, Feed>() {
        @Override
        public Feed call(Feed feed) {
            TimingLogger timings = null;
            if (BuildConfig.DEBUG) timings = new TimingLogger(Constants.LOG_TAG, "setup InitFeedSpannedText");

            if (DBG && Looper.myLooper() != null) {
                throw new IllegalStateException();
            }
            for (Entry entry: feed.entries)  entry.initSpannedText();

            if (BuildConfig.DEBUG && timings != null) {
                timings.addSplit("setup InitFeedSpannedText end");
                timings.dumpToLog();
            }
            return feed;
        }
    };

    public class FeedLoadObserver implements Observer<Feed> {

        private final boolean mIsRefresh;
        private final int mEntriesRequested;

        public FeedLoadObserver(boolean isRefresh, int entriesRequested) {
            mIsRefresh = isRefresh;
            mEntriesRequested = entriesRequested;
        }

        @Override
        public void onCompleted() {
            FeedLoader.this.onLoadCompleted(mIsRefresh, mEntriesRequested);
        }

        @Override
        public void onError(Throwable e) {
            FeedLoader.this.onLoadError(mIsRefresh, mEntriesRequested, e);
        }

        @Override
        public void onNext(Feed feed) {
            FeedLoader.this.onLoadNext(mIsRefresh, mEntriesRequested, feed);
        }
    }

}
