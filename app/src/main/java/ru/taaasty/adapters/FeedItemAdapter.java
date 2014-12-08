package ru.taaasty.adapters;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.events.CommentChanged;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.service.ApiComments;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.DeleteOrReportDialogActivity;
import ru.taaasty.ui.post.FastReplyDialogActivity;
import ru.taaasty.utils.NetworkUtils;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 *
 * TODO: хранить в data fragment'е?
 * TODO: вынести загрузчик коммментариев
 * TODO: вынести загрузчик комментариев в отдельный поток, обновлять список из этого потока, а не в главном
 * TODO: скроллить список после подгрузки комментариев
 */
public abstract class FeedItemAdapter extends RecyclerView.Adapter {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    public static final int VIEW_TYPE_HEADER = R.id.feed_view_type_header;
    public static final int VIEW_TYPE_IMAGE = R.id.feed_view_type_image;
    public static final int VIEW_TYPE_EMBEDD = R.id.feed_view_type_embedd;
    public static final int VIEW_TYPE_QUOTE = R.id.feed_view_type_quote;
    public static final int VIEW_TYPE_COMMENT = R.id.feed_view_type_comment;
    public static final int VIEW_TYPE_REPLY_FORM = R.id.feed_view_type_reply_form;
    public static final int VIEW_TYPE_OTHER = R.id.feed_view_type_other;
    public static final int VIEW_TYPE_PENDING = R.id.feed_view_type_pending_indicator;

    private static final int HEADERS_COUNT = 1;

    private final Context mContext;

    private FeedList mFeed;

    private final LayoutInflater mInflater;

    protected TlogDesign mFeedDesign;

    protected final boolean mShowUserAvatar;

    private final int mPendingResource;

    private final Set<Long> mUpdateRatingEntrySet;

    private AtomicBoolean mLoading = new AtomicBoolean(false);

    private InteractionListener mInteractionListener;

    private final CommentsLoader mCommentsLoader;

    private final CommentViewBinder mCommentViewBinder;

