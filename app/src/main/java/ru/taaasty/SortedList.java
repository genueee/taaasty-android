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

    public List<E> getItems() {
        E[] items = (E[])Array.newInstance(mTClass, size());
        for (int i = 0, size = size(); i < size; ++i) items[i] = get(i);
        return Arrays.asList(items);
    }

    public E getLastEntry() {
        return isEmpty() ? null : get(size()-1);
    }
}
