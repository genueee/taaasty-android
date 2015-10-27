package ru.taaasty.widgets;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.BuildConfig;


public class FabMenuLayout extends LinearLayoutCompat {

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String TAG = "FabMenuLayout";

    private static final int DURATION_RES_ID = android.R.integer.config_shortAnimTime;

    private static final Interpolator sLinearOutSlowInInterpolator = new LinearOutSlowInInterpolator();

    private OnItemClickListener mItemClickListener;

    private OnExpandedStateChangedListener mExpandedStateListener;

    private Animator mExpandAnimator;

    private Animator mCollapseAnimator;

    private int mFabSize;

    private boolean mLastStateIsExpanded = false;

    public FabMenuLayout(Context context) {
        this(context, null);
    }

    public FabMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FabMenuLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFabSize = (int)((56 + 28) *  context.getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0, size=getChildCount(); i < size; ++i) {
            View v = getChildAt(i);
            v.setOnClickListener(mClickListener);
        }
        setOnClickListener(mClickListener);
        notifyIfExpandedStateChanged(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        collapseMenuNoAnimation();
    }

    public void setOnItemClickListener(OnItemClickListener lisetner) {
        mItemClickListener = lisetner;
    }

    public void setOnExpandedStateListener(OnExpandedStateChangedListener listener) {
        mExpandedStateListener = listener;
    }