    /**
     * Установка click listener'ов на элементы.
     * @return true, если все было сделано и дефолтное действие не требуется
     */
    protected abstract boolean initClickListeners(RecyclerView.ViewHolder holder, int type);


    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);
    protected abstract void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder);

    public interface InteractionListener {
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize);
    }

    public FeedItemAdapter(Context context, @Nullable FeedList feed, boolean showUserAvatar) {
        this(context, feed, showUserAvatar, R.layout.endless_loading_indicator);
    }

    private FeedItemAdapter(Context context, @Nullable FeedList feed, boolean showUserAvatar, int pendingResource) {
        super();

        SortedList.OnListChangedListener onListChangedListener = new SortedList.OnListChangedListener() {

            @Override
            public void onDataSetChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onItemChanged(int location) {
                notifyItemChanged(getAdapterPosition(location));
            }

            @Override
            public void onItemInserted(int location) {
                notifyItemInserted(getAdapterPosition(location));
            }

            @Override
            public void onItemRemoved(int position) {
                notifyItemRemoved(getAdapterPosition(position));
            }

            @Override
            public void onItemMoved(int fromLocation, int toLocation) {
                notifyItemMoved(getAdapterPosition(fromLocation), getAdapterPosition(toLocation));
            }

            @Override
            public void onItemRangeChanged(int locationStart, int itemCount) {
                notifyItemRangeChanged(getAdapterPosition(locationStart), itemCount);
            }

            @Override
            public void onItemRangeInserted(int locationStart, int itemCount) {
                notifyItemRangeInserted(getAdapterPosition(locationStart), itemCount);
            }

            @Override
            public void onItemRangeRemoved(int locationStart, int itemCount) {
                notifyItemRangeRemoved(getAdapterPosition(locationStart), itemCount);
            }
        };

        mFeed = feed != null ? feed : new FeedList();
        mFeed.setListener(onListChangedListener);
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mUpdateRatingEntrySet = new HashSet<>();
        mShowUserAvatar = showUserAvatar;
        mPendingResource = pendingResource;
        mCommentViewBinder = new CommentViewBinder();
        mCommentsLoader = new CommentsLoader();
        setOnCommentButtonClickListener(mOnCommentButtonClickListener);
        setHasStableIds(false);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;
        View child;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                return onCreateHeaderViewHolder(parent);
            case VIEW_TYPE_PENDING:
                child = mInflater.inflate(mPendingResource, parent, false);
                holder = new RecyclerView.ViewHolder(child) {};
                break;
            case VIEW_TYPE_COMMENT:
                child = mInflater.inflate(R.layout.comments_item2, parent, false);
                holder = new CommentsAdapter.ViewHolder(child);
                break;
            case VIEW_TYPE_REPLY_FORM:
                child = mInflater.inflate(R.layout.list_comment_reply_form, parent, false);
                holder = new ReplyCommentFormViewHolder(child);
                break;
            case VIEW_TYPE_IMAGE:
                child = mInflater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListImageEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_EMBEDD:
                child = mInflater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListEmbeddEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_QUOTE:
                child = mInflater.inflate(R.layout.list_feed_item_quote, parent, false);
                holder = new ListQuoteEntry(mContext, child, mShowUserAvatar);
                break;
            case VIEW_TYPE_OTHER:
                child = mInflater.inflate(R.layout.list_feed_item_text, parent, false);
                holder = new ListTextEntry(mContext, child, mShowUserAvatar);
                break;
            default:
                throw new IllegalStateException();
        }
        if (holder instanceof  ListEntryBase) ((ListEntryBase)holder).setParentWidth(parent.getWidth());

        if (!initClickListeners(holder, viewType)) {
            switch (viewType) {
                case FeedItemAdapter.VIEW_TYPE_COMMENT:
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onCommentClicked((CommentsAdapter.ViewHolder)holder);
                        }
                    });
                    ((CommentsAdapter.ViewHolder)holder).avatar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Comment comment = getCommentAtHolderPosition(holder);
                            if (comment == null) return;
                            mOnCommentButtonClickListener.onAuthorAvatarClicked(v, comment);
                        }
                    });
                    break;
                case VIEW_TYPE_REPLY_FORM:
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Entry entry = getAnyEntryAtHolderPosition(holder);
                            if (entry == null) return;
                            FastReplyDialogActivity.startReplyToPost(v.getContext(), entry);
                        }
                    });
                    break;
                default:
                    break;
            }
        } else {
            if (holder instanceof ListEntryBase) {
                ((ListEntryBase) holder).mCommentLoadMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Entry entry = getAnyEntryAtHolderPosition(holder);
                        if (entry == null) return;
                        if (mCommentsLoader != null) mCommentsLoader.startLoadMoreComments(entry);
                    }
                });
            }
        }
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
        FeedListItem feedListItem = mFeed.get(feedLocation);
        if (feedListItem.isEntry()) {
            Entry entry = feedListItem.entry;
            ((ListEntryBase) viewHolder).getEntryActionBar().setOnItemListenerEntry(entry);
            ((ListEntryBase) viewHolder).setupEntry(entry, mFeedDesign);
            mCommentsLoader.onBindComment(entry, feedLocation);
            bindCommentProgressbar((ListEntryBase) viewHolder, feedListItem);
        } else if (feedListItem.isReplyForm()) {
        } else {
            bindComment((CommentsAdapter.ViewHolder) viewHolder, feedListItem.comment);
        }

        if (mInteractionListener != null)
            mInteractionListener.onBindViewHolder(viewHolder,feedLocation, mFeed.size());
    }

    @Override
    public long getItemId(int position) {
        return position == 0 || position > mFeed.size() ?  RecyclerView.NO_ID : getFeedItem(position).getId();
    }

    @Override
    public int getItemCount() {
        int size = mFeed.size() + HEADERS_COUNT;
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

        FeedListItem item = getFeedItem(position);
        if (item == null) return VIEW_TYPE_OTHER;
        if (item.isComment()) {
            return VIEW_TYPE_COMMENT;
        } else if (item.isReplyForm()) {
            return VIEW_TYPE_REPLY_FORM;
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

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        if (mCommentsLoader != null) mCommentsLoader.onDestroy();
    }

    public void setOnCommentButtonClickListener(CommentsAdapter.OnCommentButtonClickListener listener) {
        if (mCommentViewBinder == null) throw new IllegalStateException();
        mCommentViewBinder.setOnCommentButtonClickListener(listener);
    }

    public FeedList getFeed() {
        return mFeed;
    }

    /**
     * Entry в заданной позиции, независимо от типа элемента в этой позиции
     */
    @Nullable
    public Entry getAnyEntryAtHolderPosition(RecyclerView.ViewHolder holder) {
        int position = holder.getPosition();
        if (!isPositionInFeed(position)) return null;
        return mFeed.getAnyEntry(getFeedLocation(position));
    }

    /**
     * Комментарий в заданной позиции, либо null
     */
    @Nullable
    public Comment getCommentAtHolderPosition(RecyclerView.ViewHolder holder) {
        int position = holder.getPosition();
        if (!isPositionInFeed(position)) return null;
        return mFeed.getCommentAtLocation(getFeedLocation(position));
    }

    /**
     * Возвращает Entry в позиции. null - если там не entry
     * @param position позиция
     */
    public Entry getAnyEntryAtPosition(int position) {
        if (!isPositionInFeed(position)) return null;
        return mFeed.getAnyEntry(getFeedLocation(position));
    }

    /**
     * Установка "Ещё XXX комментариев" над списком комментариев
     * @param holder
     * @param feedListItem
     */
    private void bindCommentProgressbar(ListEntryBase holder, FeedListItem feedListItem) {
        long entryId = feedListItem.entry.getId();
        boolean isLoading = mCommentsLoader.isCommentsLoading(entryId);
        int commentsCount = feedListItem.entry.getCommentsCount();
        int commentsShown = mFeed.getCommentsCount(entryId);
        if (commentsCount < commentsShown) {
            // Скорее всего, уведомление об изменении статьи пришло раньше, чем уведомление об удалении комментария.
            // В этом случае, ничего страшного, но на всякий случай, корректируем
            commentsCount = commentsShown;
        }
        holder.setupCommentStatus(isLoading, commentsCount, commentsShown);
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyItemRangeChanged(0, mFeed.size());
    }

    void onCommentClicked(CommentsAdapter.ViewHolder holder) {
        // TODO: делать анимации при смене статуса коммментария в другом месте
        final Comment comment = getCommentAtHolderPosition(holder);
        if (comment == null) return;
        FeedList feed = getFeed();
        if (feed.getSelectedCommentId() != null
                && feed.getSelectedCommentId() == comment.getId()) {
            feed.setSelectedCommentId(null);
        } else {
            if (feed.isEmpty()) return;
            feed.setSelectedCommentId(null);

            ValueAnimator va = createShowCommentButtonsAnimator(holder);
            va.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    getFeed().setSelectedCommentId(comment.getId());
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            va.start();
        }
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
            if (mFeed.isEntryAtLocation(i, entryId)) {
                notifyItemChanged(getAdapterPosition(i));
                break;
            }
        }
    }

    public void onUpdateRatingEnd(long entryId) {
        mUpdateRatingEntrySet.remove(entryId);
        for (int i = 0; i < mFeed.size(); ++i) {
            if (mFeed.isEntryAtLocation(i, entryId)) {
                notifyItemChanged(getAdapterPosition(i));
                break;
            }
        }
    }

    public boolean isRatingInUpdate(long entryId) {
        return mUpdateRatingEntrySet.contains(entryId);
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

    public void onEventMainThread(EntryChanged update) {
        mFeed.updateEntry(update.postEntry);
    }

    public void onEventMainThread(CommentRemoved event) {
        mFeed.deleteComment(event.commentId);
    }

    public void onEventMainThread(EntryRemoved event) {
        mFeed.deleteEntry(event.postId);
    }

    public void onEventMainThread(CommentChanged event) {
        // Это скорее всего возврат из ответа на комментарий. Снимаем выделение.
        mFeed.setSelectedCommentId(null);
        mFeed.addComments(event.entry.getId(), Collections.singletonList(event.comment));
    }

    protected ValueAnimator createHideCommentButtonsAnimator(CommentsAdapter.ViewHolder holder) {
        return mCommentViewBinder.createHideButtonsAnimator(holder);
    }

    protected ValueAnimator createShowCommentButtonsAnimator(CommentsAdapter.ViewHolder holder) {
        return mCommentViewBinder.createShowButtonsAnimator(holder);
    }

    private FeedListItem getFeedItem(int adapterPosition) {
        return mFeed.get(getFeedLocation(adapterPosition));
    }

    /**
     * Возвращает позиция в фиде по позиции в адаптере (это позиция без учета хидеров)
     * @param adapterPosition
     */
    private int getFeedLocation(int adapterPosition) {
        return adapterPosition - HEADERS_COUNT;
    }

    private int getAdapterPosition(int feedLocation) {
        return feedLocation + HEADERS_COUNT;
    }

    private boolean isHeaderPosition(int position) {
        return position == 0;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return position == mFeed.size() + HEADERS_COUNT;
    }

    private boolean isPositionInFeed(int position) {
        return position != RecyclerView.NO_POSITION
            && !isHeaderPosition(position)
                && !isPendingIndicatorPosition(position);
    }

    private void bindComment(CommentsAdapter.ViewHolder holder, Comment comment) {
        Long selectedCommentId = mFeed.getSelectedCommentId();
        if (selectedCommentId != null && selectedCommentId == comment.getId()) {
            mCommentViewBinder.bindSelectedComment(holder, comment, mFeedDesign);
        } else {
            mCommentViewBinder.bindNotSelectedComment(holder, comment, mFeedDesign);
        }
    }

    private final CommentsAdapter.OnCommentButtonClickListener mOnCommentButtonClickListener = new CommentsAdapter.OnCommentButtonClickListener() {

        @Override
        public void onReplyToCommentClicked(View view, Comment comment) {
            Integer location = getFeed().findCommentLocation(comment.getId());
            if (location == null) return;
            FastReplyDialogActivity.startReplyToComment(view.getContext(), getFeed().getAnyEntry(location), comment);
        }

        @Override
        public void onDeleteCommentClicked(View view, Comment comment) {
            int location = getFeed().findCommentLocation(comment.getId());
            DeleteOrReportDialogActivity.startDeleteComment(view.getContext(),
                    mFeed.get(location).entry.getId(),
                    comment.getId());
        }

        @Override
        public void onReportContentClicked(View view, Comment comment) {
            DeleteOrReportDialogActivity.startReportComment(view.getContext(), comment.getId());
        }

        @Override
        public void onAuthorAvatarClicked(View view, Comment comment) {
            TlogActivity.startTlogActivity(view.getContext(), comment.getAuthor().getId(), view);
        }
    };

    /**
     * Загрузчик комментариев для постов
     */
    private final class CommentsLoader {

        public static final int MAX_QUEUE_SIZE = 3;
        private final LongSparseArray<Subscription> mLoadCommentsSubscriptions;

        private final LongSparseArray<ArrayList<Comment>> mComments;

        private final ApiComments mApiComments;

        public CommentsLoader() {
            mLoadCommentsSubscriptions = new LongSparseArray<>();
            mComments = new LongSparseArray<>();
            mApiComments = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        }

        public void onDestroy() {
            for (int i = 0; i < mLoadCommentsSubscriptions.size(); i++) {
                mLoadCommentsSubscriptions.valueAt(i).unsubscribe();
            }
            mLoadCommentsSubscriptions.clear();
        }

        public boolean isCommentsLoading(long entryId) {
            return mLoadCommentsSubscriptions.get(entryId) != null;
        }

        public void startLoadMoreComments(Entry entry) {
            long entryId;
            Long topCommentId;
            List<Comment> comments;

            entryId = entry.getId();
            comments = mComments.get(entryId);
            if (mLoadCommentsSubscriptions.get(entryId) != null) return;
            if (comments == null || comments.isEmpty()) {
                topCommentId = null;
            } else {
                topCommentId = comments.get(0).getId();
            }
            startLoad(entryId, topCommentId, Constants.SHOW_POST_COMMENTS_COUNT_LOAD_STEP);
            int location = getFeed().findEntryLocation(entryId);
            notifyItemChanged(getAdapterPosition(location));
        }

        public void onBindComment(Entry entry, int feedLocation) {
            // Обязательно запускаем подгрузку комментариев для текущего поста
            if (isCommentLoadRequired(entry)) startLoad(entry);

            // Если есть возможность, загружаем ещё 3 поста
            int queueSize = mLoadCommentsSubscriptions.size();
            int maxCount = Math.min(MAX_QUEUE_SIZE - queueSize, 3);
            if (maxCount <= 0) return;

            int feedSize = mFeed.size();
            int queued = 0;
            // Запускаем подгрузку комментариев для следующих постов, пока не заполним очередь
            for (int i = feedLocation + 1; i < feedSize; ++i) {
                FeedListItem item = mFeed.get(i);
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

            if (DBG) Log.v(TAG, "load comments queue: " + mLoadCommentsSubscriptions.size());
        }

        private boolean isCommentLoadRequired(Entry entry) {
            return entry.getCommentsCount() != 0
                    && mComments.get(entry.getId()) == null
                    && !isCommentsLoading(entry.getId());
        }

        private void startLoad(Entry entry) {
            // Выбираем количество предзагружаеых комментариев так, чтобы
            // не показывать "загрузить ещё 1 комментарий"
            int preloadLimit = entry.getCommentsCount() <= 5 ? 5 : 3;
            startLoad(entry.getId(), null, preloadLimit);
        }

        private void startLoad(long entryId, Long topCommentId, int limit) {
            Subscription s;
            s = mApiComments.getComments(entryId, null, topCommentId, ApiComments.ORDER_DESC, limit)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CommentsSubscriber(entryId));
            mLoadCommentsSubscriptions.put(entryId, s);
        }

        public class CommentsSubscriber extends Subscriber<Comments> {

            public final long entryId;

            public CommentsSubscriber(long entryId) {
                this.entryId = entryId;
            }

            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "load comments error", e);
            }

            @Override
            public void onNext(Comments commentsReply) {
                Subscription s = mLoadCommentsSubscriptions.get(entryId);
                mLoadCommentsSubscriptions.remove(entryId);
                s.unsubscribe();
                mFeed.addComments(entryId, commentsReply);

                // Обновляем локальный comments
                ArrayList<Comment> list = mComments.get(entryId);
                if (list == null) {
                    list = new ArrayList<>(commentsReply.comments.size());
                    mComments.put(entryId, list);
                }
                if (!commentsReply.comments.isEmpty()) {
                    list.addAll(commentsReply.comments);
                    Collections.sort(list, Comment.ORDER_BY_DATE_ID_COMARATOR);
                }
            }
        }
    }

    public static class ReplyCommentFormViewHolder extends RecyclerView.ViewHolder {

        public ReplyCommentFormViewHolder(View itemView) {
            super(itemView);
        }
    }

}