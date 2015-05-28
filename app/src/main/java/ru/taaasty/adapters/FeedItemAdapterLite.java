package ru.taaasty.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRatingStatusChanged;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.ui.feeds.FeedsHelper;

/**
 * Лента постов. Упрощенная версия без комментариев, только посты.
 */
public abstract class FeedItemAdapterLite extends RecyclerView.Adapter implements FeedsHelper.IFeedsHelper {

    public static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;
    public static final int VIEW_TYPE_IMAGE = R.id.feed_view_type_image;
    public static final int VIEW_TYPE_EMBEDD = R.id.feed_view_type_embedd;
    public static final int VIEW_TYPE_QUOTE = R.id.feed_view_type_quote;
    public static final int VIEW_TYPE_COMMENT = R.id.feed_view_type_comment;
    public static final int VIEW_TYPE_REPLY_FORM = R.id.feed_view_type_reply_form;
    public static final int VIEW_TYPE_OTHER = R.id.feed_view_type_other;
    public static final int VIEW_TYPE_PENDING = R.id.feed_view_type_pending_indicator;
    private static final int HEADERS_COUNT = 1;

    private static final String TAG = "FeedItemAdapterLite";
    private static final boolean DBG = BuildConfig.DEBUG;

    private final SortedList<Entry> mEntries;

    private final int mPendingResource;

    private AtomicBoolean mLoading = new AtomicBoolean(false);

    @Nullable
    private InteractionListener mInteractionListener;

    protected TlogDesign mFeedDesign;

    protected final boolean mShowUserAvatar;

