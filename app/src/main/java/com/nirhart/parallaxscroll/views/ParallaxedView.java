package com.nirhart.parallaxscroll.views;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;

import java.lang.ref.WeakReference;

public abstract class ParallaxedView {
	static public boolean isAPI11 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	protected WeakReference<View> view;
	protected int lastOffset;
    private final Rect mVisibleHeight = new Rect();

	abstract protected void translatePreICS(View view, float offset);
	
	public ParallaxedView(View view) {
		this.lastOffset = 0;
		this.view = new WeakReference<View>(view);
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

    private void setOpacity(View view, float offset) {
        float viewHeight = view.getHeight();
        float visibleHeight = view.getBottom() - offset; // XXX: wrong
        if (visibleHeight < viewHeight) {
            float opacity = 0.3f + (visibleHeight / viewHeight) * 0.7f;
            view.setAlpha(opacity);
        }
    }

	public void setView(View view) {
		this.view = new WeakReference<View>(view);
	}
}
