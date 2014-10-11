package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import ru.taaasty.R;

/**
 * Created by alexey on 14.07.14.
 */
public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {

    private final Paint mPreRefreshPaint;

    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPreRefreshPaint = new Paint();
        mPreRefreshPaint.setStyle(Paint.Style.FILL);
        mPreRefreshPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setColorSchemeResources(
                R.color.refresh_widget_color1,
                R.color.refresh_widget_color2,
                R.color.refresh_widget_color3,
                R.color.refresh_widget_color4
        );
    }

    @Override
    public void draw(Canvas canvas) {
        View target = getChildAt(0);
        if (target != null && target.getTop() > 0) {
            canvas.drawRect(getPaddingLeft(),
                    getPaddingTop(),
                    getWidth() - getPaddingLeft() - getPaddingRight(),
                    target.getTop(),
                    mPreRefreshPaint);
        }
        super.draw(canvas);
    }

}
