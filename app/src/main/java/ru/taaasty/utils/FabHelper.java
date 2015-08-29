package ru.taaasty.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import ru.taaasty.R;

/**
 * Created by alexey on 16.09.15.
 */
public class FabHelper {

    private final View mFabView;

    @Nullable
    private ObjectAnimator mFabAnimator;


    private boolean mIsFabAnimatorShow;

    public FabHelper(View fab) {
        this(fab, 0);
    }

    public FabHelper(View fab, @DimenRes int marginBottom) {
        mFabView = fab;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
            // get rid of margins since shadow area is now the margin
            p.setMargins(0, 0, 0, marginBottom == 0 ? 0 : (int)mFabView.getContext().getResources().getDimension(marginBottom));
            mFabView.setLayoutParams(p);
        }
    }

    public View getView() {
        return mFabView;
    }

    public void showFab(boolean animate) {
        if (mFabView.getVisibility() == View.VISIBLE) return;
        if (!animate) {
            if (mFabAnimator != null) mFabAnimator.cancel();
            mFabView.setVisibility(View.VISIBLE);
            mFabView.setTranslationY(0);
        } else {
            showOrHideFabSmoothly(true);
        }
    }

    public void hideFab(boolean animate) {
        if (mFabView.getVisibility() != View.VISIBLE) return;
        if (!animate) {
            if (mFabAnimator != null) mFabAnimator.cancel();
            mFabView.setVisibility(View.INVISIBLE);
            mFabView.setTranslationY(0);
        } else {
            showOrHideFabSmoothly(false);
        }
    }

    private static final Interpolator sShowHideInterpolator = new LinearOutSlowInInterpolator();

    private void showOrHideFabSmoothly(boolean doShow) {
        if (mFabAnimator != null && mFabAnimator.isStarted()) {
            if (mIsFabAnimatorShow == doShow) {
                return;
            } else {
                mFabAnimator.cancel();
            }
        }

        mIsFabAnimatorShow = doShow;
        int toDy = doShow ? 0 : getCreatePostViewTopToOffscreen();
        mFabAnimator = ObjectAnimator.ofFloat(mFabView, "translationY", (float)toDy)
                .setDuration(mFabView.getResources().getInteger(R.integer.longAnimTime));
        mFabAnimator.setInterpolator(sShowHideInterpolator);
        mFabAnimator.addListener(doShow ? mShowFabAnimatorListener : mHideFabAnimatorListener);
        mFabAnimator.start();
    }

    private final Animator.AnimatorListener mShowFabAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {
            mFabView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };

    private final Animator.AnimatorListener mHideFabAnimatorListener = new Animator.AnimatorListener() {
        @Override
        public void onAnimationStart(Animator animation) {}

        @Override
        public void onAnimationEnd(Animator animation) {
            mFabView.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationCancel(Animator animation) {}

        @Override
        public void onAnimationRepeat(Animator animation) {}
    };

    private int getCreatePostViewTopToOffscreen() {
        return mFabView.getHeight() + ((ViewGroup.MarginLayoutParams)mFabView.getLayoutParams()).bottomMargin;
    }


    public static class AutoHideScrollListener extends RecyclerView.OnScrollListener {

        private FabHelper mFabHelper;

        private boolean mRunning;

        private int mDy;

        private long mStartActionTime;

        private boolean mPause;

        private static final int THRESHOLD_HI = 64;

        private static final int THRESHOLD_LO = -10;

        private static final long MIN_TIME_NS = 64 * 1_000_000l;

        public AutoHideScrollListener(FabHelper helper) {
            mFabHelper = helper;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            long newTime = System.nanoTime();
            if (mRunning) {
                mDy += dy;
                if (isUnderThreshold() && Math.abs(newTime - mStartActionTime) > MIN_TIME_NS) {
                    runShowOrHide();
                }
            } else {
                mRunning = true;
                mDy = dy;
                mStartActionTime = newTime;
            }
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            if (newState ==  RecyclerView.SCROLL_STATE_IDLE) {
                runShowOrHide();
            }
        }

        private boolean isUnderThreshold() {
            return mDy > THRESHOLD_HI || mDy < THRESHOLD_LO;
        }

        private void runShowOrHide() {
            if (!mRunning) return;
            if (mDy > THRESHOLD_HI) {
                mFabHelper.hideFab(true);
            } else if (mDy < THRESHOLD_LO) {
                mFabHelper.showFab(true);
            }
            mRunning = false;
            mDy = 0;
            mStartActionTime = 0;
        }
    }

}
