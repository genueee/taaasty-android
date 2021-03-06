package com.nirhart.parallaxscroll.views;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;

import java.lang.ref.WeakReference;

public class ParallaxedView {
	static public boolean isAPI11 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	protected WeakReference<View> view;
	protected int lastOffset;
    private final Rect mVisibleHeight = new Rect();

	protected void translatePreICS(View view, float offset) {

    }
	
	public ParallaxedView(View view) {
		this.lastOffset = 0;
		this.view = new WeakReference<>(view);
	}

	public boolean is(View v) {
		return (v != null && view != null && view.get() != null && view.get().equals(v));
	}

	@SuppressLint("NewApi")
	public void setOffset(float offset) {
		View view = this.view.get();
		if (view != null)
            if (isAPI11) {
                view.setTranslationY(offset);
                setOpacity(view, offset);
            } else {
                translatePreICS(view, offset);
            }
    }

    protected void setOpacity(View view, float offset) {
        float viewHeight = view.getHeight();
        float visibleHeight = view.getBottom() - offset; // XXX: wrong
        if (visibleHeight < viewHeight) {
            float opacity = getParallaxOpacity(visibleHeight / viewHeight);
            view.setAlpha(opacity);
        } else {
            view.setAlpha(1);
        }
    }

    protected float getParallaxOpacity(float visiblePart) {
        return 0.3f + visiblePart * 0.7f;
    }

	public void setView(View view) {
		this.view = new WeakReference<>(view);
	}
}
