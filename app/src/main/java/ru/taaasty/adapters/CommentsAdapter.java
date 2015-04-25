package ru.taaasty.adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.Comment;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.utils.Objects;

public abstract class CommentsAdapter extends RecyclerView.Adapter {

    public static final int VIEW_TYPE_POST_HEADER = R.id.post_header;
    public static final int VIEW_TYPE_LOAD_MORE_HEADER = R.id.comments_header;
    public static final int VIEW_TYPE_COMMENT = R.id.comment;

    private final LayoutInflater mInfater;

    private final CommentsList mComments;

    private Long mSelectedCommentId;

    private final CommentViewBinder mCommentViewBinder;

    protected TlogDesign mFeedDesign;

    private boolean mShowLoadMoreHeader;

    private boolean mShowPostHeader;

    public abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType);
    public abstract void onBindHeaderHolder(RecyclerView.ViewHolder holder, int position);
    public abstract void initClickListeners(RecyclerView.ViewHolder holder, int viewType);

    public CommentsAdapter(Context context) {
        super();
        mInfater = LayoutInflater.from(context);
        mComments = new CommentsList();
        mCommentViewBinder = new CommentViewBinder();
        mFeedDesign = TlogDesign.DUMMY;
        setHasStableIds(true);
    }

    public int getPostPosition() {
        return mShowPostHeader ? 0 : -1;
    }

    public int getLoadMorePosition() {
        if (!mShowLoadMoreHeader) return -1;
        return mShowPostHeader ? 1 : 0;
    }

    public boolean isLoadMorePosition(int position) {
        return getLoadMorePosition() == position;
    }

    public boolean isPostPosition(int position) {
        return getPostPosition() == position;
    }

    @Override
    public int getItemViewType(int position) {
        if (isPostPosition(position)) {
            return VIEW_TYPE_POST_HEADER;
        } else if (isLoadMorePosition(position)) {
            return VIEW_TYPE_LOAD_MORE_HEADER;
        } else {
            return VIEW_TYPE_COMMENT;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;

        switch (viewType) {
            case VIEW_TYPE_POST_HEADER:
            case VIEW_TYPE_LOAD_MORE_HEADER:
                holder =  onCreateHeaderViewHolder(parent, viewType);
                holder.setIsRecyclable(false);
                break;
            case VIEW_TYPE_COMMENT:
                View res = mInfater.inflate(R.layout.comments_item2, parent, false);
                holder = new ViewHolder(res);
                break;
            default:
                throw  new IllegalArgumentException();
        }

        initClickListeners(holder, viewType);

        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (isLoadMorePosition(position) || isPostPosition(position)) {
            onBindHeaderHolder(holder, position);
            return;
        }

        Comment comment = mComments.get(getCommentsLocation(position));

        if (mSelectedCommentId != null && mSelectedCommentId == comment.getId()) {
            mCommentViewBinder.bindSelectedComment((ViewHolder)holder, comment, mFeedDesign);
        } else {
            mCommentViewBinder.bindNotSelectedComment((ViewHolder)holder, comment, mFeedDesign);
        }
    }

    @Override
    public long getItemId(int position) {
        if (isLoadMorePosition(position) || isPostPosition(position)) {
            return RecyclerView.NO_ID;
        } else {
            return mComments.get(getCommentsLocation(position)).getId();
        }
    }

    @Override
    public int getItemCount() {
        return mComments.size() + (mShowLoadMoreHeader ? 1 : 0) + (mShowPostHeader ? 1 : 0);
    }

    public void setComments(List<Comment> comments) {
        mComments.resetItems(comments);
    }

    public List<Comment> getComments() {
        return mComments.getItems();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        if (!mComments.isEmpty() || mShowPostHeader) notifyDataSetChanged();
    }

    public void appendComment(Comment comments) {
        mComments.add(comments);
    }

    public void appendComments(List<Comment> comments) {
        mComments.insertItems(comments);
    }

    public void deleteComment(long commentId) {
        for (int i = mComments.size() - 1; i >= 0; --i) {
            Long l = mComments.get(i).getId();
            if (mComments.get(i).getId() == commentId) {
                mComments.removeItemAt(i);
                break;
            }
        }
    }

    @Nullable
    public Long getTopCommentId() {
        if (mComments.isEmpty()) return null;
        return mComments.get(0).getId();
    }

    public boolean isLoadMoreHeaderShown() {
        return mShowLoadMoreHeader;
    }

    public void setShowLoadMoreHeader(boolean show) {
        if (show != mShowLoadMoreHeader) {
            mShowLoadMoreHeader = show;
            if (show) {
                notifyItemInserted(getLoadMorePosition());
            } else {
                notifyItemRemoved(mShowPostHeader ? 1 : 0);
            }
        }
    }

    public void setShowPost(boolean show) {
        if (show != mShowPostHeader) {
            mShowPostHeader = show;
            if (show) {
                notifyItemInserted(getPostPosition());
            } else {
                notifyItemRemoved(0);
            }
        }
    }

    public void setSelectedCommentId(Long commentId) {
        if (!Objects.equals(mSelectedCommentId, commentId)) {
            if (mSelectedCommentId != null) {
                Integer pos = findCommentPosition(mSelectedCommentId);
                if (pos != null) notifyItemChanged(pos);
            }

            if (commentId == null) {
                mSelectedCommentId = null;
            } else {
                Integer newPos = findCommentPosition(commentId);
                if (newPos != null) {
                    mSelectedCommentId = commentId;
                    notifyItemChanged(newPos);
                } else {
                    mSelectedCommentId = null;
                }
            }
        }
    }

    public void clearSelectedCommentId() {
        if (mSelectedCommentId != null) {
            Integer oldPos = findCommentPosition(mSelectedCommentId);
            mSelectedCommentId = null;
            if (oldPos != null) notifyItemChanged(oldPos);
        }
    }

    @Nullable
    public Long getCommentSelected() {
        return mSelectedCommentId;
    }

    public boolean hasComments() {
        return !mComments.isEmpty();
    }

    public int getCommentsCount() {
        return mComments.size();
    }

    public void refreshRelativeDates(RecyclerView recyclerView) {
        int childCount = recyclerView.getChildCount();
        for (int i =0; i < childCount; ++i) {
            RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(recyclerView.getChildAt(i));
            if (holder != null && holder instanceof ViewHolder) {
                ViewHolder commentHolder = (ViewHolder) holder;
                String newDate = mCommentViewBinder.getRelativeDate(recyclerView.getContext(),
                        commentHolder.updatedAtValue);
                if (!newDate.equals(commentHolder.relativeDateValue)) notifyItemChanged(holder.getPosition());
            }
        }
    }

    private int getAdapterPosition(int commentsLocation) {
        return commentsLocation + (mShowLoadMoreHeader ? 1 : 0) + (mShowPostHeader ? 1: 0);
    }

    private int getCommentsLocation(int adapterPosition) {
        return adapterPosition - (mShowLoadMoreHeader ? 1 : 0) - (mShowPostHeader ? 1 : 0);
    }

    @Nullable
    public Integer findCommentPosition(long commentId) {
        for (int i = mComments.size() - 1; i >= 0; --i) {
            if (mComments.get(i).getId() == commentId) return getAdapterPosition(i);
        }
        return null;
    }

    @Nullable
    public Comment getComment(ViewHolder holder) {
        if (holder.getAdapterPosition() < 0) return null;
        Integer location = getCommentsLocation(holder.getAdapterPosition());
        if (location == null) return null;
        return mComments.get(location);
    }

    public interface OnCommentButtonClickListener {
        void onReplyToCommentClicked(View view, ViewHolder holder);
        void onDeleteCommentClicked(View view, ViewHolder holder);
        void onReportContentClicked(View view, ViewHolder holder);
    }

    public ValueAnimator createHideButtonsAnimator(ViewHolder holder) {
        return mCommentViewBinder.createHideButtonsAnimator(holder);
    }

    public ValueAnimator createShowButtonsAnimator(ViewHolder holder) {
        Comment comment = getComment(holder);
        return mCommentViewBinder.createShowButtonsAnimator(holder, comment);
    }

    private final class CommentsList extends SortedList<Comment> {

        public CommentsList() {
            super(Comment.class, new Callback<Comment>() {
                @Override
                public int compare(Comment o1, Comment o2) {
                    return Comment.ORDER_BY_DATE_ID_COMARATOR.compare(o1, o2);
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

                @Override
                public boolean areContentsTheSame(Comment oldItem, Comment newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areItemsTheSame(Comment item1, Comment item2) {
                    return item1.getId() == item2.getId();
                }
            });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View avatarCommentRoot;
        public final ImageView avatar;
        public final TextView comment;

        long updatedAtValue;

        String relativeDateValue;

        @Nullable
        public ViewStub actionViewStub;

        @Nullable
        public View actionView;

        @Nullable
        public View replyToCommentButton;

        @Nullable
        public View deleteCommentButton;

        @Nullable
        public View reportButton;

        private OnCommentButtonClickListener mOnCommentButtonClickListener;

        public ViewHolder(View v) {
            super(v);
            avatarCommentRoot = v.findViewById(R.id.avatar_comment_root);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            comment = (TextView) v.findViewById(R.id.comment);
            actionViewStub = (ViewStub) v.findViewById(R.id.stub);
        }

        public void setOnCommentButtonClickListener(OnCommentButtonClickListener listener) {
            mOnCommentButtonClickListener = listener;
        }

        public void inflateActionViewStub() {
            if (actionViewStub != null) {
                actionView = actionViewStub.inflate();
                actionViewStub = null;
                replyToCommentButton = actionView.findViewById(R.id.reply_to_comment);
                deleteCommentButton = actionView.findViewById(R.id.delete_comment);
                reportButton = actionView.findViewById(R.id.report_to_moderator);
            }

            replyToCommentButton.setOnClickListener(mOnActionsListener);
            deleteCommentButton.setOnClickListener(mOnActionsListener);
            reportButton.setOnClickListener(mOnActionsListener);
        }

        private final View.OnClickListener mOnActionsListener = new View.OnClickListener()  {
            @Override
            public void onClick(View v) {
                if (mOnCommentButtonClickListener == null) return;

                switch (v.getId()) {
                    case R.id.reply_to_comment:
                        mOnCommentButtonClickListener.onReplyToCommentClicked(v, ViewHolder.this);
                        break;
                    case R.id.delete_comment:
                        mOnCommentButtonClickListener.onDeleteCommentClicked(v, ViewHolder.this);
                        break;
                    case R.id.report_to_moderator:
                        mOnCommentButtonClickListener.onReportContentClicked(v, ViewHolder.this);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        };
    }
}
