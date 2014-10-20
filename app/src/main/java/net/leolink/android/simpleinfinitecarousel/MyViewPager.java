package net.leolink.android.simpleinfinitecarousel;


import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import java.lang.reflect.Field;

public class MyViewPager extends ViewPager {
    public MyViewPager(Context context) {
        super(context);
        changePagerScroller();
    }

    public MyViewPager(Context context, AttributeSet attr) {
        super(context,attr);
        changePagerScroller();
    }

    public void changePagerScroller() {
        try {
            Field mScroller = null;
            mScroller = ViewPager.class.getDeclaredField("mScroller");
            mScroller.setAccessible(true);
            ViewPagerScroller scroller = new ViewPagerScroller(this.getContext());
            mScroller.set(this, scroller);
        } catch (Exception e) {
        }
    }

    class ViewPagerScroller extends Scroller {
        private int mScrollDuration = 1200; // Все ради этого вот
        public ViewPagerScroller(Context context) {
            super(context);
        }
        public ViewPagerScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }
        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
            super.startScroll(startX, startY, dx, dy, mScrollDuration);
        }
        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mScrollDuration);
        }
    }
}
