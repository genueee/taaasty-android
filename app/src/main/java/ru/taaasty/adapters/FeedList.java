package ru.taaasty.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.Log;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.taaasty.BuildConfig;
import ru.taaasty.SortedList;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Comments;
import ru.taaasty.model.Entry;
import ru.taaasty.utils.Objects;

/**
 * Список элементов фида: постов, комментариев, прочих лементов
 *
 * TODO: Вынести "загрузить NN комментариев" в отдельный элемент списка?
 * TODO: Вынести все элементы в хэши и заменить везде в списке на id?
 */
public class FeedList implements Parcelable {
    private static final String TAG = "FeedList";
    private static final boolean DBG = BuildConfig.DEBUG;

    private final List<FeedListItem> mFeed;

    private SortedList.OnListChangedListener mListener;

    // Созданный объект для #sortUniqItems()
    private final Map<Object, FeedListItem> mSortUniqMap;

    /**
     * ID комментария, для которого показывается меню с доп. действиями
     */
    private Long mSelectedCommentId;

    public FeedList() {
        mFeed = new ArrayList<>();
        mSortUniqMap = new HashMap<>();
    }

    public void setListener(SortedList.OnListChangedListener listener) {
        mListener = listener;
    }

    public FeedListItem get(int location) {
        return mFeed.get(location);
    }

    public int size() {
        return mFeed.size();
    }

    public boolean isEmpty() {
        return mFeed.isEmpty();
    }

    /**
     * @return Список. Внутренняя копия, изменять не желательно.
     */
    public List<FeedListItem> getFeed() {
        return Collections.unmodifiableList(mFeed);
    }

    /**
     * Возвращает {@link ru.taaasty.model.Entry} в позиции. null - если там не entry
     * @param location позиция
     * @return статья по позиции
     */
    public Entry getEntry(int location) {
        FeedListItem item = mFeed.get(location);
        if (!item.isEntry()) return null;
        return item.entry;
    }

    /**
     * Возвращает {@link ru.taaasty.model.Entry} в позиции, независимо от типа FeedListItem.
     * Т.е. если там комментарий - entry этого комментария, если что-то ещё - его entry.
     * @param location позиция
     * @return статья по позиции
     */
    public Entry getAnyEntry(int location) {
        return mFeed.get(location).entry;
    }

    public Integer findEntryLocation(long entryId) {
        int size = mFeed.size();
        for (int i = 0; i < size; ++i) {
            if (mFeed.get(i).isEntry()
                    && mFeed.get(i).entry.getId() == entryId) return i;
        }
        return null;
    }

    @Nullable
    public Long getLastEntryId() {
        if (mFeed.isEmpty()) return null;
        return mFeed.get(mFeed.size() - 1).entry.getId();
    }

    /**
     * Возвращает {@link ru.taaasty.model.Comment} в позиции.
     * @param location Позиция
     * @return Комментарий в позиции. Может быть null, если в этйо позиции не комментарий
     */
    @Nullable
    public Comment getCommentAtLocation(int location) {
        FeedListItem item = mFeed.get(location);
        if (!item.isComment()) return null;
        return item.comment;
    }

    public boolean isEntryAtLocation(int location, long entryId) {
        Entry entry = getEntry(location);
        return entry != null && entry.getId() == entryId;
    }

    /**
     * Возвращает кол-во комментариев в списке для поста по id. 0 - если комментариев в списке нет, либо поста нет.
     * @param entryId ID поста.
     */
    public int getCommentsCount(long entryId) {
        int count = 0;
        for (FeedListItem item: mFeed) if (item.isComment() && item.entry.getId() == entryId) count += 1;
        return count;
    }

    public Integer findCommentLocation(long commentId) {
        int size = mFeed.size();
        for (int i = 0; i < size; ++i) {
            if (mFeed.get(i).isComment()
                    && mFeed.get(i).comment.getId() == commentId) return i;
        }
        return null;
    }

    /**
     * Установка списка. Существующий список очищается.
     * @param feed Список
     */
    public void setFeed(List<FeedListItem> feed) {
        mFeed.clear();
        mFeed.addAll(feed);
        for (FeedListItem item: feed) {
            if (item.isEntry()) mFeed.add(FeedListItem.createReplyForm(item.entry));
        }
        sortUniqItems();

        // TODO: не оповещать, если список не изменился
        mListener.onDataSetChanged();
    }

