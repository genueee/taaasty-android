package ru.taaasty.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ru.taaasty.UserManager;
import ru.taaasty.model.iframely.IFramely;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.UiUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Entry implements Parcelable {

    @Retention(SOURCE)
    @StringDef({
            PRIVACY_PUBLIC,
            PRIVACY_PRIVATE,
            PRIVACY_PUBLIC_WITH_VOTING
    })
    public @interface EntryPrivacy {}

    public static final String ENTRY_TYPE_TEXT = "text";

    public static final String ENTRY_TYPE_IMAGE = "image";

    public static final String ENTRY_TYPE_VIDEO = "video";

    public static final String ENTRY_TYPE_QUOTE = "quote";

    // Устаревший тип song
    public static final String ENTRY_TYPE_SONG = "song";

    public static final String PRIVACY_PUBLIC_WITH_VOTING = "public_with_voting";

    public static final String PRIVACY_PUBLIC = "public";

    public static final String PRIVACY_PRIVATE = "private";

    /**
     * Сортировка по убыванию даты создания (более новые - в начале списка)
     */
    public static transient Comparator<Entry> ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR = new Comparator<Entry>() {
        @Override
        public int compare(Entry lhs, Entry rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            } else {
                int compareDates = rhs.getCreatedAt().compareTo(lhs.getCreatedAt());
                return compareDates != 0 ? compareDates : Objects.compare(rhs.getId(), lhs.getId());
            }
        }
    };


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

    @SerializedName("title")
    private String mTitle = "";

    @SerializedName("video_url")
    private String mVideoUrl;

    @SerializedName("cover_url")
    private String mCoverUrl;

    @SerializedName("iframely")
    private IFramely mIframely;

    @SerializedName("text")
    private String mText = "";

    @SerializedName("source")
    private String mSource;

    @SerializedName("via")
    private String mVia;

    @SerializedName("privacy")
    @EntryPrivacy
    private String mPrivacy;

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

    public boolean isEmbedd() {
        return ENTRY_TYPE_VIDEO.equals(mType);
    }

    public boolean isYoutubeVideo() {
        return isEmbedd() && "YouTube".equalsIgnoreCase(mIframely.meta.site);
    }

    public boolean isQuote() {
        return ENTRY_TYPE_QUOTE.equals(mType);
    }

    public boolean isImage() {
        return ENTRY_TYPE_IMAGE.equals(mType);
    }

    public boolean isEntryTypeText() {
        return ENTRY_TYPE_TEXT.equals(mType);
    }

    public User getAuthor() {
        return mAuthor == null ? User.DUMMY : mAuthor;
    }

    public int getCommentsCount() {
        return mCommentsCount;
    }

    public void setCommentsCount( int commentsCount ) { mCommentsCount = commentsCount; }

    public Date getCreatedAt() {
        return mCreatedAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public Date getCreateOrUpdatedAt() {
        return mUpdatedAt != null ? mUpdatedAt : mCreatedAt;
    }

    public String getEntryUrl() {
        return mEntryUrl;
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

    public String getCoverUrl() {
        return mCoverUrl;
    }

    public IFramely getIframely() {
        return mIframely;
    }

    @EntryPrivacy
    public String getPrivacy() {
        return mPrivacy;
    }


    @Nullable
    public String getVideoUrl() { return mVideoUrl; }

    @Nullable
    private synchronized Spanned getTextSpanned() {
        if (TextUtils.isEmpty(mText)) return null;
        if (mTextSpanned == null) {
            mTextSpanned = Html.fromHtml(mText);
        }
        return mTextSpanned;
    }

    @Nullable
    private synchronized Spanned getSourceSpanned() {
        if (TextUtils.isEmpty(mSource)) return null;
        if (mSourceSpanned == null) {
            mSourceSpanned = Html.fromHtml(mSource);
        }
        return mSourceSpanned;
    }

    @Nullable
    private synchronized Spanned getTitleSpanned() {
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

    public List<ImageInfo> getImages() {
        return mImages == null ? Collections.<ImageInfo>emptyList() : mImages;
    }

    public ArrayList<String> getImageUrls(boolean includeAnimatedGifs) {
        if (mImages == null) return new ArrayList<>(0);
        final ArrayList<String> images = new ArrayList<>(mImages.size());
        for (ImageInfo imageInfo: mImages) {
            if (!includeAnimatedGifs && imageInfo.isAnimatedGif()) continue;
            if (!TextUtils.isEmpty(imageInfo.image.path)) {
                images.add(NetworkUtils.createThumborUrlFromPath(imageInfo.image.path).toUrl());
            } else {
                images.add(imageInfo.image.url);
            }
        }
        return images;
    }

    /**
     * @return true если пост открытый
     */
    public boolean isPublic() {
        return PRIVACY_PUBLIC.equals(mPrivacy);
    }

    public Rating getRating() {
        return mRating != null ? mRating : Rating.DUMMY;
    }

    public void setRating(Rating rating) {
        mRating = rating;
    }

    public boolean isMyEntry() {
        Long me = UserManager.getInstance().getCurrentUserId();
        return me != null && (me == mAuthor.getId());
    }

    public boolean canEdit() {
        return isMyEntry(); // TODO: исправить, когда будет в API
    }

    public boolean canReport() {
        return !isMyEntry(); // TODO: исправить, когда будет в API
    }

    public boolean canDelete() {
        return isMyEntry(); // TODO: исправить, когда будет в API
    }

    @Nullable
    public Uri getFirstImageUri() {
        if (mImages == null || mImages.isEmpty()) return null;
        String url = mImages.get(0).image.url;
        if (TextUtils.isEmpty(url)) return null;
        return Uri.parse(url);
    }

    public Entry() {
    }

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
                ", mTitle='" + mTitle + '\'' +
                ", mText='" + mText + '\'' +
                ", mSource='" + mSource + '\'' +
                ", mPrivacy='" + mPrivacy + '\'' +
                ", mVia='" + mVia + '\'' +
                ", mImages=" + mImages +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mType);
        dest.writeParcelable(this.mAuthor, flags);
        dest.writeInt(this.mCommentsCount);
        dest.writeLong(mCreatedAt != null ? mCreatedAt.getTime() : -1);
        dest.writeLong(mUpdatedAt != null ? mUpdatedAt.getTime() : -1);
        dest.writeString(this.mEntryUrl);
        dest.writeParcelable(this.mRating, flags);
        dest.writeString(this.mTitle);
        dest.writeString(this.mVideoUrl);
        dest.writeString(this.mCoverUrl);
        dest.writeParcelable(this.mIframely, flags);
        dest.writeString(this.mText);
        dest.writeString(this.mSource);
        dest.writeString(this.mVia);
        dest.writeString(this.mPrivacy);
        dest.writeTypedList(mImages);
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
        this.mTitle = in.readString();
        this.mVideoUrl = in.readString();
        this.mCoverUrl = in.readString();
        this.mIframely = in.readParcelable(IFramely.class.getClassLoader());
        this.mText = in.readString();
        this.mSource = in.readString();
        this.mVia = in.readString();
        //noinspection ResourceType
        this.mPrivacy = in.readString();
        this.mImages = in.createTypedArrayList(ImageInfo.CREATOR);
    }

    public static final Creator<Entry> CREATOR = new Creator<Entry>() {
        public Entry createFromParcel(Parcel source) {
            return new Entry(source);
        }

        public Entry[] newArray(int size) {
            return new Entry[size];
        }
    };
}
