package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import ru.taaasty.utils.UiUtils;

public class Entry implements Parcelable {

    public static final String ENTRY_TYPE_TEXT = "text";

    public static final String ENTRY_TYPE_IMAGE = "image";

    public static final String ENTRY_TYPE_VIDEO = "video";

    public static final String ENTRY_TYPE_QUOTE = "quote";

    @SerializedName("id")
    private long mId;

    @SerializedName("type")
    private String mType;

    @SerializedName("author")
    private User mAuthor;

    @SerializedName("comments_count")
    private int mCommentsCount;

    @SerializedName("created_at")
    private Date mCreatedAt;

    @SerializedName("updated_at")
    private Date mUpdatedAt;

    @SerializedName("entry_url")
    private String mEntryUrl;

    @SerializedName("rating")
    private Rating mRating;

    @SerializedName("image_url")
    @Nullable
    private String mImageUrl;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("text")
    private String mText;

    @SerializedName("source")
    private String mSource;

    @SerializedName("via")
    private String mVia;

    @SerializedName("image_attachments")
    private List<ImageInfo> mImages;

    private transient volatile Spanned mTextSpanned = null;

    private transient volatile Spanned mSourceSpanned = null;

    private transient volatile Spanned mTitleSpanned = null;

    public long getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public User getAuthor() {
        return mAuthor == null ? User.DUMMY : mAuthor;
    }

    public int getCommentsCount() {
        return mCommentsCount;
    }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public String getEntryUrl() {
        return mEntryUrl;
    }

    public String getImageUrl() {
        return mImageUrl;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getText() {
        return mText;
    }

    public String getSource() {
        return mSource;
    }

    @Nullable
    public synchronized Spanned getTextSpanned() {
        if (TextUtils.isEmpty(mText)) return null;
        if (mTextSpanned == null) {
            mTextSpanned = Html.fromHtml(mText);
        }
        return mTextSpanned;
    }

    @Nullable
    public synchronized Spanned getSourceSpanned() {
        if (TextUtils.isEmpty(mSource)) return null;
        if (mSourceSpanned == null) {
            mSourceSpanned = Html.fromHtml(mSource);
        }
        return mSourceSpanned;
    }

    @Nullable
    public synchronized Spanned getTitleSpanned() {
        if (TextUtils.isEmpty(mTitle)) return null;
        if (mTitleSpanned == null) {
            mTitleSpanned = Html.fromHtml(mTitle);
        }
        return mTitleSpanned;
    }

    public boolean hasTitle() {
        return !UiUtils.isBlank(getTitleSpanned());
    }

    public boolean hasText() {
        return !UiUtils.isBlank(getTextSpanned());
    }

    public boolean hasNoAnyText() {
        return !hasTitle() && !hasText() && !hasSource();
    }

    public boolean hasSource() {
        return !UiUtils.isBlank(getSourceSpanned());
    }

    public boolean hasImages() {
        return mImages != null && !mImages.isEmpty();
    }

    public List<ImageInfo> getImages() {
        return mImages == null ? Collections.<ImageInfo>emptyList() : mImages;
    }

    public Rating getRating() {
        return mRating != null ? mRating : Rating.DUMMY;
    }

    public void setRating(Rating rating) {
        mRating = rating;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mType);
        dest.writeParcelable(this.mAuthor, 0);
        dest.writeInt(this.mCommentsCount);
        dest.writeLong(mCreatedAt != null ? mCreatedAt.getTime() : -1);
        dest.writeLong(mUpdatedAt != null ? mUpdatedAt.getTime() : -1);
        dest.writeString(this.mEntryUrl);
        dest.writeParcelable(this.mRating, flags);
        dest.writeString(this.mImageUrl);
        dest.writeString(this.mTitle);
        dest.writeString(this.mText);
        dest.writeString(this.mSource);
        dest.writeString(this.mVia);
        dest.writeTypedList(mImages);
    }

    public Entry() {
    }

    private Entry(Parcel in) {
        this.mId = in.readLong();
        this.mType = in.readString();
        this.mAuthor = in.readParcelable(User.class.getClassLoader());
        this.mCommentsCount = in.readInt();
        long tmpMCreatedAt = in.readLong();
        this.mCreatedAt = tmpMCreatedAt == -1 ? null : new Date(tmpMCreatedAt);
        long tmpMUpdatedAt = in.readLong();
        this.mUpdatedAt = tmpMUpdatedAt == -1 ? null : new Date(tmpMUpdatedAt);
        this.mEntryUrl = in.readString();
        this.mRating = in.readParcelable(Rating.class.getClassLoader());
        this.mImageUrl = in.readString();
        this.mTitle = in.readString();
        this.mText = in.readString();
        this.mSource = in.readString();
        this.mVia = in.readString();
        in.readTypedList(mImages, ImageInfo.CREATOR);
    }

    public static final Parcelable.Creator<Entry> CREATOR = new Parcelable.Creator<Entry>() {
        public Entry createFromParcel(Parcel source) {
            return new Entry(source);
        }

        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };

    @Override
    public String toString() {
        return "Entry{" +
                "mId=" + mId +
                ", mType='" + mType + '\'' +
                ", mAuthor=" + mAuthor +
                ", mCommentsCount=" + mCommentsCount +
                ", mCreatedAt=" + mCreatedAt +
                ", mUpdatedAt=" + mUpdatedAt +
                ", mEntryUrl='" + mEntryUrl + '\'' +
                ", mRating=" + mRating +
                ", mImageUrl='" + mImageUrl + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mText='" + mText + '\'' +
                ", mSource='" + mSource + '\'' +
                ", mVia='" + mVia + '\'' +
                ", mImages=" + mImages +
                '}';
    }

}
