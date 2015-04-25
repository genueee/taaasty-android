package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.Date;

import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 01.08.14.
 */
public class Comment implements Parcelable {

    @SerializedName("id")
    private long mId;

    @SerializedName("user")
    User mAuthor = User.DUMMY;

    @SerializedName("created_at")
    private Date mCreatedAt;

    @SerializedName("updated_at")
    private Date mUpdatedAt;

    @SerializedName("text")
    String mText;

    @SerializedName("is_disabled")
    boolean mIsDisabled;

    @SerializedName("can_edit")
    boolean mCanEdit;

    @SerializedName("can_report")
    boolean mCanReport;

    @SerializedName("can_delete")
    boolean mCanDelete;

    /**
     * Сортировка по возрастанию даты создания (самые старые - в начало списка)
     */
    public static transient Comparator<Comment> ORDER_BY_DATE_ID_COMARATOR = new Comparator<Comment>() {
        @Override
        public int compare(Comment lhs, Comment rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return 1;
            } else if (rhs == null) {
                return -1;
            } else {
                int compareDates = lhs.getUpdatedAt().compareTo(rhs.getUpdatedAt());
                return compareDates != 0 ? compareDates : Objects.compare(lhs.getId(), rhs.getId());
            }
        }
    };

    public long getId() {
        return mId;
    }

    public User getAuthor() {
        return mAuthor;
    }

    public String getText() {
        return mText;
    }

    public CharSequence getTextSpanned() {
        return TextUtils.isEmpty(mText) ? "" : Html.fromHtml(mText);
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt != null ? mUpdatedAt : mCreatedAt;
    }

    public boolean IsDisabled() {
        return mIsDisabled;
    }

    public boolean canEdit() {
        return mCanEdit;
    }

    public boolean canReport() {
        return mCanReport;
    }

    public boolean canDelete() {
        return mCanDelete;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeParcelable(this.mAuthor, 0);
        dest.writeLong(mCreatedAt != null ? mCreatedAt.getTime() : -1);
        dest.writeLong(mUpdatedAt != null ? mUpdatedAt.getTime() : -1);
        dest.writeString(this.mText);
        dest.writeByte(mIsDisabled ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanEdit ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanReport ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanDelete ? (byte) 1 : (byte) 0);
    }

    public Comment() {
    }

    private Comment(Parcel in) {
        this.mId = in.readLong();
        this.mAuthor = in.readParcelable(User.class.getClassLoader());
        long tmpMCreatedAt = in.readLong();
        this.mCreatedAt = tmpMCreatedAt == -1 ? null : new Date(tmpMCreatedAt);
        long tmpMUpdatedAt = in.readLong();
        this.mUpdatedAt = tmpMUpdatedAt == -1 ? null : new Date(tmpMUpdatedAt);
        this.mText = in.readString();
        this.mIsDisabled = in.readByte() != 0;
        this.mCanEdit = in.readByte() != 0;
        this.mCanReport = in.readByte() != 0;
        this.mCanDelete = in.readByte() != 0;
    }

    public static final Parcelable.Creator<Comment> CREATOR = new Parcelable.Creator<Comment>() {
        public Comment createFromParcel(Parcel source) {
            return new Comment(source);
        }

        public Comment[] newArray(int size) {
            return new Comment[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Comment comment = (Comment) o;

        if (mId != comment.mId) return false;
        if (mIsDisabled != comment.mIsDisabled) return false;
        if (mCanEdit != comment.mCanEdit) return false;
        if (mCanReport != comment.mCanReport) return false;
        if (mCanDelete != comment.mCanDelete) return false;
        if (mAuthor != null ? !mAuthor.equals(comment.mAuthor) : comment.mAuthor != null)
            return false;
        if (mCreatedAt != null ? !mCreatedAt.equals(comment.mCreatedAt) : comment.mCreatedAt != null)
            return false;
        if (mUpdatedAt != null ? !mUpdatedAt.equals(comment.mUpdatedAt) : comment.mUpdatedAt != null)
            return false;
        return !(mText != null ? !mText.equals(comment.mText) : comment.mText != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (mId ^ (mId >>> 32));
        result = 31 * result + (mAuthor != null ? mAuthor.hashCode() : 0);
        result = 31 * result + (mCreatedAt != null ? mCreatedAt.hashCode() : 0);
        result = 31 * result + (mUpdatedAt != null ? mUpdatedAt.hashCode() : 0);
        result = 31 * result + (mText != null ? mText.hashCode() : 0);
        result = 31 * result + (mIsDisabled ? 1 : 0);
        result = 31 * result + (mCanEdit ? 1 : 0);
        result = 31 * result + (mCanReport ? 1 : 0);
        result = 31 * result + (mCanDelete ? 1 : 0);
        return result;
    }
}
