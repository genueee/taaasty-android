package ru.taaasty.adapters.grid;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import ru.taaasty.model.Entry;

/**
 * Created by alexey on 19.10.14.
 */
public abstract class GridEntryBase extends RecyclerView.ViewHolder {

    final Context mContext;
    final int mCardWidth;

    public GridEntryBase(Context context, View v, int cardWidth) {
        super(v);
        mContext = context;
        mCardWidth = cardWidth;

    }

    public abstract void bindEntry(Entry entry);

    public abstract void recycle();

}
