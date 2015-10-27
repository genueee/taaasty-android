package ru.taaasty.ui.feeds;

import android.annotation.SuppressLint;
import android.os.Handler;

/**
 * Created by alexey on 29.10.15.
 */
abstract class TlogInterfaceVisibilityController {

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private final Handler mHideActionBarHandler;
    private volatile boolean userForcedToShowInterface = false;
    private boolean mNavigationHidden;

    protected abstract boolean isInterfaceShown();
    protected abstract void onOverlayVisibilityChanged(boolean isShown);

    public TlogInterfaceVisibilityController() {
        mHideActionBarHandler = new Handler();
        userForcedToShowInterface = false;
    }

    public void onResume() {
        mNavigationHidden = !isInterfaceShown();
        runHideActionBarTimer();
    }

    public void onDestroy() {
        mHideActionBarHandler.removeCallbacks(mHideAbRunnable);
    }

    public void onListClicked() {
        if (mNavigationHidden) {
            userForcedToShowInterface = true;
        }
        toggleShowOrHideHideyBarMode();
    }

    public boolean isNavigationHidden() {
        return mNavigationHidden;
    }

    public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        if (dy < -50
                || totalCount == 0
                || (firstVisibleItem == 0 && firstVisibleFract < 0.1)
                ) {
            userForcedToShowInterface();
        }
    }

    void onVisibilityChanged(boolean shown) {
        mNavigationHidden = !shown;
        onOverlayVisibilityChanged(shown);
    }

    private void userForcedToShowInterface() {
        if (mNavigationHidden) {
            userForcedToShowInterface = true;
            toggleShowOrHideHideyBarMode();
        }
    }

    @SuppressLint("InlinedApi")
    private void toggleShowOrHideHideyBarMode() {
        if (!mNavigationHidden) {
            onVisibilityChanged(false);
        } else {
            userForcedToShowInterface = false;
            onVisibilityChanged(true);
            runHideActionBarTimer();
        }
    }

    private void runHideActionBarTimer() {
        mHideActionBarHandler.removeCallbacks(mHideAbRunnable);
        mHideActionBarHandler.postDelayed(mHideAbRunnable, HIDE_ACTION_BAR_DELAY);
    }

    private final Runnable mHideAbRunnable = new Runnable() {
        @Override
        public void run() {
            if (!userForcedToShowInterface && !mNavigationHidden) {
                toggleShowOrHideHideyBarMode();
            }
        }
    };
}
