package ru.taaasty.adapters.grid;

import android.content.Context;
import android.view.View;

import ru.taaasty.model.Entry;

/**
 * Created by alexey on 03.11.14.
 */
public class GridEntryPendingResource extends GridEntryBase {

    public GridEntryPendingResource(View view) {
        this(null, view, 0);
    }

    public GridEntryPendingResource(Context context, View v, int cardWidth) {
        super(context, v, cardWidth);
    }

    @Override
    public void bindEntry(Entry entry) {
    }

    @Override
    public void recycle() {}
}
