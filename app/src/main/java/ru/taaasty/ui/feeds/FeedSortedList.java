package ru.taaasty.ui.feeds;

import android.support.annotation.Nullable;
import ru.taaasty.recyclerview.RecyclerView;

import ru.taaasty.SortedList;
import ru.taaasty.adapters.FeedItemAdapterLite;
import ru.taaasty.rest.model.Entry;

/**
 * Created by alexey on 21.05.15.
 */
public class FeedSortedList extends SortedList<Entry> {

    public interface IAdapterProvider {
        @Nullable
        RecyclerView.Adapter getTargetAdapter();
    }

    public FeedSortedList(final IAdapterProvider adapterProvider) {
        super(Entry.class, new Callback<Entry>() {
            @Override
            public int compare(Entry o1, Entry o2) {
                return Entry.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR.compare(o1, o2);
            }

            @Override
            public void onInserted(int position, int count) {
                RecyclerView.Adapter adapter = adapterProvider.getTargetAdapter();
                if (adapter != null)
                    adapter.notifyItemRangeInserted(FeedItemAdapterLite.getAdapterPosition(position), count);
            }

            @Override
            public void onRemoved(int position, int count) {
                RecyclerView.Adapter adapter = adapterProvider.getTargetAdapter();
                if (adapter != null)
                    adapter.notifyItemRangeRemoved(FeedItemAdapterLite.getAdapterPosition(position), count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                RecyclerView.Adapter adapter = adapterProvider.getTargetAdapter();
                if (adapter != null)
                    adapter.notifyItemMoved(FeedItemAdapterLite.getAdapterPosition(fromPosition),
                            FeedItemAdapterLite.getAdapterPosition(toPosition));
            }

            @Override
            public void onChanged(int position, int count) {
                RecyclerView.Adapter adapter = adapterProvider.getTargetAdapter();
                if (adapter != null)
                    adapter.notifyItemRangeChanged(FeedItemAdapterLite.getAdapterPosition(position), count);
            }

            @Override
            public boolean areContentsTheSame(Entry oldItem, Entry newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(Entry item1, Entry item2) {
                return item1.getId() == item2.getId();
            }
        });
    }
}
