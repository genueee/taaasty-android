package ru.taaasty.widgets;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.IParallaxedHeaderHolder;

/**
 * RecyclerView с параметрами, которые все равно везде ставим
 */
public class MyRecyclerView extends android.support.v7.widget.RecyclerView {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyRecyclerView";

    private Set<ScrollEventConsumerVh> mScrollListeners = Collections.newSetFromMap(new WeakHashMap<ScrollEventConsumerVh, Boolean>());

    public interface ScrollEventConsumerVh {

        void onStartScroll();

        void onStopScroll();

    }

    public MyRecyclerView(Context context) {
        this(context, null, 0);
    }

    public MyRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        ItemAnimator animator = getItemAnimator();
        if (animator instanceof DefaultItemAnimator) {
            ((DefaultItemAnimator)animator).setSupportsChangeAnimations(false);
        }
        animator.setAddDuration(getResources().getInteger(R.integer.longAnimTime));
    }

    @Override
    public void onChildAttachedToWindow(View child) {
        super.onChildAttachedToWindow(child);
        ViewHolder vh = getChildViewHolder(child);
        if (vh != null && vh instanceof ScrollEventConsumerVh) {
            mScrollListeners.add((ScrollEventConsumerVh)vh);
        }

        if (vh instanceof IParallaxedHeaderHolder) {
            vh.itemView.getViewTreeObserver().addOnScrollChangedListener((IParallaxedHeaderHolder) vh);
        }

    }

    @Override
    public void onChildDetachedFromWindow(View child) {
        super.onChildDetachedFromWindow(child);
        ViewHolder vh = getChildViewHolder(child);
        if (vh != null && vh instanceof ScrollEventConsumerVh) {
            mScrollListeners.remove(vh);
        }

        if (vh != null && vh instanceof IParallaxedHeaderHolder && vh.itemView.getViewTreeObserver().isAlive()) {
            vh.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) vh);
        }
    }

    @Override
    public void onScrollStateChanged(int newState) {
        super.onScrollStateChanged(newState);

        boolean stopScroll = newState == SCROLL_STATE_IDLE;

        int cnt = 0;
        for (ScrollEventConsumerVh consumer: mScrollListeners) {
            if (consumer != null) {
                cnt += 1;
                if (stopScroll) {
                    consumer.onStopScroll();
                } else {
                    consumer.onStartScroll();
                }
            }
        }
    }
}
