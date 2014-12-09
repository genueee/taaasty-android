package ru.taaasty.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.grid.GridEmbeddEntry;
import ru.taaasty.adapters.grid.GridEntryBase;
import ru.taaasty.adapters.grid.GridEntryHeader;
import ru.taaasty.adapters.grid.GridEntryPendingResource;
import ru.taaasty.adapters.grid.GridImageEntry;
import ru.taaasty.adapters.grid.GridQuoteEntry;
import ru.taaasty.adapters.grid.GridTextEntry;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;


public abstract class FeedGridItemAdapter extends RecyclerView.Adapter<GridEntryBase> {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedGridItemAdapter";

    private static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;
    private static final int VIEW_TYPE_IMAGE = R.id.feed_view_type_image;
    private static final int VIEW_TYPE_EMBEDD = R.id.feed_view_type_embedd;
    private static final int VIEW_TYPE_QUOTE = R.id.feed_view_type_quote;
    private static final int VIEW_TYPE_OTHER = R.id.feed_view_type_other;
    private static final int VIEW_TYPE_PENDING = R.id.feed_view_type_pending_indicator;

    private final EntryFeed mFeed;
    private final LayoutInflater mInfater;
    private final int mGridColumnCount;

    private AtomicBoolean mKeepOnAppending = new AtomicBoolean(true);
    private AtomicBoolean mLoading = new AtomicBoolean(false);

    private final int mPendingResource;

    private final Handler mHandler;
    private Subscription mFeedAppendSubscription = SubscriptionHelper.empty();

    protected abstract GridEntryBase onCreateHeaderViewHolder(ViewGroup parent);
    protected abstract void onBindHeaderViewHolder(GridEntryBase holder);

    protected abstract Observable<Feed> createObservable(Long sinceEntryId);
    protected abstract void onRemoteError(Throwable e);
    public abstract void onHeaderMoved(boolean isVisible, int viewTop);

    public FeedGridItemAdapter(Context context) {
        this(context, R.layout.endless_loading_indicator);
    }

    public FeedGridItemAdapter(Context context, int pendingResource) {
        super();
        mFeed = new EntryFeed();
        mInfater = LayoutInflater.from(context);
        mGridColumnCount = context.getResources().getInteger(R.integer.live_feed_column_count);
        mHandler = new Handler();
        mPendingResource = pendingResource;
        setHasStableIds(true);
    }

    @Override
    public GridEntryBase onCreateViewHolder(ViewGroup parent, int viewType) {
        View res;
        Context context = parent.getContext();
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return onCreateHeaderViewHolder(parent);
            case VIEW_TYPE_PENDING:
                res = mInfater.inflate(mPendingResource, parent, false);
                if (res.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
                    StaggeredGridLayoutManager.LayoutParams lp = (StaggeredGridLayoutManager.LayoutParams)res.getLayoutParams();
                    lp.setFullSpan(true);
                    res.setLayoutParams(lp);
                }
                return new GridEntryPendingResource(res);
            case VIEW_TYPE_IMAGE:
                res = mInfater.inflate(R.layout.grid_feed_item_image, parent, false);
                return new GridImageEntry(context, res, guessCardWidth(parent, res));
            case VIEW_TYPE_EMBEDD:
                res = mInfater.inflate(R.layout.grid_feed_item_image, parent, false);
                return new GridEmbeddEntry(context, res, guessCardWidth(parent, res));
            case VIEW_TYPE_QUOTE:
                res = mInfater.inflate(R.layout.grid_feed_item_quote, parent, false);
                return new GridQuoteEntry(context, res, guessCardWidth(parent, res));
            case VIEW_TYPE_OTHER:
                res = mInfater.inflate(R.layout.grid_feed_item_text, parent, false);
                return new GridTextEntry(context, res, guessCardWidth(parent, res));
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public void onBindViewHolder(GridEntryBase holder, int position) {
        if (isHeaderPosition(position)) {
            onBindHeaderViewHolder(holder);
        } else if (!isPendingIndicatorPosition(position)) {
            Entry item = mFeed.get(position - 1);
            holder.bindEntry(item);

            if (!mLoading.get()
                    && mKeepOnAppending.get()
                    && !mFeed.isEmpty()
                    && position == mFeed.size()) {
                activateCacheInBackground();
            }
        }
    }

    public class GridEntryHeader2 extends GridEntryHeader {

        public GridEntryHeader2(Context context, View v) {
            super(context, v);
        }

        @Override
        public void onScrollChanged() {
            super.onScrollChanged();
            onHeaderMoved(true, itemView.getTop());
        }
    }

    @Override
    public void onViewAttachedToWindow(GridEntryBase holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().addOnScrollChangedListener((IParallaxedHeaderHolder)holder);
            onHeaderMoved(true, holder.itemView.getTop());
        }
    }

