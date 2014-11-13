package ru.taaasty.ui.feeds;

import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.FeedListItem;
import ru.taaasty.model.Feed;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

/**
 * подгрузчик записей и коментариев для адаптера
 */
public abstract class FeedLoader {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedLoader";
    public static final int ENTRIES_TO_TRIGGER_APPEND = 3;

    private final FeedItemAdapter mAdapter;

    private final Handler mHandler;

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


    private CompositeSubscription mLoadCommentsSubscription;


    protected abstract Observable<Feed> createObservable(Long sinceEntryId, Integer limit);

    public FeedLoader(FeedItemAdapter adapter)  {
        mAdapter = adapter;
        mHandler = new Handler();
        mKeepOnAppending = new AtomicBoolean(true);
        mFeedAppendSubscription = SubscriptionHelper.empty();
        mFeedRefreshSubscription = SubscriptionHelper.empty();
        mAdapter.setInteractionListener(new FeedItemAdapter.InteractionListener() {
            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize, FeedListItem entry) {
                FeedLoader.this.onBindViewHolder(viewHolder, position, feedSize, entry);
            }
        });
        mLoadCommentsSubscription = new CompositeSubscription();

    }

    public void refreshFeed() {
        refreshFeed(null, Constants.LIST_FEED_INITIAL_LENGTH);
    }

    public void refreshFeed(Observable<Feed> observable, int entriesRequested) {
        if (!mFeedRefreshSubscription.isUnsubscribed()) {
            onFeedIsUnsubscribed(true);
            mFeedRefreshSubscription.unsubscribe();
        }
        mFeedRefreshSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FeedLoadObserver(true, entriesRequested));
    }

    public void onCreate() {

    }

    public void onDestroy() {
        mFeedAppendSubscription.unsubscribe();
        mFeedRefreshSubscription.unsubscribe();

    }

    private void setKeepOnAppending(boolean newValue) {
        mKeepOnAppending.set(newValue);
        mAdapter.setLoading(false);
    }

    private void activateCacheInBackground() {
        if (DBG) Log.v(TAG, "activateCacheInBackground()");
        final Long lastEntryId = mAdapter.getFeed().getLastEntryId();
        if (lastEntryId == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mAdapter.getIsLoading()) return;
                mAdapter.setLoading(true);
                if (!mFeedAppendSubscription.isUnsubscribed()) {
                    onFeedIsUnsubscribed(false);
                    mFeedAppendSubscription.unsubscribe();
                }

                int requestEntries = Constants.LIST_FEED_APPEND_LENGTH;
                mFeedAppendSubscription = createObservable(lastEntryId, requestEntries)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new FeedLoadObserver(false, requestEntries));
            }
        });
    }

    protected void onLoadCompleted(boolean isRefresh, int entriesRequested) {
        if (DBG) Log.v(TAG, "onCompleted()");
    }

    protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
        if (DBG) Log.e(TAG, "onError", e);
        mAdapter.setLoading(false);
    }

    protected void onLoadNext(boolean isRefresh, int entriesRequested, Feed feed) {
        if (DBG) Log.e(TAG, "onNext " + feed.toString());
        boolean keepOnAppending = (feed != null) && (feed.entries.size() == entriesRequested);
        if (feed != null && !feed.entries.isEmpty()) {
            if (isRefresh) {
                // XXX: мы здесь не удаляем записи. Т.е.если по каким-то причинам фид станет короче,
                // у нас останутся старые записи
                mAdapter.getFeed().addEntries(feed.entries);
            } else {
                if (!mAdapter.getFeed().appendEntries(feed.entries)) keepOnAppending = false;
            }
        }

        setKeepOnAppending(keepOnAppending);
        mAdapter.setLoading(false);
    }

    protected void onFeedIsUnsubscribed(boolean isRefresh) {
        if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
    }

    private void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize, FeedListItem entry) {
        if (!mKeepOnAppending.get() || feedSize == 0 || mAdapter.getIsLoading()) return;
        if (position >= feedSize - ENTRIES_TO_TRIGGER_APPEND) activateCacheInBackground();
    }

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