    protected abstract boolean initClickListeners(RecyclerView.ViewHolder holder, int type);

    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);

    protected abstract void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);

    public FeedItemAdapterLite(SortedList<Entry> list, boolean showUserAvatar) {
        this(list, null, showUserAvatar, R.layout.endless_loading_indicator);
    }

    private FeedItemAdapterLite(SortedList<Entry> entries, @Nullable List<Entry> feed, boolean showUserAvatar, int pendingResource) {
        super();
        mEntries = entries != null ? entries : new EntryList();
        mShowUserAvatar = showUserAvatar;
        mPendingResource = pendingResource;
        mFeedDesign = TlogDesign.DUMMY;
        setHasStableIds(true);
        if (feed != null) mEntries.resetItems(feed);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;
        View child;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return onCreateHeaderViewHolder(parent);
            case VIEW_TYPE_PENDING:
                child = inflater.inflate(mPendingResource, parent, false);
                holder = new RecyclerView.ViewHolder(child) {};
                break;
            case VIEW_TYPE_IMAGE:
                child = inflater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListImageEntry(context, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_EMBEDD:
                child = inflater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListEmbeddEntry(context, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_QUOTE:
                child = inflater.inflate(R.layout.list_feed_item_quote, parent, false);
                holder = new ListQuoteEntry(context, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_OTHER:
                child = inflater.inflate(R.layout.list_feed_item_text, parent, false);
                holder = new ListTextEntry(context, child, mShowUserAvatar);
                break;
            default:
                throw new IllegalStateException();
        }
        if (holder instanceof ListEntryBase) ((ListEntryBase)holder).setParentWidth(parent.getWidth());

        initClickListeners(holder, viewType);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int adapterPosition) {
        if (isHeaderPosition(adapterPosition)) {
            onBindHeaderViewHolder(viewHolder);
            return;
        } else if (isPendingIndicatorPosition(adapterPosition)) {
            return;
        }
        int feedLocation = getFeedLocation(adapterPosition);
        Entry entry = mEntries.get(feedLocation);
        ((ListEntryBase) viewHolder).getEntryActionBar().setOnItemListenerEntry(entry);
        ((ListEntryBase) viewHolder).setupEntry(entry, mFeedDesign);

        if (mInteractionListener != null)
            mInteractionListener.onBindViewHolder(viewHolder,feedLocation, mEntries.size());
    }

    @Override
    public long getItemId(int position) {
        return position == 0 || position > mEntries.size() ?  RecyclerView.NO_ID : mEntries.get(getFeedLocation(position)).getId();
    }

    @Override
    public int getItemCount() {
        int size = mEntries.size() + HEADERS_COUNT;
        if (mLoading.get()) size += 1;
        return size;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return VIEW_TYPE_HEADER;
        } else if (isPendingIndicatorPosition(position)) {
            return VIEW_TYPE_PENDING;
        }

        Entry entry = mEntries.get(getFeedLocation(position));
        if (entry.isImage()) {
            return VIEW_TYPE_IMAGE;
        } else if (entry.isEmbedd()) {
            return VIEW_TYPE_EMBEDD;
        } else if (entry.isQuote()) {
            return VIEW_TYPE_QUOTE;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().addOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
        if (holder instanceof  ListImageEntry) ((ListImageEntry) holder).onAttachedToWindow();
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder && holder.itemView.getViewTreeObserver().isAlive()) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
        if (holder instanceof  ListImageEntry) ((ListImageEntry) holder).onDetachedFromWindow();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        onViewDetachedFromWindow(holder);
        if (holder instanceof  ListEntryBase) ((ListEntryBase) holder).recycle();
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy(RecyclerView parent) {
        EventBus.getDefault().unregister(this);
        mInteractionListener = null;

        // Особенности FragmentStatePagerAdapter.
        // onViewDetachedFromWindow может не вызываться при удалении активности и адаптер и фрагмент висят в памяти
        if (parent != null) {
            for (int i = 0; i < parent.getChildCount(); ++i) {
                RecyclerView.ViewHolder vh = parent.getChildViewHolder(parent.getChildAt(i));
                onViewDetachedFromWindow(vh);
            }
        }
    }

    public void setFeedDesign(TlogDesign design) {
        if (!mFeedDesign.equals(design)) {
            mFeedDesign = design;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @Nullable
    public Entry getAnyEntryAtHolderPosition(RecyclerView.ViewHolder holder) {
        return getEntry(holder.getPosition());
    }

    public Entry getEntry(int position) {
        if (!isPositionInFeed(position)) return null;
        return mEntries.get(getFeedLocation(position));
    }

    public void removeEntry(Entry entry) {
        mEntries.remove(entry);
    }

    public void addEntry(Entry entry) {
        mEntries.add(entry);
    }

    public void setInteractionListener(InteractionListener listener) {
        mInteractionListener = listener;
    }

    public void setLoading(boolean newValue) {
        if (mLoading.compareAndSet(!newValue, newValue)) {
            notifyDataSetChanged();
        }
    }

    public boolean getIsLoading() {
        return mLoading.get();
    }

    public abstract void onEventMainThread(EntryChanged update);

    public void onEventMainThread(EntryRemoved event) {
        for (int i = mEntries.size() - 1; i >= 0; --i) {
            if (mEntries.get(i).getId() == event.postId) {
                mEntries.removeItemAt(i);
                break;
            }
        }
    }

    public void onEventMainThread(EntryRatingStatusChanged event) {
        if (event.newStatus == EntryRatingStatusChanged.STATUS_START_UPDATE) {
            for (int i = mEntries.size() - 1; i >= 0; --i) {
                if (mEntries.get(i).getId() == event.entryId) {
                    notifyItemChanged(getAdapterPosition(i));
                    break;
                }
            }
        }
    }

    public SortedList<Entry> getFeed() {
        return mEntries;
    }

    protected boolean hasEntry(long id) {
        for (int i = mEntries.size() - 1; i >= 0; --i) {
            if (mEntries.get(i).getId() == id) return true;
        }
        return false;
    }

    public static int getFeedLocation(int adapterPosition) {
        return adapterPosition - HEADERS_COUNT;
    }

    public static int getAdapterPosition(int feedLocation) {
        return feedLocation + HEADERS_COUNT;
    }

    public boolean isHeaderPosition(int position) {
        return position == 0;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return position == mEntries.size() + HEADERS_COUNT;
    }

    public boolean isPositionInFeed(int position) {
        return position != RecyclerView.NO_POSITION
                && !isHeaderPosition(position)
                && !isPendingIndicatorPosition(position);
    }

    public interface InteractionListener {
        void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize);
    }


    public final class EntryList extends SortedList<Entry> {
        public EntryList() {
            super(Entry.class, new SortedList.Callback<Entry>() {

                @Override
                public int compare(Entry o1, Entry o2) {
                    return Entry.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR.compare(o1, o2);
                }

                @Override
                public boolean areContentsTheSame(Entry oldItem, Entry newItem) {
                    // TODO
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areItemsTheSame(Entry item1, Entry item2) {
                    return item1.getId() == item2.getId();
                }

                @Override
                public void onInserted(int position, int count) {
                    notifyItemRangeInserted(getAdapterPosition(position), count);
                }

                @Override
                public void onRemoved(int position, int count) {
                    notifyItemRangeRemoved(getAdapterPosition(position), count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    notifyItemMoved(getAdapterPosition(fromPosition), getAdapterPosition(toPosition));
                }

                @Override
                public void onChanged(int position, int count) {
                    notifyItemRangeChanged(getAdapterPosition(position), count);
                }
            });
        }

    }
}
