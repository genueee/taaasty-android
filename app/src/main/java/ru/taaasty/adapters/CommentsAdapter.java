package ru.taaasty.adapters;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import ru.taaasty.R;
import ru.taaasty.model.Comment;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.CustomTypefaceSpan;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;

public class CommentsAdapter extends BaseAdapter {
    private final LayoutInflater mInfater;
    private final Resources mResources;
    private final FontManager mFontManager;
    private final ImageUtils mImageUtils;

    private final ArrayList<Comment> mComments;
    private final OnCommentButtonClickListener mListener;
    private final Typeface mSystemFontBold;
    private TlogDesign mFeedDesign;

    private DateFormat mTimeFormatInstance;
    private DateFormat mDdMmFormatInstance;
    private DateFormat mMmYyFormatInstance;

    private Long mSelectedCommentId;
    private boolean mShowDeleteCommentButton;
    private boolean mShowReportButton;

    public CommentsAdapter(Context context, OnCommentButtonClickListener listener) {
        super();
        mInfater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mResources = context.getResources();
        mFontManager = FontManager.getInstance();
        mImageUtils = ImageUtils.getInstance();
        mComments = new ArrayList<>();
        mListener = listener;
        mTimeFormatInstance = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        mDdMmFormatInstance = new SimpleDateFormat("dd MMM", Locale.getDefault());
        mMmYyFormatInstance = new SimpleDateFormat("LL/yyy", Locale.getDefault());
        mSystemFontBold = mFontManager.getFontSystemBold();
    }

    public void setComments(List<Comment> comments) {
        mComments.clear();
        appendComments(comments);
        sortUniqComments();
        notifyDataSetChanged();
    }

    public ArrayList<Comment> getComments() {
        return mComments;
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyDataSetChanged();
    }

    public void appendComments(List<Comment> comments) {
        mComments.addAll(comments);
        sortUniqComments();
        notifyDataSetChanged();
    }

    public void deleteComment(long commentId) {
        boolean dataChanged = false;
        Iterator<Comment> i = mComments.iterator();
        while (i.hasNext()) {
            if (i.next().getId() == commentId) {
                i.remove();
                dataChanged = true;
                break;
            }
        }
        if (dataChanged) notifyDataSetChanged();
    }

    private void sortUniqComments() {
        Map<Long, Comment> map = new HashMap<>(mComments.size());
        for (Comment c: mComments) map.put(c.getId(), c);
        mComments.clear();
        mComments.addAll(map.values());
        Collections.sort(mComments, Comment.SORT_BY_DATE_COMARATOR);
    }

    public Long getTopCommentId() {
        if (mComments.isEmpty()) return null;
        return mComments.get(0).getId();
    }

