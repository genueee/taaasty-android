package ru.taaasty.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import ru.taaasty.R;

public class Tabbar extends LinearLayout {

    private static final int DEFAULT_ACTIVATED_TAB = R.id.btn_tabbar_live;

    private int mActivatedElement;

    public Tabbar(Context context) {
        this(context, null);
    }

    public Tabbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public Tabbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, R.layout.tabbar, this);
        mActivatedElement = DEFAULT_ACTIVATED_TAB;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        refreshActivated();
    }

    public void refreshActivated() {
        int childCount = getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = getChildAt(i);
            v.setActivated(mActivatedElement == v.getId());
        }
    }
}
