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

    public static final int VIEW_TYPE_HEADER = R.id.comments_header;
    public static final int VIEW_TYPE_COMMENT = R.id.comment;

    private final LayoutInflater mInfater;

    private final CommentsList mComments;

    private Long mSelectedCommentId;

    private final CommentViewBinder mCommentViewBinder;

    private TlogDesign mFeedDesign;

    private boolean mShowHeader;

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

    @Override
    public int getItemViewType(int position) {
        if (mShowHeader && position == 0) return VIEW_TYPE_HEADER;
        return VIEW_TYPE_COMMENT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RecyclerView.ViewHolder holder;

        switch (viewType) {
            case VIEW_TYPE_HEADER:
                holder =  onCreateHeaderViewHolder(parent, viewType);
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
        if (mShowHeader && position == 0) {
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
        if (mShowHeader && position == 0) {
            return RecyclerView.NO_ID;
        } else {
            return mComments.get(getCommentsLocation(position)).getId();
        }
    }

    @Override
    public int getItemCount() {
        return mComments.size() + (mShowHeader ? 1 : 0);
    }

    public void setComments(List<Comment> comments) {
        mComments.resetItems(comments);
    }

    public List<Comment> getComments() {
        return mComments.getItems();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        if (!mComments.isEmpty()) notifyDataSetChanged();
    }

    public void appendComments(List<Comment> comments) {
        mComments.insertItems(comments);
    }

    public void deleteComment(long commentId) {
        mComments.deleteItem(commentId);
    }

    @Nullable
    public Long getTopCommentId() {
        if (mComments.isEmpty()) return null;
        return mComments.get(0).getId();
    }

    public boolean isHeaderShown() {
        return mShowHeader;
    }

    public void setShowHeader(boolean show) {
        if (show != mShowHeader) {
            mShowHeader = show;
            if (show) {
                notifyItemInserted(0);
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

    private int getAdapterPosition(int commentsLocation) {
        return commentsLocation + (mShowHeader ? 1 : 0);
    }

    private int getCommentsLocation(int adapterPosition) {
        return adapterPosition - (mShowHeader ? 1 : 0);
    }

    @Nullable
    private Integer findCommentPosition(long commentId) {
        Integer pos = mComments.findLocation(commentId);
        return pos == null ? null : getAdapterPosition(pos);
    }

    @Nullable
    public Comment getComment(ViewHolder holder) {
        if (holder.getPosition() < 0) return null;
        Integer location = getCommentsLocation(holder.getPosition());
        if (location == null) return null;
        return mComments.get(location);
    }

    public interface OnCommentButtonClickListener {
        public void onReplyToCommentClicked(View view, ViewHolder holder);
        public void onDeleteCommentClicked(View view, ViewHolder holder);
        public void onReportContentClicked(View view, ViewHolder holder);
    }

    public ValueAnimator createHideButtonsAnimator(ViewHolder holder) {
        return mCommentViewBinder.createHideButtonsAnimator(holder);
    }

    public ValueAnimator createShowButtonsAnimator(ViewHolder holder) {
        Comment comment = getComment(holder);
        return mCommentViewBinder.createShowButtonsAnimator(holder, comment);
    }

    private final class CommentsList extends SortedList<Comment> implements SortedList.OnListChangedListener {

        public CommentsList() {
            super(Comment.ORDER_BY_DATE_ID_COMARATOR);
            setListener(this);
        }

        @Override
        public long getItemId(Comment item) {
            return item.getId();
        }

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
        public void onItemRemoved(int location) {
            notifyItemRemoved(getAdapterPosition(location));
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
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final View avatarCommentRoot;
        public final ImageView avatar;
        public final TextView comment;

        @Nullable
        public final TextView date;

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
            date = (TextView) v.findViewById(R.id.date_relative);
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
