package ru.taaasty.ui.feeds;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.Date;

import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.model.Entry;
import ru.taaasty.widgets.DateIndicatorWidget;

/**
 * Created by alexey on 04.11.14.
 */
public class FeedsHelper {

    private FeedsHelper() {

    }

    @Nullable
    public static int getAdapterPositionAtWidgetHeight(RecyclerView listView,
                                                 View widget,
                                                 FeedItemAdapter adapter
                                                 ) {
        // Здесь считаем, что top listview и top parent'а dateindicator находятся на одной высоте
        int triggerTop = widget.getTop() + widget.getHeight() / 2;
        // int triggerTop = mListView.getTop() + mListView.getHeight() / 2;
        View child = null;
        // Ищем ближайшую запись
        RecyclerView.LayoutManager lm = listView.getLayoutManager();
        final int count = lm.getChildCount();
        for (int i = count - 1; i >= 0; i--) {
            final View checked = listView.getChildAt(i);
            if (triggerTop >= checked.getTop()) {
                child = checked;
                break;
            }
        }

        return child == null ? RecyclerView.NO_POSITION : listView.getChildPosition(child);
    }

    public static void updateDateIndicator(RecyclerView listView,
                                           DateIndicatorWidget dateIndicator,
                                           FeedItemAdapter adapter,
                                           boolean animScrollUp) {
        Date newDate = null;
        if (listView == null || dateIndicator == null) return;

        int position = getAdapterPositionAtWidgetHeight(listView, dateIndicator, adapter);
        if (position != RecyclerView.NO_POSITION) {
            Entry entry = adapter.getEntryAtPosition(position);
            if (entry != null) newDate = entry.getCreatedAt();
        }
        dateIndicator.setDate(newDate, animScrollUp);
    }

}
