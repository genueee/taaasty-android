package ru.taaasty.adapters;

import android.animation.Animator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
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
    private final DateFormat mTimeFormatLongInstance, mDdMmFormatLongInstance, mMmYyFormatLongInstance;

    public CommentViewBinder() {
        mImageUtils = ImageUtils.getInstance();

        mTimeFormatInstance = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        // 10 нояб.
        mDdMmFormatInstance = new SimpleDateFormat("dd MMM", Locale.getDefault());

        // Нояб/2010
        mMmYyFormatInstance = new SimpleDateFormat("LL/yyy", Locale.getDefault());

        //
        mTimeFormatLongInstance = SimpleDateFormat.getTimeInstance(SimpleDateFormat.DEFAULT, Locale.getDefault());

        // 7 Ноября
        mDdMmFormatLongInstance = new SimpleDateFormat("dd MMMM", Locale.getDefault());

        // 7 Ноября 2010
        mMmYyFormatLongInstance = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    }

    public void bindNotSelectedComment(CommentsAdapter.ViewHolder vh, Comment comment, TlogDesign design) {
        if (vh.actionView != null) vh.actionView.setVisibility(View.GONE);
        bindAuthor(vh, comment);
        vh.avatar.setVisibility(View.VISIBLE);

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
        lp.rightMargin = 0;
        vh.comment.setLayoutParams(lp);
        bindCommentText(vh, comment, design);
        updateCommentRightPadding(vh, false);
    }

    public void bindSelectedComment(CommentsAdapter.ViewHolder vh, Comment comment, TlogDesign design) {
        bindActionView(vh, comment);
        bindCommentText(vh, comment, design);

        vh.actionView.setVisibility(View.VISIBLE);
        vh.avatar.setVisibility(View.GONE);
        updateCommentRightPadding(vh, true);
    }

    public ValueAnimator createShowButtonsAnimator(final CommentsAdapter.ViewHolder vh, Comment comment) {
        if (vh == null) throw new NullPointerException();
        bindActionView(vh, comment);
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
                vh.avatar.setTranslationX(0f);
                vh.comment.setTranslationX(0f);
                vh.actionView.setAlpha(1f);
                updateCommentRightPadding(vh, true);
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

    public ValueAnimator createHideButtonsAnimator(final CommentsAdapter.ViewHolder vh) {
        vh.inflateActionViewStub();

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
                vh.actionView.setAlpha(dalpha);
            }
        });
        va.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animation) {
                vh.avatarCommentRoot.setTranslationX(-textLeft);
                vh.avatar.setVisibility(View.VISIBLE);

                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
                lp.rightMargin = 0;
                vh.comment.setLayoutParams(lp);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                vh.actionView.setAlpha(1);
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

    /**
     * При помощи comment.maarginRight выравниваем текст комментария, чтобы он не налезал на кнопки с действиями.
     * Если ширина кнопок ещё неизвестна, то дожидаемся её определения
     */
    private void updateCommentRightPadding(final CommentsAdapter.ViewHolder vh, boolean isSelectedComment) {
        if (vh.actionView == null) return;
        if (!isSelectedComment) {
            updateCommentRightPaddingMeasured(vh, 0);
            return;
        }

        if (vh.actionView.getWidth() == 0) {
            if (BuildConfig.DEBUG) Log.v("CommentViewBinder", "actionView width is 0");
            vh.actionView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (vh.actionView != null && vh.actionView.getViewTreeObserver().isAlive()) {
                        vh.actionView.getViewTreeObserver().removeOnPreDrawListener(this);
                        return !updateCommentRightPaddingMeasured(vh, vh.actionView.getWidth());
                    } else {
                        return true;
                    }
                }
            });
        } else {
            updateCommentRightPaddingMeasured(vh, vh.actionView.getWidth());
        }
    }

    private boolean updateCommentRightPaddingMeasured(CommentsAdapter.ViewHolder vh, int newRightPadding) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)vh.comment.getLayoutParams();
        if (lp.rightMargin != newRightPadding) {
            lp.rightMargin = newRightPadding;
            vh.comment.setLayoutParams(lp);
            return true;
        }
        return false;
    }

    private void bindActionView(CommentsAdapter.ViewHolder vh, Comment comment) {
        vh.inflateActionViewStub();
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

        // Добавляем дату в текст
        vh.updatedAtValue = item.getUpdatedAt().getTime();
        vh.relativeDateValue = getRelativeDate(context, vh.updatedAtValue);
        int start = ssb.length();
        ssb.append(" —\u00A0");
        ssb.append(vh.relativeDateValue.replace(" ", "\u00A0"));
        int textAppearance = design.isLightTheme() ? R.style.TextAppearanceDateCommentInlineWhite : R.style.TextAppearanceDateCommentInlineBlack;
        TextAppearanceSpan span = new TextAppearanceSpan(context, textAppearance);
        ssb.setSpan(span, start, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        vh.comment.setText(ssb);
        vh.comment.setTextColor( design.getFeedTextColor(context.getResources()) );
    }

    public String getRelativeDate(Context context, long updatedAt) {
        String relative = UiUtils.getRelativeDate(context, updatedAt);
        return relative != null ? relative : getDate(updatedAt, true);
    }

    private String getDate(long updatedAt, boolean longFormat) {
        final DateFormat defaultFormat, ddMmDormat, mmYyFormat;
        String date;
        long timediff = Math.abs(System.currentTimeMillis() - updatedAt);

        if (longFormat) {
            defaultFormat = mTimeFormatLongInstance;
            ddMmDormat = mDdMmFormatLongInstance;
            mmYyFormat = mMmYyFormatLongInstance;
        } else {
            defaultFormat = mTimeFormatInstance;
            ddMmDormat = mDdMmFormatInstance;
            mmYyFormat = mMmYyFormatInstance;
        }

        if (timediff < 24 * 60 * 60 * 1000l) {
            date = defaultFormat.format(updatedAt);
        } else {
            Calendar lastYear = Calendar.getInstance();
            lastYear.add(Calendar.YEAR, -1);
            if (new Date(updatedAt).after(lastYear.getTime())) {
                date = ddMmDormat.format(updatedAt);
            } else {
                date = mmYyFormat.format(updatedAt);
            }
        }
        return date;
    }


}
