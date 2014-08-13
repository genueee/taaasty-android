package ru.taaasty.adapters;


import android.content.Context;
import android.util.Log;

import com.commonsware.cwac.endless.EndlessAdapter;

import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public abstract class EndlessFeedGridItemAdapter extends EndlessAdapter {
    private static final String TAG = "EndlessFeedGridItemAdapter";
    private static final boolean DBG = BuildConfig.DEBUG;

    private final FeedGridItemAdapter mAdapter;

    private Subscription mFeedAppendSubscription = Subscriptions.empty();

    public EndlessFeedGridItemAdapter(Context context) {
        super(context, new FeedGridItemAdapter(context), R.layout.endless_loading_indicator, false);
        mAdapter = (FeedGridItemAdapter)getWrappedAdapter();
        setRunInBackground(false);
    }

    @Override
    protected boolean cacheInBackground() throws Exception {
        if (DBG) Log.v(TAG, "cacheInBackground()");
        int cnt = mAdapter.getCount();
        if (cnt > 0) {
            mFeedAppendSubscription = createObservable(mAdapter.getItemId(cnt-1))
                    .subscribe(mFeedAppendObserver);
        }
        return true;
    }

    public void appendCachedData(Feed data) {
        if (data == null || data.entries.isEmpty()) {
            stopAppending();
        } else {
            mAdapter.appendFeed(data.entries);
            onDataReady();
        }
    }

    @Override
    protected void appendCachedData() {
        throw new IllegalStateException();
    }

    public void setFeed(List<Entry> entries) {
        mAdapter.setFeed(entries);
        restartAppending();
    }

    public List<Entry> getFeed() {
        return mAdapter.getFeed();
    }

    public void onDestroy() {
        mFeedAppendSubscription.unsubscribe();
    }

    public abstract void onRemoteError(Throwable e);

    public abstract Observable<Feed> createObservable(Long sinceEntryId);

    private final Observer<Feed> mFeedAppendObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            onRemoteError(e);
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            appendCachedData(feed);
        }
    };
}
