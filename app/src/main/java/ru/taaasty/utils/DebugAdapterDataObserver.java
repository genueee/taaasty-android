package ru.taaasty.utils;

import android.support.v7.widget.RecyclerView;
import android.util.Log;

public class DebugAdapterDataObserver extends RecyclerView.AdapterDataObserver {

    private final String mTag;

    public DebugAdapterDataObserver(String tag) {
        mTag = tag;
    }

    @Override
    public void onChanged() {
        super.onChanged();
        Log.v(mTag, "onChanged");
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount) {
        super.onItemRangeChanged(positionStart, itemCount);
        Log.v(mTag, "onItemRangeChanged start: " + positionStart + " count: " + itemCount);
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        super.onItemRangeInserted(positionStart, itemCount);
        Log.v(mTag, "onItemRangeInserted start: " + positionStart + " count: " + itemCount);
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        super.onItemRangeMoved(fromPosition, toPosition, itemCount);
        Log.v(mTag, "onItemRangeMoved from: " + fromPosition + " to: " + toPosition + " count: " + itemCount);
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        super.onItemRangeRemoved(positionStart, itemCount);
        Log.v(mTag, "onItemRangeRemoved start: " + positionStart + " count: " + itemCount);
    }
}
