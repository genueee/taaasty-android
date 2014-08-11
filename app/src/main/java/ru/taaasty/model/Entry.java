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

public class Entry implements Parcelable {

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
    private List<Image> mImages;

    private transient Spanned mTextSpanned = null;

    private transient Spanned mSourceSpanned = null;

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
    public Spanned getTextSpanned() {
        if (TextUtils.isEmpty(mText)) return null;
        if (mTextSpanned == null) {
            mTextSpanned = Html.fromHtml(mText);
        }
        return mTextSpanned;
    }

    @Nullable
    public Spanned getSourceSpanned() {
        if (TextUtils.isEmpty(mSource)) return null;
        if (mSourceSpanned == null) {
            mSourceSpanned = Html.fromHtml(mText);
        }
        return mSourceSpanned;
    }

    public List<Image> getImages() {
        return mImages == null ? Collections.<Image>emptyList() : mImages;
    }

    public Rating getRating() {
        return mRating != null ? mRating : Rating.DUMMY;
    }

    public void setRating(Rating rating) {
        mRating = rating;
    }

    public static class Image implements Parcelable {
        public long id;

        public Date createAt;

        String title;

        String source;

        public Image2 image = Image2.DUMMY;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.id);
            dest.writeLong(createAt != null ? createAt.getTime() : -1);
            dest.writeString(this.title);
            dest.writeString(this.source);
            dest.writeParcelable(this.image, flags);
        }

        public Image() {
        }

        private Image(Parcel in) {
            this.id = in.readLong();
            long tmpCreateAt = in.readLong();
            this.createAt = tmpCreateAt == -1 ? null : new Date(tmpCreateAt);
            this.title = in.readString();
            this.source = in.readString();
            this.image = in.readParcelable(Image2.class.getClassLoader());
        }

        public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {
            public Image createFromParcel(Parcel source) {
                return new Image(source);
            }

            public Image[] newArray(int size) {
                return new Image[size];
            }
        };
    }

    public static class Image2 implements Parcelable {

        public static final Image2 DUMMY = new Image2();

        public String url = "";

        /**
         * Thumbor path
         */
        public String path = "";

        public ImageGeometry geometry = new ImageGeometry();


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeString(this.path);
            dest.writeParcelable(this.geometry, flags);
        }

        public Image2() {
        }

        private Image2(Parcel in) {
            this.url = in.readString();
            this.path = in.readString();
            this.geometry = in.readParcelable(ImageGeometry.class.getClassLoader());
        }

        public static final Parcelable.Creator<Image2> CREATOR = new Parcelable.Creator<Image2>() {
            public Image2 createFromParcel(Parcel source) {
                return new Image2(source);
            }

            public Image2[] newArray(int size) {
                return new Image2[size];
            }
        };
    }

    public static class ImageGeometry implements Parcelable {
        public int width;
        public int height;


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.width);
            dest.writeInt(this.height);
        }

        public ImageGeometry() {
        }

        private ImageGeometry(Parcel in) {
            this.width = in.readInt();
            this.height = in.readInt();
        }

        public static final Parcelable.Creator<ImageGeometry> CREATOR = new Parcelable.Creator<ImageGeometry>() {
            public ImageGeometry createFromParcel(Parcel source) {
                return new ImageGeometry(source);
            }

            public ImageGeometry[] newArray(int size) {
                return new ImageGeometry[size];
            }
        };
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
        in.readTypedList(mImages, Image.CREATOR);
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