    @Override
    public void onViewDetachedFromWindow(GridEntryBase holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) holder);
            onHeaderMoved(false, 0);
        }
    }
    @Override
    public void onViewRecycled(GridEntryBase holder) {
        holder.recycle();
    }

    @Override
    public int getItemCount() {
        int size = mFeed.size() + 1;
        if (mKeepOnAppending.get() && mLoading.get()) {
            size += 1;
        }
        return size;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return VIEW_TYPE_HEADER;
        } else if (isPendingIndicatorPosition(position)) {
            return VIEW_TYPE_PENDING;
        }

        Entry item = mFeed.get(position - 1);
        if (item == null) return VIEW_TYPE_OTHER;
        if (item.isImage()) {
            return VIEW_TYPE_IMAGE;
        } else if (item.isEmbedd()) {
            return VIEW_TYPE_EMBEDD;
        } else if (item.isQuote()) {
            return VIEW_TYPE_QUOTE;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @Override
    public long getItemId(int position) {
        return position == 0 || position > mFeed.size() ?  RecyclerView.NO_ID : mFeed.get(position - 1).getId();
    }

    public Entry getItemById(long id) {
        return mFeed.findItem(id);
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mFeedAppendSubscription.unsubscribe();
    }

    public void onEventMainThread(EntryChanged update) {
        updateEntry(update.postEntry);
    }

    public void onEventMainThread(EntryRemoved event) {
        deleteEntry(event.postId);
    }

    public void setFeed(List<Entry> feed) {
        mFeed.resetItems(feed);
    }

    public void insertEntries(List<Entry> feed) {
        mFeed.insertItems(feed);
    }

    public void updateEntry(Entry entry) {
        mFeed.insertItem(entry);
    }

    public void deleteEntry(long id) {
        mFeed.deleteItem(id);
    }

    public boolean isEmpty() {
        return mFeed.isEmpty();
    }

    public List<Entry> getFeed() {
        return mFeed.getItems();
    }

    public void stopAppending() {
        setKeepOnAppending(false);
    }

    public void restartAppending() {
        setKeepOnAppending(true);
    }

    public void appendCachedData(Feed data) {
        if (data == null || data.entries.isEmpty()) {
            stopAppending();
        } else {
            insertEntries(data.entries);
        }
    }

    private int guessCardWidth(View parent, View child) {
        if (parent.getWidth() == 0) return 0;
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)child.getLayoutParams();
        int width = (parent.getWidth() -  parent.getPaddingLeft() - parent.getPaddingRight()) / mGridColumnCount
                -  lp.rightMargin - lp.leftMargin;
        // if (DBG) Log.v(TAG, "card width: " + width);
        return width;
    }

    private void activateCacheInBackground() {
        if (DBG) Log.v(TAG, "activateCacheInBackground()");
        if (!isEmpty()) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    setLoading(true);
                    mFeedAppendSubscription.unsubscribe();
                    mFeedAppendSubscription = createObservable(mFeed.get(mFeed.size() - 1).getId())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(mFeedAppendObserver);
                }
            });
        }
    }

    private void setKeepOnAppending(boolean newValue) {
        if (mKeepOnAppending.compareAndSet(!newValue, newValue)) {
            notifyDataSetChanged();
        }
    }

    private void setLoading(boolean newValue) {
        if (mLoading.compareAndSet(!newValue, newValue)) {
            notifyItemRemoved(mFeed.size() + 1);
        }
    }

    private boolean isHeaderPosition(int position) {
        return position == 0;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return position == mFeed.size() + 1;
    }

    private int getAdapterPosition(int listLocation) {
        return listLocation + 1;
    }

    private int getListLocation(int adapterPosition) {
        return adapterPosition - 1;
    }

    private final Observer<Feed> mFeedAppendObserver = new Observer<Feed>() {
        @Override
        public void onCompleted() {
            if (DBG) Log.v(TAG, "onCompleted()");
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            onRemoteError(e);
            setLoading(false);
        }

        @Override
        public void onNext(Feed feed) {
            if (DBG) Log.e(TAG, "onNext " + feed.toString());
            appendCachedData(feed);
            setLoading(false);
        }
    };

    private final class EntryFeed extends SortedList<Entry> implements SortedList.OnListChangedListener  {

        public EntryFeed() {
            super(Entry.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR);
            setListener(this);
        }

        @Override
        public long getItemId(Entry item) {
            return item.getId();
        }

        @Override
        public void onDataSetChanged() {
            notifyDataSetChanged();
            if (DBG) Log.v(TAG, "onDataSetChanged");
        }

        @Override
        public void onItemChanged(int location) {
            notifyItemChanged(getAdapterPosition(location));
            if (DBG) Log.v(TAG, "onItemChanged " + location);
        }

        @Override
        public void onItemInserted(int location) {
            notifyItemInserted(getAdapterPosition(location));
            if (DBG) Log.v(TAG, "onItemInserted " + location);
        }

        @Override
        public void onItemRemoved(int location) {
            notifyItemRemoved(getAdapterPosition(location));
            if (DBG) Log.v(TAG, "onItemRemoved " + location);
        }

        @Override
        public void onItemMoved(int fromLocation, int toLocation) {
            notifyItemMoved(getAdapterPosition(fromLocation), toLocation);
            if (DBG) Log.v(TAG, "onItemRemoved " + fromLocation + " -> " + toLocation);
        }

        @Override
        public void onItemRangeChanged(int locationStart, int itemCount) {
            notifyItemRangeChanged(getAdapterPosition(locationStart), itemCount);
            if (DBG) Log.v(TAG, "onItemRangeChanged " + locationStart + " : " + itemCount);
        }

        @Override
        public void onItemRangeInserted(int locationStart, int itemCount) {
            notifyItemRangeInserted(getAdapterPosition(locationStart), itemCount);
            if (DBG) Log.v(TAG, "onItemRangeInserted " + locationStart + " : " + itemCount);
        }

        @Override
        public void onItemRangeRemoved(int locationStart, int itemCount) {
            notifyItemRangeRemoved(getAdapterPosition(locationStart), itemCount);
            if (DBG) Log.v(TAG, "onItemRangeRemoved " + locationStart + " : " + itemCount);
        }
    }

}
