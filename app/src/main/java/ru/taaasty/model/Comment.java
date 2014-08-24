package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.Html;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

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
}
