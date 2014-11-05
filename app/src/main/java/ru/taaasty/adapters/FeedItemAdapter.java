package ru.taaasty.adapters;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.PostRemoved;
import ru.taaasty.events.UserLikeOrCommentUpdate;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.service.ApiComments;
import ru.taaasty.utils.NetworkUtils;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by alexey on 31.10.14.
 */
public abstract class FeedItemAdapter extends RecyclerView.Adapter {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    public static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;
    public static final int VIEW_TYPE_IMAGE = R.id.feed_view_type_image;
    public static final int VIEW_TYPE_EMBEDD = R.id.feed_view_type_embedd;
    public static final int VIEW_TYPE_QUOTE = R.id.feed_view_type_quote;
    public static final int VIEW_TYPE_COMMENT = R.id.feed_view_type_comment;
    public static final int VIEW_TYPE_OTHER = R.id.feed_view_type_other;
    public static final int VIEW_TYPE_PENDING = R.id.feed_view_type_pending_indicator;

    private final Context mContext;
    private final List<EntryOrComment> mFeed;
    private final LayoutInflater mInfater;

    protected TlogDesign mFeedDesign;

    protected final boolean mShowUserAvatar;

    private final int mPendingResource;

    private final boolean mShowComments;

    private final Set<Long> mUpdateRatingEntrySet;

    private AtomicBoolean mLoading = new AtomicBoolean(false);

    private InteractionListener mInteractionListener;

    private final CommentsLoader mCommentsLoader;

    private final CommentViewBinder mCommentViewBinder;

