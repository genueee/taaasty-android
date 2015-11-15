package ru.taaasty.utils;

import android.view.View;
import android.view.ViewTreeObserver;

/**
* Created by alexey on 24.12.14.
*/
public class ListScrollController {

    private static final int LAST_SCROLLSTATE_AT_TOP_BOTTOM = 0;
    private static final int LAST_SCROLLSTATE_AT_TOP = 1;
    private static final int LAST_SCROLLSTATE_AT_BOTTOM = 2;
    private static final int LAST_SCROLLSTATE_NOT_EDGE = 3;

    private final View mListView;
    private final OnListScrollPositionListener mListener;

    private int mNewState = LAST_SCROLLSTATE_AT_TOP_BOTTOM;

    private boolean mPreDrawCheckQueued;

    public interface OnListScrollPositionListener {
        void onEdgeReached(boolean atTop);
        void onEdgeUnreached();
    }

    public ListScrollController(View listView, OnListScrollPositionListener listener) {
        mListView = listView;
        mListener = listener;
    }

    public void checkScrollStateOnViewPreDraw() {
        if (mListView == null) return;
        if (mPreDrawCheckQueued) return;
        mPreDrawCheckQueued = true;
        mListView.getViewTreeObserver().addOnPreDrawListener(mOnPreDrawListener);
    }

    public void checkScroll() {
        if (mListView == null) return;
        boolean atTop = !mListView.canScrollVertically(-1);
        boolean atBottom = !mListView.canScrollVertically(1);

        int newState;
        if (atTop && atBottom) {
            newState = LAST_SCROLLSTATE_AT_TOP_BOTTOM;
        } else if (atTop) {
            newState = LAST_SCROLLSTATE_AT_TOP;
        } else if (atBottom) {
            newState = LAST_SCROLLSTATE_AT_BOTTOM;
        } else {
            newState = LAST_SCROLLSTATE_NOT_EDGE;
        }

        if (newState != mNewState) {
            mNewState = newState;
            if (newState == LAST_SCROLLSTATE_NOT_EDGE) {
                mListener.onEdgeUnreached();
            } else {
                if (mListener != null) mListener.onEdgeReached(atTop);
            }
        }
    }

    private final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
        public boolean onPreDraw() {
            if (mListView.getViewTreeObserver().isAlive()) {
                mListView.getViewTreeObserver().removeOnPreDrawListener(this);
                if (!mPreDrawCheckQueued) {
                    mPreDrawCheckQueued = false;
                    checkScroll();
                }
            }
            return true;
        }
    };
}