    /**
     * Добавление записей в конец списка. Добавляемый фид должен быть уже отсортирован и ни одной
     * из записей не должно быть уже в списке.
     */
    public boolean appendEntries(List<Entry> feed) {
        boolean adedAllItems = true;
        int location = mFeed.size();
        mFeed.addAll(wrapEntries(feed));
        for (Entry e: feed) mFeed.add(FeedListItem.createReplyForm(e));
        sortUniqItems();
        int locationAfter = mFeed.size();
        /*
        if (locationAfter - location != feed.size()) {
            if (DBG) throw new IllegalStateException("В добавляемых записях есть записи с id уже в списке. Скорее всего, баг");
            adedAllItems = false;
        }
        */
        if (locationAfter - location > 0) {
            mListener.onItemRangeInserted(location, locationAfter - location);
        }
        return adedAllItems;
    }

    /**
     * Добавление либо обновление постов
     * XXX на данный момент не обновляем комментарии
     * @param entries Список постов
     */
    public void addEntries(List<Entry> entries) {
        mFeed.addAll(wrapEntries(entries));
        for (Entry e: entries) mFeed.add(FeedListItem.createReplyForm(e));
        sortUniqItems();
        mListener.onDataSetChanged();
    }

    /**
     * Добавление комментариев одной статьи, либо обновление, если они там есть.
     * Если значение комментариев статьи изменилось, обновляется и это значение
     */
    public void addComments(long entryId, Comments commentsReply) {
        if (commentsReply == null) return;

        Integer entryLocation = findEntryLocation(entryId);
        if (entryLocation == null) {
            // скорее всего, статья удалена из списка. Пропускаем.
            if (DBG) Log.e(TAG, "addComments no entry with id " + entryId);
            return;
        }

        addComments(entryLocation, commentsReply.comments);

        // Обновлям кол-во комментариев, если оно изменилось
        Entry entry = get(entryLocation).entry;
        if (entry.getCommentsCount() != commentsReply.totalCount) {
            entry.setCommentsCount(commentsReply.totalCount);
            mListener.onItemChanged(entryLocation);
        }
    }

    /**
     * Добавление комментариев одной статьи, либо обновление, если они там есть.
     */
    public void addComments(long entryId, List<Comment> comments) {
        if (comments == null || comments.isEmpty()) return;

        Integer entryLocation = findEntryLocation(entryId);
        if (entryLocation == null) {
            // скорее всего, статья удалена из списка. Пропускаем.
            if (DBG) Log.e(TAG, "addComments no entry with id " + entryId);
            return;
        }

        addComments(entryLocation, comments);
    }

    private void addComments(int entryLocation, List<Comment> comments) {
        Entry entry = get(entryLocation).entry;
        mFeed.addAll(entryLocation + 1, wrapComments(entry, comments));

        refreshCommentReplyButton(entry);

        // TODO: более умный вариант
        ArrayList<FeedListItem> before = new ArrayList<>(mFeed);
        sortUniqItems();
        if (before.equals(mFeed)) {
            if (DBG) Log.v(TAG, "addComments() inserted " + comments.size() + " comments");
            // На всякий случай, обновляем индикатор
            mListener.onItemChanged(entryLocation);
            mListener.onItemRangeInserted(entryLocation + 1, comments.size());
        } else {
            mListener.onDataSetChanged();
        }
    }

