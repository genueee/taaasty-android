package ru.taaasty.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;

/**
 * Created by alexey on 14.07.14.
 */
public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SwipeRefreshLayout";

    // Default offset in dips from the top of the view to where the progress spinner should stop
    private static final int DEFAULT_CIRCLE_TARGET = 215;

    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    @SuppressLint("ResourceAsColor")
    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        setProgressViewOffset(false, 0, (int)(DEFAULT_CIRCLE_TARGET * metrics.density));
        setProgressBackgroundColor(R.color.view_pager_progress_indicator);
    }

    public boolean canChildScrollUp() {
        if (getTarget() instanceof CoordinatorLayout) {
            ViewGroup target = (ViewGroup)getTarget();
            for (int i = target.getChildCount() - 1; i >=0 ; --i){
                View child = target.getChildAt(i);
                AppBarLayout ab;
                if (child instanceof AppBarLayout) {
                    View appbarRoot = ((AppBarLayout) child).getChildAt(0);
                    if (DBG) Log.v(TAG, "view translation y: " + child.getTranslationY());
                    return child.getTranslationY() != 0 || child.getScrollY() > 0 || child.canScrollVertically(-1);
                }
                /*
                if (child instanceof RecyclerView) {
                    final int range = ((RecyclerView)child).computeVerticalScrollRange() -
                            ((RecyclerView)child).computeVerticalScrollExtent();
                    if (range == 0) return false;
                    return ((RecyclerView)child).computeVerticalScrollOffset() > 0;
                } else if (child instanceof ScrollingView) {
                    return ViewCompat.canScrollVertically(target.getChildAt(i), -1) || child.getScrollY() > 0;
                }
                */
            }
        }

        return super.canChildScrollUp();
    }

    private View getTarget() {
        return getChildAt(0);
    }
}
