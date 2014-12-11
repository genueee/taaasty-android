package ru.taaasty;

import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Отсортированный список элементов, с уведомлениями при изменениях.
 * Элементы в списке уникальны по ID и отсортированны comparator'ом.
 */
public abstract class SortedList<E> {

    private static final String TAG = "FeedList";
    private static final boolean DBG = BuildConfig.DEBUG;

    private final Comparator<E> mComparator;

    private final List<E> mList;

    private SortedList.OnListChangedListener mListener;

    public SortedList(Comparator<E> comparator) {
        mComparator = comparator;
        mList = new ArrayList<E>();
    }

    public abstract long getItemId(E item);

    private long getIdAt(int location) {
        return getItemId(mList.get(location));
    }

    public void setListener(SortedList.OnListChangedListener listener) {
        mListener = listener;
    }

    public E get(int location) {
        return mList.get(location);
    }

    public int size() {
        return mList.size();
    }

    public boolean isEmpty() {
        return mList.isEmpty();
    }

    /**
     * @return Список. Внутренняя копия, изменять не желательно.
     */
    public List<E> getItems() {
        return Collections.unmodifiableList(mList);
    }

    public void resetItems(@Nullable Collection<E> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            if (!mList.isEmpty()) {
                int size = mList.size();
                mList.clear();
                mListener.onItemRangeRemoved(0, size);
            }
        } else {
            mList.clear();
            mList.addAll(newItems);
            sortUniqItems();
            if (mListener != null) mListener.onDataSetChanged();
        }
    }

    /**
     * Добавление элемента либо установка, если он уже есть в списке
     *
     * @param item
     */
    public void insertItem(E item) {
        if (item == null) return;
        int insertLocation = Collections.binarySearch(mList, item, mComparator);

        if (insertLocation >= 0 && getIdAt(insertLocation) == getItemId(item)) {
            mList.set(insertLocation, item);
            if (mListener != null) mListener.onItemChanged(insertLocation);
        } else {
            int newLocation = -insertLocation - 1;
            Integer oldLocation = findLocation(getItemId(item));

            // Удаляем элемент с заданным ID, если он у нас уже есть в списке
            if (oldLocation != null) {
                mList.remove(oldLocation.intValue());
                if (mListener != null) mListener.onItemInserted(oldLocation);
                if (newLocation > oldLocation) newLocation -= 1;
            }

            mList.add(newLocation, item);
            if (mListener != null) mListener.onItemInserted(newLocation);
        }
    }

    public void insertItems(List<E> items) {
        if (items == null || items.isEmpty()) return;
        if (items.size() == 1) {
            insertItem(items.get(0));
            return;
        }

        int initialLocation = mList.size();
        mList.addAll(items);
        ArrayList<E> old = new ArrayList<>(mList);
        sortUniqItems();
        if (mListener != null) {
            if (mList.equals(old)) {
                mListener.onItemRangeInserted(initialLocation, items.size());
            } else {
                mListener.onDataSetChanged();
            }
        }
    }

    @Nullable
    public E findItem(long itemId) {
        for (E item: mList) if (getItemId(item) == itemId) return item;
        return null;
    }

    @Nullable
    public Integer findLocation(long itemId) {
        int size = mList.size();
        for (int i = 0; i < size; ++i) if (getIdAt(i) == itemId) return i;
        return null;
    }

    public void deleteItem(long id) {
        int size = mList.size();
        for (int location = 0; location < size; ++location) {
            if (getIdAt(location) == id) {
                mList.remove(location);
                if (mListener != null) mListener.onItemRemoved(location);
                break;
            }
        }
    }

    public Long getLastEntryId() {
        if (isEmpty()) return null;
        return getIdAt(mList.size() - 1);
    }

    private void sortUniqItems() {
        int size = mList.size();
        LongSparseArray<E> messagesArray = new LongSparseArray<>(size);
        for (E item : mList) messagesArray.put(getItemId(item), item);
        mList.clear();
        int length = messagesArray.size();
        for (int i = 0; i < length; ++i) mList.add(messagesArray.valueAt(i));
        Collections.sort(mList, mComparator);
    }

    public interface OnListChangedListener {
        public void onDataSetChanged();

        public void onItemChanged(int location);

        public void onItemInserted(int location);

        public void onItemRemoved(int location);

        public void onItemMoved(int fromLocation, int toLocation);

        public void onItemRangeChanged(int locationStart, int itemCount);

        public void onItemRangeInserted(int locationStart, int itemCount);

        public void onItemRangeRemoved(int locationStart, int itemCount);
    }
}
