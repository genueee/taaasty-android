package ru.taaasty.widgets;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by alexey on 16.01.15.
 */
public class LinearLayoutManagerNonFocusable extends LinearLayoutManager{

    public LinearLayoutManagerNonFocusable(Context context) {
        super(context);
    }

    public LinearLayoutManagerNonFocusable(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }


    @Override
    public boolean onRequestChildFocus (RecyclerView parent, RecyclerView.State state, View child, View focused) {
        // Не даем сфокусироваться на элементе, иначе появляется ненужный скроллинг при кликах на длинных постах
        return true;
    }
}
