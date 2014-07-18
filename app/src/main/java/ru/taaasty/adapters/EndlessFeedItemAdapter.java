package ru.taaasty.adapters;

import android.content.Context;
import android.util.Log;


import com.commonsware.cwac.endless.EndlessAdapter;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

/**
 * Created by alexey on 17.07.14.
 */
public abstract class EndlessFeedItemAdapter extends EndlessAdapter {
    private static final String TAG = "EndlessFeedItemAdapter";
    private static final boolean DBG = BuildConfig.DEBUG;

    private final FeedItemAdapter mAdapter;

    private Subscription mFeedAppendSubscription = Subscriptions.empty();

    public EndlessFeedItemAdapter(Context context) {
        super(context, new FeedItemAdapter(context), R.layout.endless_loading_indicator, false);
        mAdapter = (FeedItemAdapter)getWrappedAdapter();
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

    public void setFeed(Feed feed) {
        mAdapter.setFeed(feed.entries);
        restartAppending();
    }

    public void setFeedDesign(TlogDesign design) {
        mAdapter.setFeedDesign(design);
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