    public void setSelectedCommentId(Long commentId, boolean showDeleteCommentButton, boolean showReportButton) {
        mShowDeleteCommentButton = showDeleteCommentButton;
        mShowReportButton = showReportButton;
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
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.comments_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.comment_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.comment_view_holder);
        }

        Comment comment = mComments.get(position);

        if (mSelectedCommentId != null && mSelectedCommentId == comment.getId()) {
            setupActionView(vh, comment);
            vh.actionView.setVisibility(View.VISIBLE);
            vh.avatar.setVisibility(View.GONE);
            vh.date.setVisibility(View.GONE);

            // При помощи comment.maarginRight выравниваем текст комментария, чтобы он не налезал на кнопки с действиями.
            // Т.е. он должен быть такой же ширины, как и кнопки.
            // Если ширина кнопок ещё неизвестна, то надо, по идее, дождаться её определения
            if (vh.actionView.getWidth() != 0) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
                lp.rightMargin = vh.actionView.getWidth();
                vh.comment.setLayoutParams(lp);
            } else {
                Log.e("CommentsAdapter", "actionView width is 0", new IllegalStateException());
            }
        } else {
            if (vh.actionView != null) vh.actionView.setVisibility(View.GONE);
            setAuthor(vh, comment);
            setDate(vh, comment);
            vh.avatar.setVisibility(View.VISIBLE);
            vh.date.setVisibility(View.VISIBLE);

            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
            lp.rightMargin = 0;
            vh.comment.setLayoutParams(lp);
        }

        setCommentText(vh, comment);

        return res;
    }

    private void setupActionView(ViewHolder vh, Comment comment) {
        vh.inflateActionViewStub();
        vh.setActionViewListener(comment, mListener);
        vh.deleteCommentButton.setVisibility(mShowDeleteCommentButton ? View.VISIBLE : View.GONE);
        vh.reportButton.setVisibility(mShowReportButton ? View.VISIBLE : View.GONE);
    }

    private void setAuthor(ViewHolder vh, Comment item) {
        User author = item.getAuthor();
        mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);
    }

    private void setCommentText(ViewHolder vh, Comment item) {
        Context context = vh.comment.getContext();
        if (context == null) return;

        TextAppearanceSpan tas = new TextAppearanceSpan(context, mFeedDesign.getAuthorTextAppearance());
        // имя пользователя в комментах proxima bold, накладываем typeface вручную
        CustomTypefaceSpan cts = new CustomTypefaceSpan("sans-serif", mSystemFontBold);

        String slug = item.getAuthor().getSlug();
        SpannableStringBuilder ssb = new SpannableStringBuilder(slug);
        ssb.setSpan(tas, 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.setSpan(cts, 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(' ');
        ssb.append(item.getTextSpanned());
        vh.comment.setText(ssb);
        vh.comment.setTextColor( mFeedDesign.getFeedTextColor(mResources) );
    }

    private void setDate(ViewHolder vh, Comment item) {
        Date updatedAt = item.getUpdatedAt();
        long timediff = Math.abs(System.currentTimeMillis() - updatedAt.getTime());
        String date;
        if (timediff < 24 * 60 * 60 * 1000l) {
            date = mTimeFormatInstance.format(updatedAt);
        } else {
            Calendar lastYear = Calendar.getInstance();
            lastYear.add(Calendar.YEAR, -1);
            if (updatedAt.after(lastYear.getTime())) {
                date = mDdMmFormatInstance.format(updatedAt);
            } else {
                date = mMmYyFormatInstance.format(updatedAt);
            }
        }
        vh.date.setText(date);
    }

    public ValueAnimator createShowButtonsAnimator(View view) {
        final ViewHolder vh = (ViewHolder)view.getTag(R.id.comment_view_holder);
        if (vh == null) throw new NullPointerException();
        vh.inflateActionViewStub();
        assert vh.actionView != null;

        // Сдвигаем влево аватарку и комментарий. View с датой растягиваем до размера кнопок
        int textLeft = vh.comment.getLeft();
        PropertyValuesHolder dxTextLeft = PropertyValuesHolder.ofFloat("dxTextLeft", 0, -textLeft);
        PropertyValuesHolder dAlphaButtons = PropertyValuesHolder.ofFloat("dAlphaButtons",0f, 1f);

        ValueAnimator va = ValueAnimator.ofPropertyValuesHolder(dxTextLeft, dAlphaButtons);
        va.setDuration(view.getResources().getInteger(R.integer.shortAnimTime));
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float dxTextLeft = (Float) animation.getAnimatedValue("dxTextLeft");
                Float alhaButtons = (Float) animation.getAnimatedValue("dAlphaButtons");
                vh.avatarCommentRoot.setTranslationX(dxTextLeft);
                vh.actionView.setAlpha(alhaButtons);
                vh.date.setAlpha(1 - alhaButtons);
            }
        });
        va.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                vh.actionView.setVisibility(View.VISIBLE);
                vh.actionView.setAlpha(0);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                assert vh.actionView != null;
                vh.avatarCommentRoot.setTranslationX(0);
                vh.avatar.setVisibility(View.GONE);
                vh.date.setVisibility(View.GONE);

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
                lp.rightMargin = vh.actionView.getWidth();
                vh.comment.setLayoutParams(lp);

                vh.date.setAlpha(1f);
                vh.avatar.setTranslationX(0f);
                vh.comment.setTranslationX(0f);
                vh.actionView.setAlpha(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return va;
    }

    public ValueAnimator createHideButtonsAnimator(View view) {
        final ViewHolder vh = (ViewHolder)view.getTag(R.id.comment_view_holder);
        if (vh == null) throw new NullPointerException();
        vh.inflateActionViewStub();

        final int textLeft = vh.comment.getLeft();
        final PropertyValuesHolder dxTextLeft = PropertyValuesHolder.ofFloat("dxTextLeft", -textLeft, 0);
        PropertyValuesHolder dalphaButtons = PropertyValuesHolder.ofFloat("dalpha",1f, 0f);

        ValueAnimator va = ValueAnimator.ofPropertyValuesHolder(dxTextLeft, dalphaButtons);
        va.setDuration(view.getResources().getInteger(R.integer.shortAnimTime));
        va.setInterpolator(new AccelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float dalpha = (Float) animation.getAnimatedValue("dalpha");
                Float dxTextLeft = (Float) animation.getAnimatedValue("dxTextLeft");
                vh.avatarCommentRoot.setTranslationX(dxTextLeft);
                vh.date.setAlpha(1f - dalpha);
                vh.actionView.setAlpha(dalpha);
            }
        });
        va.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                vh.avatarCommentRoot.setTranslationX(-textLeft);
                vh.avatar.setVisibility(View.VISIBLE);
                vh.date.setVisibility(View.VISIBLE);
                vh.date.setMinWidth(0);

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
                lp.rightMargin = 0;
                vh.comment.setLayoutParams(lp);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                vh.actionView.setAlpha(1);
                vh.date.setAlpha(1f);
                vh.actionView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return va;
    }

    public interface OnCommentButtonClickListener {
        public void onReplyToCommentClicked(View view, Comment comment);
        public void onDeleteCommentClicked(View view, Comment comment);
        public void onReportContentClicked(View view, Comment comment);
    }

    private static class ActionViewClickListener implements View.OnClickListener {

        private Comment mComment;
        private OnCommentButtonClickListener mListener;

        public ActionViewClickListener(Comment comment, OnCommentButtonClickListener listener) {
            setComment(comment, listener);
        }

        public void setComment(Comment comment, OnCommentButtonClickListener listener) {
            mListener = listener;
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

    public static class ViewHolder {
        public final View root;
        public final View avatarCommentRoot;
        public final ImageView avatar;
        public final TextView comment;
        public final TextView date;

        @Nullable
        private ViewStub actionViewStub;

        @Nullable
        private View actionView;

        @Nullable
        private View replyToCommentButton;

        @Nullable
        private View deleteCommentButton;

        @Nullable
        private View reportButton;

        @Nullable
        private ActionViewClickListener actionViewClickListener;

        public ViewHolder(View v) {
            root = v;
            avatarCommentRoot = v.findViewById(R.id.avatar_comment_root);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            comment = (TextView) v.findViewById(R.id.comment);
            date = (TextView) v.findViewById(R.id.date_relative);
            actionViewStub = (ViewStub) v.findViewById(R.id.stub);
        }

        public void inflateActionViewStub() {
            if (actionViewStub == null) return;
            actionView = actionViewStub.inflate();
            actionViewStub = null;
            replyToCommentButton = actionView.findViewById(R.id.reply_to_comment);
            deleteCommentButton = actionView.findViewById(R.id.delete_comment);
            reportButton = actionView.findViewById(R.id.report_to_moderator);
        }

        public void setActionViewListener(Comment comment, OnCommentButtonClickListener listener) {
            if (actionViewClickListener == null) {
                actionViewClickListener = new ActionViewClickListener(comment, listener);
                replyToCommentButton.setOnClickListener(actionViewClickListener);
                deleteCommentButton.setOnClickListener(actionViewClickListener);
                reportButton.setOnClickListener(actionViewClickListener);
            } else {
                actionViewClickListener.setComment(comment, listener);
            }
        }
    }
}
