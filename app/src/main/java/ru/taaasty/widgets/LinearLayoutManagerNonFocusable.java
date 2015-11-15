package ru.taaasty.widgets;

import android.content.Context;
import android.view.View;

import ru.taaasty.recyclerview.LinearLayoutManager;
import ru.taaasty.recyclerview.RecyclerView;

/**
 * Created by alexey on 16.01.15.
 */
public class LinearLayoutManagerNonFocusable extends LinearLayoutManager {

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
