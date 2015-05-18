package ru.taaasty;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by alexey on 25.04.15.
 */
public class SortedList<E> extends android.support.v7.util.SortedList<E> {

    private Class<E> mTClass;

    public SortedList(Class<E> klass, Callback<E> callback) {
        super(klass, callback);
        mTClass = klass;
    }

    public SortedList(Class<E> klass, Callback<E> callback, int initialCapacity) {
        super(klass, callback, initialCapacity);
        mTClass = klass;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void resetItems(Collection<E> feed) {
        beginBatchedUpdates();
        try {
            Set<E> items;
            if (feed == null || feed.isEmpty()) {
                items = Collections.emptySet();
            } else {
                items = new HashSet<>(feed.size());
                for (E entry: feed) {
                    items.add(entry);
                    add(entry);
                }
            }

            for (int i=size()-1; i >= 0; --i) {
                if (!items.contains(get(i))) removeItemAt(i);
            }


        } finally {
            endBatchedUpdates();
        }
    }

    public void insertItems(Collection<E> items) {
        beginBatchedUpdates();
        try {
            for (E entry: items) add(entry);
        } finally {
            endBatchedUpdates();
        }
    }

    public E[] getItemsArray() {
        E[] items = (E[])Array.newInstance(mTClass, size());
        for (int i = 0, size = size(); i < size; ++i) items[i] = get(i);
        return items;
    }

    public List<E> getItems() {
        return Arrays.asList(getItemsArray());
    }

    public E getLastEntry() {
        return isEmpty() ? null : get(size()-1);
    }

    public interface ILocationToPosition {
        int getAdapterPosition(int location);
    }

    @SuppressWarnings("unchecked")
    public static <T> SortedList.Callback<T> callbacksWithHeader(ILocationToPosition f, SortedList.Callback<T> base) {
        return new CallbackWithHeader(f, base);
    }

    private static class CallbackWithHeader<T> extends SortedList.Callback<T> {

        final SortedList.Callback<T> base;
        final ILocationToPosition f;

        public CallbackWithHeader(ILocationToPosition f, SortedList.Callback<T> base) {
            this.base = base;
            this.f = f;
        }

        @Override
        public boolean areContentsTheSame(T oldItem, T newItem) {
            return base.areContentsTheSame(oldItem, newItem);
        }

        @Override
        public int compare(T o1, T o2) {
            return base.compare(o1, o2);
        }

        @Override
        public void onInserted(int position, int count) {
            base.onInserted(f.getAdapterPosition(position), count);
        }

        @Override
        public void onRemoved(int position, int count) {
            base.onRemoved(f.getAdapterPosition(position), count);
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            base.onMoved(f.getAdapterPosition(fromPosition), f.getAdapterPosition(toPosition));
        }

        @Override
        public void onChanged(int position, int count) {
            base.onChanged(f.getAdapterPosition(position), count);
        }

        @Override
        public boolean areItemsTheSame(T item1, T item2) {
            return base.areContentsTheSame(item1, item2);
        }
    }
}
