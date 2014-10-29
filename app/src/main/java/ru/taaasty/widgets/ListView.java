package ru.taaasty.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxListView;

/**
 * ListView с нашими обходами багов
 */
public class ListView extends ParallaxListView {
    public ListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // workaround для неверного определения canScrollVertically при SmoothScrollbarEnabled
    // Исправляет цепляние SwipeRefreshLayout вверху
    @Override
    public boolean canScrollVertically(int direction) {
        boolean canScroll = super.canScrollVertically(direction);
        if (direction < 0
                && !isSmoothScrollbarEnabled()
                && getChildCount() > 0
                ) {
            View child = getChildAt(0);
            canScroll = getFirstVisiblePosition() > 0 || child.getTop() < getPaddingTop();
        }
        return canScroll;
    }

}