    /**
     * Обновление записи в списке. Если записи в списке нет, ничего не происходит.
     * @param entry запись
     */
    public void updateEntry(Entry entry) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).getId() == entry.getId()) {
                mFeed.set(i, FeedListItem.createEntry(entry));
                // TODO: обновлять комментарии
                mListener.onItemChanged(i);
                break;
            }
        }
    }

    /**
     * Удаление записи из списка
     * @param entryId ID статьи
     */
    public void deleteEntry(long entryId) {
        int size = mFeed.size();
        for (int i = size - 1; i >= 0; i--) {
            // Удаляем все записи, комментарии и формы ответа
            if (mFeed.get(i).entry.getId() == entryId) {
                mFeed.remove(i);
                mListener.onItemRemoved(i);
            }
        }
    }

    /**
     * Удаление комментария
     * @param commentId ID комментария
     */
    public void deleteComment(long commentId) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).isComment() && mFeed.get(i).comment.getId() == commentId) {
                long commentEntryId = mFeed.get(i).entry.getId();
                mFeed.remove(i);
                mListener.onItemRemoved(i);
                // Статья, скорее всего, изменилась.
                for (int j = i; j >= 0; j--) {
                    if (mFeed.get(i).isEntry() && mFeed.get(i).entry.getId() == commentEntryId) {
                        mListener.onItemChanged(j);
                        break;
                    }
                }
                break;
            }
        }
    }

    /**
     * Возвращает ID выбранного (отображаемого с кнопками действия над ним) комментария.
     * @return null, если комментарий не выбран
     */
    @Nullable
    public Long getSelectedCommentId() {
        return mSelectedCommentId;
    }

    /**
     * Устанавливает ID выбранного комментария
     * @param commentId null, если необходимо снять выбор
     */
    public void setSelectedCommentId(@Nullable Long commentId) {
        Integer oldLocation, newLocation;
        if (Objects.equals(mSelectedCommentId, commentId)) return;

        if (mSelectedCommentId != null) {
            oldLocation = findCommentLocation(mSelectedCommentId);
        } else {
            oldLocation = null;
        }

        if (commentId != null) {
            newLocation = findCommentLocation(commentId);
            if (newLocation == null) {
                if (DBG) Assert.fail("Комментарий " + commentId + " не найден в списке, хотя вроде как должен быть");
                commentId = null;
            }
        } else {
            newLocation = null;
        }

        mSelectedCommentId = commentId;

        if (oldLocation != null) mListener.onItemChanged(oldLocation);
        if (newLocation != null) mListener.onItemChanged(newLocation);
    }

    private void sortUniqItems() {
        for (FeedListItem c: mFeed) { mSortUniqMap.put(c.getUniqId(), c); }
        mFeed.clear();
        mFeed.addAll(mSortUniqMap.values());
        mSortUniqMap.clear();
        Collections.sort(mFeed, FeedListItem.ORDER_BY_ENTRY_COMMENT_CREATE_DATE_DESC_COMARATOR);
    }

    private List<FeedListItem> wrapEntries(List<Entry> feed) {
        FeedListItem newFeed[] = new FeedListItem[feed.size()];
        int size = feed.size();
        for (int i=0; i<size; ++i) {
            newFeed[i] = FeedListItem.createEntry(feed.get(i));
        }
        return Arrays.asList(newFeed);
    }

    private List<FeedListItem> wrapComments(Entry entry, List<Comment> comments) {
        FeedListItem newFeed[] = new FeedListItem[comments.size()];
        int size = comments.size();
        for (int i=0; i<size; ++i) {
            newFeed[i] = FeedListItem.createComment(entry, comments.get(i));
        }
        return Arrays.asList(newFeed);
    }

    /**
     * Добавляет кнопку "ответить на комментари", если её ещё нет
     * @param entry
     * @return
     */
    private boolean refreshCommentReplyButton(Entry entry) {
        boolean dataSetChanged = false;

        boolean itemFound = false;
        for (FeedListItem item : mFeed) {
            if (item.entry.getId() == entry.getId() && item.isReplyForm()) {
                itemFound = true;
                break;
            }
        }
        if (!itemFound) {
            mFeed.add(FeedListItem.createReplyForm(entry));
            dataSetChanged = true;
        }

        return dataSetChanged;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(mFeed);
        dest.writeValue(this.mSelectedCommentId);
    }

    private FeedList(Parcel in) {
        mFeed = in.createTypedArrayList(FeedListItem.CREATOR);
        this.mSelectedCommentId = (Long) in.readValue(Long.class.getClassLoader());
        mSortUniqMap = new HashMap<>(mFeed.size());
    }

    public static final Parcelable.Creator<FeedList> CREATOR = new Parcelable.Creator<FeedList>() {
        public FeedList createFromParcel(Parcel source) {
            return new FeedList(source);
        }

        public FeedList[] newArray(int size) {
            return new FeedList[size];
        }
    };
}
