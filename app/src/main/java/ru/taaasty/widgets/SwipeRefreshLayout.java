package ru.taaasty.widgets;

import android.content.Context;
import android.util.AttributeSet;

import ru.taaasty.R;

/**
 * Created by alexey on 14.07.14.
 */
public class SwipeRefreshLayout extends android.support.v4.widget.SwipeRefreshLayout {
    public SwipeRefreshLayout(Context context) {
        this(context, null);
    }

    public SwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
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
}
