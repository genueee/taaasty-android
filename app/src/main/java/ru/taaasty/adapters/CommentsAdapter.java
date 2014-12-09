package ru.taaasty.adapters;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.Comment;
import ru.taaasty.model.TlogDesign;

public class CommentsAdapter extends BaseAdapter {
    private final LayoutInflater mInfater;

    private final CommentsList mComments;

    private Long mSelectedCommentId;

    private final CommentViewBinder mCommentViewBinder;

    private TlogDesign mFeedDesign;

    private final OnCommentButtonClickListener mListener;

    public CommentsAdapter(Context context, OnCommentButtonClickListener listener) {
        super();
        mInfater = LayoutInflater.from(context);
        mComments = new CommentsList();
        mListener = listener;
        mCommentViewBinder = new CommentViewBinder();
        mCommentViewBinder.setOnCommentButtonClickListener(listener);
        mFeedDesign = TlogDesign.DUMMY;
    }

    public void setComments(List<Comment> comments) {
        mComments.resetItems(comments);
    }

    public List<Comment> getComments() {
        return mComments.getItems();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        if (!isEmpty()) notifyDataSetChanged();
    }

    public void appendComments(List<Comment> comments) {
        mComments.insertItems(comments);
    }

    public void deleteComment(long commentId) {
        mComments.deleteItem(commentId);
    }

    public Long getTopCommentId() {
        if (mComments.isEmpty()) return null;
        return mComments.get(0).getId();
    }

    public void setSelectedCommentId(Long commentId) {
        mSelectedCommentId = commentId;
        notifyDataSetChanged();
    }

    public void clearSelectedCommentId() {
        if (mSelectedCommentId != null) {
            mSelectedCommentId = null;
            notifyDataSetChanged();
        }
    }

    @Nullable
    public Long getCommentSelected() {
        return mSelectedCommentId;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public Comment getItem(int position) {
        return mComments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mComments.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.comments_item2, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.comment_view_holder, vh);
            vh.avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        mListener.onAuthorAvatarClicked(v, vh.currentComment);
                    }
                }
            });
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.comment_view_holder);
        }

        Comment comment = mComments.get(position);

        if (mSelectedCommentId != null && mSelectedCommentId == comment.getId()) {
            mCommentViewBinder.bindSelectedComment(vh, comment, mFeedDesign);
        } else {
            mCommentViewBinder.bindNotSelectedComment(vh, comment, mFeedDesign);
        }

        return res;
    }

    public interface OnCommentButtonClickListener {
        public void onReplyToCommentClicked(View view, Comment comment);
        public void onDeleteCommentClicked(View view, Comment comment);
        public void onReportContentClicked(View view, Comment comment);
        public void onAuthorAvatarClicked(View view, Comment comment);
    }

    private static class ActionViewClickListener implements View.OnClickListener {
        private Comment mComment;
        private OnCommentButtonClickListener mListener;

        public ActionViewClickListener() {
        }

        public void setListener(OnCommentButtonClickListener listener) {
            mListener = listener;
        }

        public void setComment(Comment comment) {
            mComment = comment;
        }

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.reply_to_comment:
                    if (mListener != null) mListener.onReplyToCommentClicked(v, mComment);
                    break;
                case R.id.delete_comment:
                    if (mListener != null) mListener.onDeleteCommentClicked(v, mComment);
                    break;
                case R.id.report_to_moderator:
                    if (mListener != null) mListener.onReportContentClicked(v, mComment);
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    public ValueAnimator createHideButtonsAnimator(View view) {
        final CommentsAdapter.ViewHolder vh = (CommentsAdapter.ViewHolder) view.getTag(R.id.comment_view_holder);
        return mCommentViewBinder.createHideButtonsAnimator(vh);
    }

    public ValueAnimator createShowButtonsAnimator(View view) {
        final CommentsAdapter.ViewHolder vh = (CommentsAdapter.ViewHolder) view.getTag(R.id.comment_view_holder);
        return mCommentViewBinder.createShowButtonsAnimator(vh);
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
            notifyDataSetChanged();
        }

        @Override
        public void onItemInserted(int location) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRemoved(int location) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemMoved(int fromLocation, int toLocation) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int locationStart, int itemCount) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeInserted(int locationStart, int itemCount) {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeRemoved(int locationStart, int itemCount) {
            notifyDataSetChanged();
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

        @Nullable
        private ActionViewClickListener actionViewClickListener;

        public Comment currentComment;

        public ViewHolder(View v) {
            super(v);
            avatarCommentRoot = v.findViewById(R.id.avatar_comment_root);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            comment = (TextView) v.findViewById(R.id.comment);
            date = (TextView) v.findViewById(R.id.date_relative);
            actionViewStub = (ViewStub) v.findViewById(R.id.stub);
        }

        public void inflateActionViewStub(OnCommentButtonClickListener listener) {
            if (actionViewStub != null) {
                actionView = actionViewStub.inflate();
                actionViewStub = null;
                replyToCommentButton = actionView.findViewById(R.id.reply_to_comment);
                deleteCommentButton = actionView.findViewById(R.id.delete_comment);
                reportButton = actionView.findViewById(R.id.report_to_moderator);
            }

            if (actionViewClickListener == null) initActionViewClickListener(listener);
        }

        public void setComment(Comment comment) {
            currentComment = comment;
            if (actionViewClickListener != null) actionViewClickListener.setComment(comment);
        }

        // TODO: не должно его здесь быть. Переделать всё.
        private void initActionViewClickListener(OnCommentButtonClickListener listener) {
            actionViewClickListener = new ActionViewClickListener();
            actionViewClickListener.setListener(listener);
            replyToCommentButton.setOnClickListener(actionViewClickListener);
            deleteCommentButton.setOnClickListener(actionViewClickListener);
            reportButton.setOnClickListener(actionViewClickListener);
        }
    }
}
