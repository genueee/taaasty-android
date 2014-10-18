package ru.taaasty.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

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


}
