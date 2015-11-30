package ru.taaasty.utils;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.ui.CreateFlowActivity;
import ru.taaasty.ui.post.CreateAnonymousPostActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.ui.post.Page;
import ru.taaasty.widgets.FabMenuLayout;

/**
 * Created by alexey on 16.09.15.
 */
public class FabHelper {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FabHelper";

    private final View mFabView;

    private final FabMenuLayout mFabMenu;

    private static final int EXPANDED_MENU_FAB_ICON_ROTATION = 45;

    @Nullable
    private ObjectAnimator mFabPositionAnimator;

    private ObjectAnimator mFabRotationAnimator;

    private boolean mIsFabAnimatorShow;

    private static final Interpolator sShowHideInterpolator = new LinearOutSlowInInterpolator();

    private final int mInitialMenuPaddingBottom;

    private final int mInitialFabMarginBottom;

    private FabMenuLayout.OnItemClickListener mFabMenuClickListener;

    public FabHelper(View fab, FabMenuLayout fabMenu) {
        this(fab, fabMenu, 0);
    }

    public FabHelper(View fab, FabMenuLayout fabMenu, @DimenRes int marginBottom) {
        mFabView = fab;
        mFabMenu = fabMenu;
        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMenu();
            }
        });

        mFabMenu.setOnItemClickListener(new FabMenuLayout.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view) {
                boolean handled = false;
                if (mFabMenuClickListener != null) {
                    handled = mFabMenuClickListener.onItemClick(view);
                }
                return handled;
            }
        });

        mInitialMenuPaddingBottom = mFabView.getResources().getDimensionPixelSize(R.dimen.fab_menu_padding_bottom);
        mInitialFabMarginBottom = ((ViewGroup.MarginLayoutParams)mFabView.getLayoutParams()).bottomMargin;

        if (marginBottom != 0) setBottomMargin(fab.getResources().getDimensionPixelSize(marginBottom));

        mFabMenu.setOnExpandedStateListener(new FabMenuLayout.OnExpandedStateChangedListener() {
            @Override
            public void onExpandedStateChanged(boolean expanded, boolean isAnimationActive) {
                if (expanded) {
                    if (mFabRotationAnimator == null || !mFabRotationAnimator.isRunning()) {
                        Log.d(TAG, "onExpandedStateChanged expand fab");
                        setFabIconState(true, isAnimationActive);
                        mFabView.setRotation(EXPANDED_MENU_FAB_ICON_ROTATION);
                    }
                } else {
                    if (mFabRotationAnimator == null || !mFabRotationAnimator.isRunning()) {
                        Log.d(TAG, "onExpandedStateChanged collapse fab");
                        setFabIconState(false, isAnimationActive);
                    }
                }
            }
        });

    }

    public void setMenuListener(FabMenuLayout.OnItemClickListener listener) {
        mFabMenuClickListener = listener;
    }

    public View getView() {
        return mFabView;
    }

    public FabMenuLayout getMenu() {
        return mFabMenu;
    }

    public void setBottomMargin(int margin) {
        mFabMenu.setPadding(mFabMenu.getPaddingLeft(), mFabMenu.getPaddingTop(), mFabMenu.getPaddingRight(),
                mInitialMenuPaddingBottom + margin);

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)mFabView.getLayoutParams();
        lp.bottomMargin = mInitialFabMarginBottom + margin;
        mFabView.setLayoutParams(lp);
    }

    public void showFab(boolean animate) {
        collapseMenuOnly(false);
        showFabOnly(animate);
    }

    public void hideFab(boolean animate) {
        collapseMenuOnly(false);
        hideFabOnly(animate);
    }

    public void expandMenu(boolean animate) {
        showFabOnly(animate);
        expandMenuOnly(animate);
    }

    public void collapseMenu(boolean animate) {
        showFabOnly(animate);
        collapseMenuOnly(animate);
    }

    public void toggleMenu() {
        if (mFabMenu.isExpanded()) {
            collapseMenu(true);
        } else {
            expandMenu(true);
        }
    }


    private void hideFabOnly(boolean animate) {
        if (mFabView.getVisibility() != View.VISIBLE) return;
        if (!animate) {
            if (mFabPositionAnimator != null) mFabPositionAnimator.cancel();
            mFabView.setVisibility(View.INVISIBLE);
            mFabView.setTranslationY(0);
        } else {
            showOrHideFabSmoothly(false);
        }
    }

    private void showFabOnly(boolean animate) {
        if (mFabView.getVisibility() == View.VISIBLE) return;
        if (!animate) {
            if (mFabPositionAnimator != null) mFabPositionAnimator.cancel();
            mFabView.setVisibility(View.VISIBLE);
            mFabView.setTranslationY(0);
        } else {
            showOrHideFabSmoothly(true);
        }
    }

    private void expandMenuOnly(boolean animate) {
        if (mFabMenu.isExpanded()) return;

        setFabIconState(true, animate);
        if (animate) {
            mFabMenu.expandMenu();
        } else {
            mFabMenu.expandMenuNoAnimation();
            mFabView.setRotation(EXPANDED_MENU_FAB_ICON_ROTATION);
        }
    }

    private void collapseMenuOnly(boolean animate) {
        if (!mFabMenu.isExpanded()) return;
        setFabIconState(false, animate);
        if (animate) {
            mFabMenu.collapseMenu();
        } else {
            mFabMenu.collapseMenuNoAnimation();

        }
    }

    private void setFabIconState(boolean stateExpanded, boolean animate) {
        if (mFabRotationAnimator != null) mFabRotationAnimator.cancel();
        float rotationDst = stateExpanded ? EXPANDED_MENU_FAB_ICON_ROTATION : 0;
        if (animate) {
            mFabRotationAnimator = ObjectAnimator.ofFloat(mFabView, View.ROTATION, rotationDst)
                    .setDuration(mFabView.getResources().getInteger(R.integer.shortAnimTime));
            mFabRotationAnimator.start();
        } else {
            mFabView.setRotation(rotationDst);
        }
    }

    public boolean onBackPressed() {
        if (mFabMenu.isExpanded()) {
            collapseMenu(true);
            return true;
        }
        return false;
    }

    private void showOrHideFabSmoothly(boolean doShow) {
        if (mFabPositionAnimator != null && mFabPositionAnimator.isStarted()) {
            if (mIsFabAnimatorShow == doShow) {
                return;
            } else {
                mFabPositionAnimator.cancel();
            }
        }

        mIsFabAnimatorShow = doShow;
        int toDy = doShow ? 0 : getCreatePostViewTopToOffscreen();
        mFabPositionAnimator = ObjectAnimator.ofFloat(mFabView, View.TRANSLATION_Y, (float)toDy)
                .setDuration(mFabView.getResources().getInteger(R.integer.longAnimTime));
        mFabPositionAnimator.setInterpolator(sShowHideInterpolator);
        mFabPositionAnimator.addListener(doShow ? mShowFabAnimatorListener : mHideFabAnimatorListener);
        mFabPositionAnimator.start();
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


    public static class FabMenuDefaultListener implements FabMenuLayout.OnItemClickListener {

        private final Fragment mFragment;

        private final Activity mActivity;

        public FabMenuDefaultListener(Fragment fragment) {
            this.mFragment = fragment;
            this.mActivity = null;
        }

        public FabMenuDefaultListener(Activity activity) {
            this.mFragment = null;
            this.mActivity = activity;
        }

        @Override
        public boolean onItemClick(View view) {
            Activity activity = getActivity(view);
            switch (view.getId()) {
                case R.id.create_flow:
                    CreateFlowActivity.startActivity(activity, null);
                    ((TaaastyApplication) activity.getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FAB,
                            "Открыто создание потока", null);
                    break;
                case R.id.create_anonymous_post:
                    CreateAnonymousPostActivity.startActivity(activity, null);
                    ((TaaastyApplication) activity.getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FAB,
                            "Открыто создание анонимки", null);
                    break;
                case R.id.create_text_post:
                    CreatePostActivity.startCreatePostActivityForResult(view.getContext(),
                            mFragment != null ? mFragment : mActivity, null, Page.TEXT_POST,
                            Constants.ACTIVITY_REQUEST_CODE_CREATE_POST);
                    ((TaaastyApplication) activity.getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FAB,
                            "Открыт текст в дневник", null);
                    break;
                case R.id.create_image_post:
                    CreatePostActivity.startCreatePostActivityForResult(activity,
                            mFragment != null ? mFragment : mActivity, null, Page.IMAGE_POST,
                            Constants.ACTIVITY_REQUEST_CODE_CREATE_POST);
                    ((TaaastyApplication) activity.getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_FAB,
                            "Открыта картинка в дневник", null);
                    break;
                default:
                    return false;

            }
            return true;
        }

        private Activity getActivity(View view) {
            if (mActivity != null) return mActivity;
            if (mFragment != null && mFragment.getActivity() != null) return mFragment.getActivity();
            if (view.getContext() instanceof Activity) return (Activity)view.getContext();
            return null;
        }
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
