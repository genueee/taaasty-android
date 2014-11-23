package ru.taaasty.adapters;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Comment;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 06.11.14.
 */
public class CommentViewBinder {

    private final ImageUtils mImageUtils;
    private final DateFormat mTimeFormatInstance, mDdMmFormatInstance, mMmYyFormatInstance;

    private CommentsAdapter.OnCommentButtonClickListener mListener;

    public CommentViewBinder() {
        mImageUtils = ImageUtils.getInstance();
        mTimeFormatInstance = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        mDdMmFormatInstance = new SimpleDateFormat("dd MMM", Locale.getDefault());
        mMmYyFormatInstance = new SimpleDateFormat("LL/yyy", Locale.getDefault());
    }

    public void setOnCommentButtonClickListener(CommentsAdapter.OnCommentButtonClickListener listener) {
        mListener = listener;
    }

    public void bindNotSelectedComment(CommentsAdapter.ViewHolder vh, Comment comment, TlogDesign design) {
        vh.setComment(comment);
        if (vh.actionView != null) vh.actionView.setVisibility(View.GONE);
        bindAuthor(vh, comment);
        bindDate(vh, comment);
        vh.avatar.setVisibility(View.VISIBLE);
        vh.date.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
        lp.rightMargin = 0;
        vh.comment.setLayoutParams(lp);
        bindCommentText(vh, comment, design);
    }

    public void bindSelectedComment(CommentsAdapter.ViewHolder vh, Comment comment, TlogDesign design) {
        vh.setComment(comment);
        bindCommentText(vh, comment, design);
        bindActionView(vh, comment);
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
    }

    public ValueAnimator createShowButtonsAnimator(final CommentsAdapter.ViewHolder vh) {
        if (vh == null) throw new NullPointerException();
        vh.inflateActionViewStub(mListener);
        assert vh.actionView != null;

        // Сдвигаем влево аватарку и комментарий. View с датой растягиваем до размера кнопок
        int textLeft = vh.comment.getLeft() - vh.avatar.getLeft();
        PropertyValuesHolder dxTextLeft = PropertyValuesHolder.ofFloat("dxTextLeft", 0, -textLeft);
        PropertyValuesHolder dAlphaButtons = PropertyValuesHolder.ofFloat("dAlphaButtons",0f, 1f);

        ValueAnimator va = ValueAnimator.ofPropertyValuesHolder(dxTextLeft, dAlphaButtons);
        va.setDuration(vh.itemView.getResources().getInteger(R.integer.shortAnimTime));
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

                if (vh.actionView.getWidth() == 0) {
                    if (BuildConfig.DEBUG) Log.v("CommentViewBinder", "actionView width is 0");
                    vh.actionView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            vh.actionView.getViewTreeObserver().removeOnPreDrawListener(this);
                            return !updateCommentRightPadding(vh);
                        }
                    });
                } else {
                    updateCommentRightPadding(vh);
                }

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

    private boolean updateCommentRightPadding(CommentsAdapter.ViewHolder vh) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
        int width = vh.actionView.getWidth();
        if (lp.rightMargin != width) {
            lp.rightMargin = width;
            vh.comment.setLayoutParams(lp);
            return true;
        }
        return false;
    }

    private final ViewTreeObserver.OnPreDrawListener mActionViewOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
            return false;
        }
    };

    public ValueAnimator createHideButtonsAnimator(final CommentsAdapter.ViewHolder vh) {
        vh.inflateActionViewStub(mListener);

        final int textLeft = vh.comment.getLeft();
        final PropertyValuesHolder dxTextLeft = PropertyValuesHolder.ofFloat("dxTextLeft", -textLeft, 0);
        PropertyValuesHolder dalphaButtons = PropertyValuesHolder.ofFloat("dalpha",1f, 0f);

        ValueAnimator va = ValueAnimator.ofPropertyValuesHolder(dxTextLeft, dalphaButtons);
        va.setDuration(vh.itemView.getResources().getInteger(R.integer.shortAnimTime));
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

    private void bindActionView(CommentsAdapter.ViewHolder vh, Comment comment) {
        vh.inflateActionViewStub(mListener);
        vh.deleteCommentButton.setVisibility(comment.canDelete() ? View.VISIBLE : View.GONE);
        vh.reportButton.setVisibility(comment.canReport() ? View.VISIBLE : View.GONE);
    }

    private void bindAuthor(CommentsAdapter.ViewHolder vh, Comment item) {
        User author = item.getAuthor();
        mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);
    }

    private void bindCommentText(CommentsAdapter.ViewHolder vh, Comment item, TlogDesign design) {
        Context context = vh.comment.getContext();
        if (context == null) return;

        SpannableStringBuilder ssb = new SpannableStringBuilder(item.getAuthor().getSlug());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), item.getAuthor().getId(), context, design.getAuthorTextAppearance());
        ssb.append(' ');
        ssb.append(item.getTextSpanned());
        vh.comment.setText(ssb);
        vh.comment.setTextColor( design.getFeedTextColor(context.getResources()) );
    }

    private void bindDate(CommentsAdapter.ViewHolder vh, Comment item) {
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


}