    protected abstract void initClickListeners(RecyclerView.ViewHolder holder, int type);
    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);
    protected abstract void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);

    public interface InteractionListener {
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize, EntryOrComment entry);
    }

    public FeedItemAdapter(Context context, boolean showComments, boolean showUserAvatar) {
        this(context, showUserAvatar, showComments, R.layout.endless_loading_indicator);
    }

    public FeedItemAdapter(Context context, boolean showUserAvatar) {
        this(context, showUserAvatar, false, R.layout.endless_loading_indicator);
    }

    public FeedItemAdapter(Context context, boolean showUserAvatar, boolean showComments, int pendingResource) {
        super();
        mFeed = new ArrayList<>();
        mContext = context;
        mInfater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mUpdateRatingEntrySet = new HashSet<>();
        mShowUserAvatar = showUserAvatar;
        mPendingResource = pendingResource;
        mShowComments = showComments;
        if (showComments) {
            mCommentViewBinder = new CommentViewBinder();
            mCommentsLoader = new CommentsLoader();
        } else {
            mCommentsLoader = null;
            mCommentViewBinder = null;
        }
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        View child;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return onCreateHeaderViewHolder(parent);
            case VIEW_TYPE_PENDING:
                child = mInfater.inflate(mPendingResource, parent, false);
                holder = new RecyclerView.ViewHolder(child) {};
                break;
            case VIEW_TYPE_COMMENT:
                child = mInfater.inflate(R.layout.comments_item2, parent, false);
                holder = new CommentsAdapter.ViewHolder(child);
                break;
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
        if (holder instanceof  ListEntryBase) ((ListEntryBase)holder).setParentWidth(parent.getWidth());
        initClickListeners(holder, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (isHeaderPosition(position)) {
            onBindHeaderViewHolder(viewHolder);
            return;
        } else if (isPendingIndicatorPosition(position)) {
            return;
        }
        EntryOrComment entryOrComment = mFeed.get(position - 1);
        if (entryOrComment.isEntry()) {
            Entry entry = entryOrComment.entry;
            ((ListEntryBase) viewHolder).getEntryActionBar().setOnItemListenerEntry(entry);
            ((ListEntryBase) viewHolder).setupEntry(entry, mFeedDesign);
            if (mShowComments) {
                mCommentsLoader.onBindComment(entry, position - 1);
                bindCommentProgressbar((ListEntryBase) viewHolder, entryOrComment);
            }
        } else {
            bindComment((CommentsAdapter.ViewHolder) viewHolder, entryOrComment.comment);
        }

        if (mInteractionListener != null)
            mInteractionListener.onBindViewHolder(viewHolder, position - 1, mFeed.size(), entryOrComment);
    }

    @Override
    public long getItemId(int position) {
        return position == 0 || position > mFeed.size() ?  RecyclerView.NO_ID : mFeed.get(position - 1).getId();
    }

    @Override
    public int getItemCount() {
        int size = mFeed.size() + 1;
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

        EntryOrComment item = mFeed.get(position-1);
        if (item == null) return VIEW_TYPE_OTHER;
        if (item.isComment()) {
            return VIEW_TYPE_COMMENT;
        } else if (item.entry.isImage()) {
            return VIEW_TYPE_IMAGE;
        } else if (item.entry.isEmbedd()) {
            return VIEW_TYPE_EMBEDD;
        } else if (item.entry.isQuote()) {
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
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
    }

    @Nullable
    public EntryOrComment getItemById(long entryId) {
        for (EntryOrComment entry: mFeed) if (entry.getId() == entryId) return entry;
        return null;
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (mCommentsLoader != null) mCommentsLoader.onDestroy();
    }

    public List<EntryOrComment> getFeed() {
        return Collections.unmodifiableList(mFeed);
    }

    public void setFeed(List<Entry> feed) {
        EntryOrComment newFeed[] = new EntryOrComment[feed.size()];
        int size = feed.size();
        for (int i=0; i<size; ++i) {
            newFeed[i] = EntryOrComment.createEntry(feed.get(i));
        }
        setEntriesAndComments(Arrays.asList(newFeed));
    }

    public void setEntriesAndComments(List<EntryOrComment> feed) {
        mFeed.clear();
        mFeed.addAll(feed);
        sortUniqItems();
        notifyDataSetChanged();
    }

    public boolean appendEntriesAndComments(List<EntryOrComment> feed) {
        boolean successful = true;
        int position = mFeed.size() + 1;
        mFeed.addAll(feed);
        sortUniqItems();
        int positionAfter = mFeed.size() + 1;
        if (DBG && positionAfter - position != feed.size()) {
            throw new IllegalStateException("В добавляемых записях есть записи с id уже в списке. Скорее всего, баг");
        }
        if (positionAfter - position > 0) {
            notifyItemRangeInserted(position, positionAfter - position);
        } else {
            successful = false;
        }
        return successful;
    }

    public boolean appendFeed(List<Entry> feed) {
        return appendEntriesAndComments(wrapEntries(feed));
    }

    public void refreshItems(List<Entry> items) {
        refreshEntriesAndComments(wrapEntries(items));
    }

    public void addComments(Entry entry, Comments comments) {
        // TODO: более умный вариант
        for (Comment comment: comments.comments) {
            mFeed.add(EntryOrComment.createComment(entry, comment));
        }
        sortUniqItems();
        notifyDataSetChanged();
    }

    public int getCommentsCounts(long entryId) {
        int count = 0;
        for (EntryOrComment item: mFeed) if (item.isComment() && item.entry.getId() == entryId) count += 1;
        return count;
    }

    public void refreshEntriesAndComments(List<EntryOrComment> items) {
        // TODO: работает неверно для комментариев
        mFeed.addAll(items);
        sortUniqItems();
        sortUniqItems();
        notifyDataSetChanged();
    }

    private void sortUniqItems() {
        Map<Long, EntryOrComment> map = new HashMap<>(mFeed.size());
        for (EntryOrComment c: mFeed) map.put(c.getId(), c);
        mFeed.clear();
        mFeed.addAll(map.values());
        Collections.sort(mFeed, EntryOrComment.ORDER_BY_ENTRY_COMMENT_CREATE_DATE_DESC_COMARATOR);
    }

    private void bindCommentProgressbar(ListEntryBase holder, EntryOrComment entryOrComment) {
        if (!mShowComments) return;
        long entryId = entryOrComment.entry.getId();
        boolean isLoading = mCommentsLoader.isCommentsLoading(entryId);
        int commentsShown = getCommentsCounts(entryId);
        holder.setupCommentStatus(isLoading, entryOrComment.entry.getCommentsCount(), commentsShown);
    }

    public boolean isEmpty() {
        return mFeed.isEmpty();
    }

    /**
     * Entry по позиции. null, если в этой позиции не entry
     * @param position
     * @return Entry
     */
    @Nullable
    public EntryOrComment getEntryAtPosition(int position) {
        if (position > 0 && position <= mFeed.size()) {
            return mFeed.get(position - 1);
        } else {
            return null;
        }
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

    public void updateEntry(EntryOrComment entry) {
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
            // TODO: удалить все комментарии
            if (mFeed.get(i).getId() == id) {
                mFeed.remove(i);
                notifyItemRemoved(i + 1);
                break;
            }
        }
    }

    public void deleteComment(long commentId) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).isComment() && mFeed.get(i).comment.getId() == commentId) {
                long commentEntryId = mFeed.get(i).entry.getId();
                mFeed.remove(i);
                notifyItemRemoved(i + 1);
                for (int j = i; j >= 0; i--) {
                    if (mFeed.get(i).isEntry() && mFeed.get(i).entry.getId() == commentEntryId) {
                        notifyItemChanged(j + 1);
                        break;
                    }
                }
                break;
            }
        }
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

    @Nullable
    public Long getLastEntryId() {
        if (mFeed.isEmpty()) return null;
        return mFeed.get(mFeed.size() - 1).getId();
    }

    public void onEventMainThread(UserLikeOrCommentUpdate update) {
        updateEntry(EntryOrComment.createEntry(update.postEntry));
    }

    public void onEventMainThread(CommentRemoved event) {
        if (!mShowComments) return;
        deleteComment(event.commentId);
    }

    public void onEventMainThread(PostRemoved event) {
        deleteEntry(event.postId);
    }

    private boolean isHeaderPosition(int position) {
        return position == 0;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return position == mFeed.size() + 1;
    }

    private List<EntryOrComment> wrapEntries(List<Entry> feed) {
        EntryOrComment newFeed[] = new EntryOrComment[feed.size()];
        int size = feed.size();
        for (int i=0; i<size; ++i) {
            newFeed[i] = EntryOrComment.createEntry(feed.get(i));
        }
        return Arrays.asList(newFeed);
    }

    private void bindComment(CommentsAdapter.ViewHolder holder, Comment comment) {
        mCommentViewBinder.bindNotSelectedComment(holder, comment, mFeedDesign);
    }

    /**
     * Загрузчик комментариев для постов
     */
    private final class CommentsLoader {

        public static final int MAX_QUEUE_SIZE = 3;
        private final LongSparseArray<Subscription> mLoadCommensSubscriptions;

        private final LongSparseArray<Comments> mComments;

        private final ApiComments mApiComments;

        public CommentsLoader() {
            mLoadCommensSubscriptions = new LongSparseArray<>();
            mComments = new LongSparseArray<>();
            mApiComments = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        }

        public void onDestroy() {
            for (int i = 0; i < mLoadCommensSubscriptions.size(); i++) {
                mLoadCommensSubscriptions.valueAt(i).unsubscribe();
            }
            mLoadCommensSubscriptions.clear();
        }

        public boolean isCommentsLoading(long entryId) {
            return mLoadCommensSubscriptions.get(entryId) != null;
        }

        public void onBindComment(Entry entry, int feedListPosition) {
            if (isCommentLoadRequired(entry)) startLoad(entry);

            // Если есть возможность, загружаем ещё 3 поста
            int queueSize = mLoadCommensSubscriptions.size();
            int maxCount = Math.min(MAX_QUEUE_SIZE - queueSize, 3);
            if (maxCount <= 0) return;

            int feedSize = mFeed.size();
            int queued = 0;
            for (int i = feedListPosition + 1; i < feedSize; ++i) {
                EntryOrComment item = mFeed.get(i);
                if (item.isEntry()) {
                    if (isCommentLoadRequired(item.entry)) {
                        startLoad(item.entry);
                    }
                    if (isCommentsLoading(item.entry.getId())) {
                        // Считаем и только что начатые загрузки и те, что уже в процессе.
                        // Иначе слишком быстро загружаем все комменты
                        queued += 1;
                        if (queued == maxCount) break;
                    }
                }
            }

            if (DBG) Log.v(TAG, "load comments queue: " + mLoadCommensSubscriptions.size());
        }

        private boolean isCommentLoadRequired(Entry entry) {
            if (entry.getCommentsCount() == 0) return false;
            if (mComments.get(entry.getId()) != null) return false;
            if (isCommentsLoading(entry.getId())) return false;
            return true;
        }

        private void startLoad(Entry entry) {
            Subscription s = mApiComments.getComments(entry.getId(), null, null, null, 3)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CommentsSubscriber(entry));
            mLoadCommensSubscriptions.put(entry.getId(), s);
        }

        public class CommentsSubscriber extends Subscriber<Comments> {

            public final Entry entry;

            public CommentsSubscriber(Entry entry) {
                this.entry = entry;
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "load comments error", e);
            }

            @Override
            public void onNext(Comments comments) {
                Subscription s = mLoadCommensSubscriptions.get(entry.getId());
                mLoadCommensSubscriptions.remove(entry.getId());
                s.unsubscribe();
                mComments.put(entry.getId(), comments);
                addComments(entry, comments);
            }
        }
    }

    public static class EntryOrComment implements Parcelable {

        public final Entry entry;

        public final Comment comment;

        /**
         * TODO: покрыть тестами
         */
        public static Comparator<EntryOrComment> ORDER_BY_ENTRY_COMMENT_CREATE_DATE_DESC_COMARATOR = new Comparator<EntryOrComment>() {
            @Override
            public int compare(EntryOrComment lhs, EntryOrComment rhs) {
                if (lhs == null && rhs == null) {
                    return 0;
                } else if (lhs == null) {
                    return -1;
                } else if (rhs == null) {
                    return 1;
                }

                // В любом случае сравниваем сначала статьи
                int compareEntries = Entry.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR.compare(lhs.entry, rhs.entry);
                if (compareEntries != 0) return compareEntries;

                if (rhs.isEntry() && lhs.isEntry()) {
                    // Статья и статья
                    return 0;
                }

                if (lhs.isEntry()) {
                    // Статья и комментарий этой статьи
                    return -1;
                }

                if (rhs.isEntry()) {
                    // Комментарий статьи и сама статья
                    return 1;
                }

                // Комментарий и комментарий одной статьи
                return Comment.ORDER_BY_DATE_ID_COMARATOR.compare(lhs.comment, rhs.comment);

            }
        };

        public static EntryOrComment createEntry(Entry entry) {
            return new EntryOrComment(entry, null);
        }

        public static EntryOrComment createComment(Entry entry, Comment comment) {
            return new EntryOrComment(entry, comment);
        }

        private EntryOrComment(Entry entry, Comment comment) {
            this.entry = entry;
            this.comment = comment;
        }

        public boolean isEntry() {
            return comment == null;
        }

        public boolean isComment() {
            return comment != null;
        }

        public long getId() {
            // Это бздец полный, но да ладно
            return isEntry() ? entry.getId() : -1 * comment.getId();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(this.entry, 0);
            dest.writeParcelable(this.comment, 0);
        }

        private EntryOrComment(Parcel in) {
            this.entry = in.readParcelable(Entry.class.getClassLoader());
            this.comment = in.readParcelable(Comment.class.getClassLoader());
        }

        public static final Parcelable.Creator<EntryOrComment> CREATOR = new Parcelable.Creator<EntryOrComment>() {
            public EntryOrComment createFromParcel(Parcel source) {
                return new EntryOrComment(source);
            }

            public EntryOrComment[] newArray(int size) {
                return new EntryOrComment[size];
            }
        };
    }

}