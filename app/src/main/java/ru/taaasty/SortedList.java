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
public class    SortedList<E> extends android.support.v7.util.SortedList<E> {

    private Class<E> mTClass;

    private final Callback<E> mCalback;

    public SortedList(Class<E> klass, Callback<E> callback) {
        super(klass, callback);
        mTClass = klass;
        mCalback = callback;
    }

    public SortedList(Class<E> klass, Callback<E> callback, int initialCapacity) {
        super(klass, callback, initialCapacity);
        mTClass = klass;
        mCalback = callback;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void resetItems(Collection<E> feed) {
        beginBatchedUpdates();
        try {
            Set<E> itemSet;
            if (feed == null || feed.isEmpty()) {
                itemSet = Collections.emptySet();
            } else {
                itemSet = new HashSet<>(feed.size());
                for (E entry: feed) {
                    itemSet.add(entry);
                    add(entry);
                }
            }

            for (int i=size()-1; i >= 0; --i) {
                if (!itemSet.contains(get(i))) removeItemAt(i);
            }


        } finally {
            endBatchedUpdates();
        }
    }

    @Override
    public int add(E item) {
        return super.add(item);
    }

    /**
     * Вызов {@linkplain #add(Object)} } для коллекции элементов.
     * Требования те же: если в списке уже есть какой-то из элементов, то у него должны совпадать
     * критерии сортировки, иначе могут появляться дубли.
     * @param items
     */
    public void addItems(Collection<E> items) {
        beginBatchedUpdates();
        try {
            for (E entry: items) add(entry);
        } finally {
            endBatchedUpdates();
        }
    }

    public void addOrUpdateItems(E[] items) {
        beginBatchedUpdates();
        try {
            for (E entry: items) addOrUpdate(entry);
        } finally {
            endBatchedUpdates();
        }
    }

    public E[] getItemsArray() {
        E[] items = (E[])Array.newInstance(mTClass, size());
        for (int i = 0, size = size(); i < size; ++i) items[i] = get(i);
        return items;
    }

    /**
     * Добавление, либо обновление элемента в списке.
     * Вызывает {@linkplain #updateItemAt(int, Object)}}, если элемент есть в списке и
     * {@linkplain #add(Object)}, если его нет.
     * Вызывается, если элемент мог быть уже в списке и его позиция изменилась (т.е. у него изменились
     * какой-то из параметров сортировки). Вызывает onMoved().
     * Если известно, что добавляемого элемена точно нет в списке, либо у него ничего не поменялось,
     * то лучше использовать {@linkplain #add(Object)}.
     * XXX Очень не оптимально
     * @param item
     */
    public void addOrUpdate(E item) {
        int idx = linearSearchIndexOf(item);
        if (idx == INVALID_POSITION) {
            add(item);
        } else {
            updateItemAt(idx, item);
        }
    }

    public int linearSearchIndexOf(E item) {
        for (int i=size()-1; i >= 0; --i) if (mCalback.areItemsTheSame(get(i), item)) return i;
        return INVALID_POSITION;
    }


    public List<E> getItems() {
        return Arrays.asList(getItemsArray());
    }

    public E getLastEntry() {
        return isEmpty() ? null : get(size()-1);
    }
}