    public void expandMenuNoAnimation() {
        setVisibility(View.VISIBLE);
        getBackground().setAlpha(255);
        cancelAnimation();
        for (int i = 0, size=getChildCount(); i < size; ++i) {
            View v = getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                if (isPortait()) {
                    getChildAt(i).setTranslationY(0);
                } else {
                    getChildAt(i).setTranslationX(0);
                }
            }
        }
        notifyIfExpandedStateChanged(false);
    }

    public void collapseMenuNoAnimation() {
        setVisibility(View.INVISIBLE);
        cancelAnimation();
        getBackground().setAlpha(0);
        for (int i = 0, size=getChildCount(); i < size; ++i) {
            View v = getChildAt(i);
            if (v.getVisibility() != View.GONE) {
                if (isPortait()) {
                    getChildAt(i).setTranslationY(getItemInitialTranslation(v));
                } else {
                    getChildAt(i).setTranslationX(getItemInitialTranslation(v));
                }
            }
        }
        notifyIfExpandedStateChanged(false);
    }

    public void expandMenu() {
        if (isExpanded()) return;
        cancelAnimation();

        List<Animator> animatorList = new ArrayList<>(2 * getChildCount() + 1);
        for (int i = 0, size=getChildCount(); i < size; ++i) {
            View v = getChildAt(i);
            if (v.getVisibility() == View.GONE) continue;
            createExpandAnimator(getChildAt(i), getItemInitialTranslation(v), animatorList);
        }

        Drawable background = getBackground();
        Animator backgroundAnimator = ObjectAnimator.ofInt(background, "alpha", 0, 255);
        animatorList.add(backgroundAnimator);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(getResources().getInteger(DURATION_RES_ID));
        animatorSet.playTogether(animatorList);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(View.VISIBLE);
                notifyIfExpandedStateChanged(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mExpandAnimator = animatorSet;
        animatorSet.start();
    }

    public void collapseMenu() {
        if (!isExpanded()) return;
        cancelAnimation();

        List<Animator> animatorList = new ArrayList<>(2 * getChildCount() + 1);

        for (int i = 0, size=getChildCount(); i < size; ++i) {
            createCollapseAnimator(getChildAt(i), getItemInitialTranslation(getChildAt(i)), animatorList);
        }

        Drawable background = getBackground();
        Animator backgroundAnimator = ObjectAnimator.ofInt(background, "alpha", 255, 0);
        animatorList.add(backgroundAnimator);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.setDuration(getResources().getInteger(DURATION_RES_ID));
        animatorSet.playTogether(animatorList);
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                notifyIfExpandedStateChanged(true);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });

        mCollapseAnimator = animatorSet;
        animatorSet.start();
    }

    public void toggleMenu() {
        if (isExpanded()) {
            collapseMenu();
        } else {
            expandMenu();
        }
    }

    public boolean isExpanded() {
        if (getVisibility() == View.VISIBLE) {
            if (mCollapseAnimator != null && mCollapseAnimator.isRunning()) return false;
            return true;
        } else {
            if (mExpandAnimator != null && mExpandAnimator.isRunning()) return true;
            return false;
        }
    }

    private void notifyIfExpandedStateChanged(boolean isAnimationActive) {
        boolean newIsExpanded = isExpanded();
        if (newIsExpanded != mLastStateIsExpanded) {
            mLastStateIsExpanded = newIsExpanded;
            if (DBG) Log.d(TAG, "FabMenuLayout expanded state changed. New state: " + (newIsExpanded ? "expanded" : "collapsed"));
            if (mExpandedStateListener != null) mExpandedStateListener.onExpandedStateChanged(newIsExpanded, isAnimationActive);
        }
    }

    private int getItemInitialTranslation(View view) {
        if (isPortait()) {
            return getHeight() + mFabSize - getPaddingBottom() - view.getBottom();
        } else {
            return getWidth() + mFabSize - getPaddingRight() - view.getRight();
        }
    }

    private boolean isPortait() {
        return getOrientation() == VERTICAL;
    }

    private void createCollapseAnimator(final View view, float offset, List<Animator> dst) {
        Animator positionAnimator;
        if (isPortait()) {
            positionAnimator = ObjectAnimator.ofFloat(view, TRANSLATION_Y, view.getTranslationY(), offset);
        } else {
            positionAnimator = ObjectAnimator.ofFloat(view, TRANSLATION_X, view.getTranslationX(), offset);
        }

        positionAnimator.setInterpolator(sLinearOutSlowInInterpolator);
        dst.add(positionAnimator);

        Animator alphaAnimator = ObjectAnimator.ofFloat(view, ALPHA, view.getAlpha(), 0);
        alphaAnimator.setInterpolator(sLinearOutSlowInInterpolator);
        dst.add(alphaAnimator);
    }

    private void createExpandAnimator(View view, float offset, List<Animator> dst) {
        float initialOffset;
        Animator positionAnimator;


        if (isPortait()) {
            if ((int)view.getTranslationY() == 0) {
                initialOffset = offset;
            } else {
                initialOffset = view.getTranslationY();
            }
            positionAnimator = ObjectAnimator.ofFloat(view, TRANSLATION_Y, initialOffset, 0);
        } else {
            if ((int)view.getTranslationX() == 0) {
                initialOffset = offset;
            } else {
                initialOffset = view.getTranslationX();
            }
            positionAnimator = ObjectAnimator.ofFloat(view, TRANSLATION_X, initialOffset, 0);
        }

        positionAnimator.setInterpolator(sLinearOutSlowInInterpolator);
        dst.add(positionAnimator);


        float initialAlpha = 0;
        if ((int)view.getAlpha() == 1) {
            initialAlpha = 0;
        } else {
            initialAlpha = view.getAlpha();
        }

        Animator alphaAnimator = ObjectAnimator.ofFloat(view, ALPHA, initialAlpha, 1);
        alphaAnimator.setInterpolator(sLinearOutSlowInInterpolator);
        dst.add(alphaAnimator);
    }

    private void cancelAnimation() {
        if (mExpandAnimator != null) mExpandAnimator.cancel();
        if (mCollapseAnimator != null) mCollapseAnimator.cancel();
        mExpandAnimator = null;
        mCollapseAnimator = null;
    }

    public interface OnItemClickListener {
        /**
         * @param view
         * @return true, если действие было предпринято. В этом случае меню будет закрыто
         * без анимации, иначе - с анимацией
         */
        boolean onItemClick(View view);
    }

    public interface OnExpandedStateChangedListener {
        void onExpandedStateChanged(boolean expanded, boolean isAnimationActive);
    }

    private final OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == FabMenuLayout.this) {
                collapseMenu();
                return;
            }

            boolean handled = false;
            if (mItemClickListener != null) handled = mItemClickListener.onItemClick(v);
            if (handled) {
                collapseMenuNoAnimation();
            } else {
                collapseMenu();
            }
        }
    };
}
