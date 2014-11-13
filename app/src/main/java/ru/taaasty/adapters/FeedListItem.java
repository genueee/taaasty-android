package ru.taaasty.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.RecyclerView;

import java.util.Comparator;

import ru.taaasty.BuildConfig;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Entry;

/**
 * Статья, комментарий, или форма ответа на комментарий
 */
public class FeedListItem implements Parcelable {

    /**
     * TODO: покрыть тестами
     */
    public static Comparator<FeedListItem> ORDER_BY_ENTRY_COMMENT_CREATE_DATE_DESC_COMARATOR = new Comparator<FeedListItem>() {
        @Override
        public int compare(FeedListItem lhs, FeedListItem rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            }

            // В любом случае сравниваем сначала статьи
            int compareEntries = Entry.ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR.compare(lhs.entry, rhs.entry);
            if (compareEntries != 0) return compareEntries;

            if (rhs.isEntry() && lhs.isEntry()) {
                // Статья и статья
                return 0;
            }

            if (lhs.isEntry()) {
                // Статья и комментарий/форма ответа этой статьи
                return -1;
            }

            if (rhs.isEntry()) {
                // Комментарий/форма ответа статьи и сама статья
                return 1;
            }

            if (lhs.isReplyForm() && rhs.isReplyForm()) {
                if (BuildConfig.DEBUG) throw new IllegalStateException("Не должно быть у одной статьи две формы ответа");
                return 0;
            }

            // Форма ответа и комментарий. Форма ответа всегда внизу.
            if (lhs.isReplyForm()) {
                return 1;
            }

            // Комментарий и форма ответа. Форма ответа всегда внизу.
            if (rhs.isReplyForm()) {
                return -1;
            }

            // Комментарий и комментарий одной статьи
            return Comment.ORDER_BY_DATE_ID_COMARATOR.compare(lhs.comment, rhs.comment);

        }
    };

    public final Entry entry;

    public final Comment comment;

    private final UniqId mUniqId;

    private static final int TYPE_ENTRY = 0;

    private static final int TYPE_COMMENT = 1;

    private static final int TYPE_REPLY_FORM = 2;

    private final int type;


    public static FeedListItem createEntry(Entry entry) {
        return new FeedListItem(TYPE_ENTRY, entry, null);
    }

    public static FeedListItem createComment(Entry entry, Comment comment) {
        return new FeedListItem(TYPE_COMMENT, entry, comment);
    }

    public static FeedListItem createReplyForm(Entry entry) {
        return new FeedListItem(TYPE_REPLY_FORM, entry, null);
    }

    FeedListItem(int type, Entry entry, Comment comment) {
        this.type = type;
        this.entry = entry;
        this.comment = comment;
        this.mUniqId = new UniqId(type, entry.getId(), comment == null ? -1l : comment.getId());
    }

    public boolean isEntry() {
        return type == TYPE_ENTRY;
    }

    public boolean isComment() {
        return type == TYPE_COMMENT;
    }

    public boolean isReplyForm() { return type == TYPE_REPLY_FORM; }

    public long getId() {
        // Это бздец полный, но да ладно
        if (isReplyForm()) return RecyclerView.NO_ID;
        return isEntry() ? entry.getId() : -1 * comment.getId();
    }

    public Object getUniqId() {
        return mUniqId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(type);
        dest.writeParcelable(this.entry, flags);
        dest.writeParcelable(this.comment, flags);
    }

    FeedListItem(Parcel in) {
        this.type = in.readInt();
        this.entry = in.readParcelable(Entry.class.getClassLoader());
        this.comment = in.readParcelable(Comment.class.getClassLoader());
        mUniqId = new UniqId(type, entry.getId(), comment == null ? -1l : comment.getId());
    }

    public static final Creator<FeedListItem> CREATOR = new Creator<FeedListItem>() {
        public FeedListItem createFromParcel(Parcel source) {
            return new FeedListItem(source);
        }

        public FeedListItem[] newArray(int size) {
            return new FeedListItem[size];
        }
    };

    private class UniqId {
        final int type;
        final long entryId;
        final long commentId;

        UniqId(int type, long entryId, long commentId) {
            this.type = type;
            this.entryId = entryId;
            this.commentId = commentId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UniqId uniqId = (UniqId) o;

            if (commentId != uniqId.commentId) return false;
            if (entryId != uniqId.entryId) return false;
            if (type != uniqId.type) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = type;
            result = 31 * result + (int) (entryId ^ (entryId >>> 32));
            result = 31 * result + (int) (commentId ^ (commentId >>> 32));
            return result;
        }
    }
}
