package ru.taaasty.adapters;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.events.PostRemoved;
import ru.taaasty.events.UserLikeOrCommentUpdate;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Feed;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by alexey on 31.10.14.
 */
public abstract class FeedItemAdapter extends RecyclerView.Adapter {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    private static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;
    private static final int VIEW_TYPE_IMAGE = R.id.feed_view_type_image;
    private static final int VIEW_TYPE_EMBEDD = R.id.feed_view_type_embedd;
    private static final int VIEW_TYPE_QUOTE = R.id.feed_view_type_quote;
    private static final int VIEW_TYPE_OTHER = R.id.feed_view_type_other;
    private static final int VIEW_TYPE_PENDING = R.id.feed_view_type_pending_indicator;

    private final Context mContext;
    private final List<Entry> mFeed;
    private final LayoutInflater mInfater;

    protected TlogDesign mFeedDesign;

    protected final boolean mShowUserAvatar;

    private final Set<Long> mUpdateRatingEntrySet;

    private AtomicBoolean mKeepOnAppending = new AtomicBoolean(true);
    private AtomicBoolean mLoading = new AtomicBoolean(false);

    private final int mPendingResource;

    private Subscription mFeedAppendSubscription = SubscriptionHelper.empty();

    private final Handler mHandler;

    protected abstract void initClickListeners(ListEntryBase holder);
    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);
    protected abstract void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);
    protected abstract Observable<Feed> createObservable(Long sinceEntryId);
    protected abstract void onRemoteError(Throwable e);

    public FeedItemAdapter(Context context, boolean showUserAvatar) {
        this(context, showUserAvatar, R.layout.endless_loading_indicator);
    }

    public FeedItemAdapter(Context context, boolean showUserAvatar, int pendingResource) {
        super();
        mHandler = new Handler();
        mFeed = new ArrayList<>();
        mContext = context;
        mInfater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mUpdateRatingEntrySet = new HashSet<>();
        mShowUserAvatar = showUserAvatar;
        mPendingResource = pendingResource;
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ListEntryBase holder;
        View child;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return onCreateHeaderViewHolder(parent);
            case VIEW_TYPE_PENDING:
                child = mInfater.inflate(mPendingResource, parent, false);
                return new RecyclerView.ViewHolder(child) { };
            case VIEW_TYPE_IMAGE:
                child = mInfater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListImageEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_EMBEDD:
                child = mInfater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListEmbeddEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_QUOTE:
                child = mInfater.inflate(R.layout.list_feed_item_quote, parent, false);
                holder = new ListQuoteEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_OTHER:
                child = mInfater.inflate(R.layout.list_feed_item_text, parent, false);
                holder = new ListTextEntry(mContext, child, mShowUserAvatar);
                break;
            default:
                throw new IllegalStateException();
        }
        holder.setParentWidth(parent.getWidth());
        initClickListeners(holder);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (position == getHeaderPosition()) {
            onBindHeaderViewHolder(viewHolder);
            return;
        } else if (position == getPendingIndicatorPosition()) {
            return;
        }

        if (!mLoading.get()
            && mKeepOnAppending.get()
            && !mFeed.isEmpty()
            && position == mFeed.size()) {
            activateCacheInBackground();
        }

        Entry entry = mFeed.get(position - 1);
        ((ListEntryBase)viewHolder).getEntryActionBar().setOnItemListenerEntry(entry);
        ((ListEntryBase)viewHolder).setupEntry(entry, mFeedDesign);
    }

    @Override
    public long getItemId(int position) {
        return position == 0 || position > mFeed.size() ?  RecyclerView.NO_ID : mFeed.get(position - 1).getId();
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
        if (position == getHeaderPosition()) {
            return VIEW_TYPE_HEADER;
        } else if (position == getPendingIndicatorPosition()) {
            return VIEW_TYPE_PENDING;
        }

        Entry item = mFeed.get(position-1);
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
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof ParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().addOnScrollChangedListener((ParallaxedHeaderHolder)holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof ParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((ParallaxedHeaderHolder) holder);
        }
    }


    @Nullable
    public Entry getItemById(long entryId) {
        for (Entry entry: mFeed) if (entry.getId() == entryId) return entry;
        return null;
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        mFeedAppendSubscription.unsubscribe();
    }

    public List<Entry> getFeed() {
        return Collections.unmodifiableList(mFeed);
    }

    public void setFeed(List<Entry> feed) {
        mFeed.clear();
        mFeed.addAll(feed);
        sortUniqItems();
        notifyDataSetChanged();
    }

    public void appendFeed(List<Entry> feed) {
        int position = mFeed.size() + 1;
        mFeed.addAll(feed);
        sortUniqItems();
        int positionAfter = mFeed.size() + 1;
        if (DBG && positionAfter - position != feed.size()) {
            setKeepOnAppending(false);
            throw new IllegalStateException("В добавляемых записях есть записи с id уже в списке. Скорее всего, баг");
        }
        if (positionAfter - position > 0) {
            notifyItemRangeInserted(position, positionAfter - position);
        } else {
            setKeepOnAppending(false);
        }
    }

    public void refreshItems(List<Entry> items) {
        mFeed.addAll(items);
        sortUniqItems();
        notifyDataSetChanged();
    }

    private void sortUniqItems() {
        Map<Long, Entry> map = new HashMap<>(mFeed.size());
        for (Entry c: mFeed) map.put(c.getId(), c);
        mFeed.clear();
        mFeed.addAll(map.values());
        Collections.sort(mFeed, Entry.ORDER_BY_CREATE_DATE_DESC_COMARATOR);
    }

    public boolean isEmpty() {
        return mFeed.isEmpty();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyItemRangeChanged(0, mFeed.size());
    }

    public void onUpdateRatingStart(long entryId) {
        if (mUpdateRatingEntrySet.contains(entryId)) {
            if (DBG) {
                throw new IllegalStateException();
            } else {
                return;
            }
        }
        mUpdateRatingEntrySet.add(entryId);
        for (int i = 0; i < mFeed.size(); ++i) {
            if (mFeed.get(i).getId() == entryId) notifyItemChanged(i + 1);
        }
    }

    public void onUpdateRatingEnd(long entryId) {
        mUpdateRatingEntrySet.remove(entryId);
        for (int i = 0; i < mFeed.size(); ++i) {
            if (mFeed.get(i).getId() == entryId) notifyItemChanged(i + 1);
        }
    }

    public boolean isRatingInUpdate(long entryId) {
        return mUpdateRatingEntrySet.contains(entryId);
    }

    public void updateEntry(Entry entry) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).getId() == entry.getId()) {
                mFeed.set(i, entry);
                notifyItemChanged(i + 1);
                break;
            }
        }
    }

    public void deleteEntry(long id) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).getId() == id) {
                mFeed.remove(i);
                notifyItemRemoved(i + 1);
                break;
            }
        }
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
            appendFeed(data.entries);
        }
    }

    public void onEventMainThread(UserLikeOrCommentUpdate update) {
        updateEntry(update.postEntry);
    }

    public void onEventMainThread(PostRemoved event) {
        deleteEntry(event.postId);
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

    private int getHeaderPosition() {
        return 0;
    }

    private int getPendingIndicatorPosition() {
        return mFeed.size() + 1;
    }

    private void setKeepOnAppending(boolean newValue) {
        if (mKeepOnAppending.compareAndSet(!newValue, newValue)) {
            notifyDataSetChanged();
        }
    }

    private void setLoading(boolean newValue) {
        if (mLoading.compareAndSet(!newValue, newValue)) {
            notifyDataSetChanged();
        }
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

}
