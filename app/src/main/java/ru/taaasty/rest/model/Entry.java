package ru.taaasty.rest.model;

import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import junit.framework.Assert;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.Session;
import ru.taaasty.rest.model.iframely.IFramely;
import ru.taaasty.rest.model.iframely.Link;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.UiUtils;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class Entry implements Parcelable, Cloneable {

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

    public static final String ENTRY_TYPE_ANONYMOUS = "anonymous";

    // Устаревший тип song
    public static final String ENTRY_TYPE_SONG = "song";

    public static final String PRIVACY_PUBLIC_WITH_VOTING = "public_with_voting";

    public static final String PRIVACY_PUBLIC = "public";

    public static final String PRIVACY_PRIVATE = "private";

    /**
     * Сортировка по убыванию даты создания (более новые - в начале списка)
     */
    public static transient Comparator<Entry> ORDER_BY_CREATE_DATE_DESC_ID_COMARATOR = (lhs, rhs) -> {
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
    };


    @SerializedName("id")
    private long mId;

    @SerializedName("type")
    private String mType;

    @SerializedName("author")
    private User mAuthor;

    @SerializedName("tlog")
    private EntryTlog mTlog;

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

    @SerializedName("is_favorited")
    private boolean mFavorited;

    @SerializedName("is_voteable")
    private boolean mIsVoteable;

    @SerializedName("can_vote")
    private boolean mCanVote;

    @SerializedName("can_report")
    private boolean mCanReport;

    @SerializedName("can_edit")
    private boolean mCanEdit;

    @SerializedName("can_delete")
    private boolean mCanDelete;

    @SerializedName("image_url")
    private String mImageUrl;

    @SerializedName("preview_image")
    private ImageInfo.Image2 mPreviewImage;

    private transient volatile SpannedString mTextSpanned = null;

    private transient volatile SpannedString mSourceSpanned = null;

    private transient volatile SpannedString mTitleSpanned = null;

    public static Entry setRating(Entry entry, Rating rating) {
        Entry dst = null;
        try {
            dst = entry.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
        dst.mRating = rating;
        return dst;
    }

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

    public boolean isAnonymousPost() { return ENTRY_TYPE_ANONYMOUS.equals(mType); }

    public User getAuthor() {
        return mAuthor == null ? User.ANONYMOUS : mAuthor;
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

    /**
     * @return Тлог, в который написан пост. Может отличаться от текущей загруженной ленты (отличается у постов в потоки, репосты и т.п.)
     */
    public EntryTlog getTlog() {
        return mTlog;
    }

    /**
     *
     * @return Эту запись автор написал в свой дневник, а не в поток или ещё куда-нибудь.
     */
    public boolean isEntryInOwnTlog() {
        return mAuthor.mId == mTlog.id;
    }

    public String getTitle() {
        return mTitle == null ? "" : mTitle;
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

    @Override
    protected Entry clone() throws CloneNotSupportedException {
        return (Entry)super.clone();
    }

    /**
     * @return Дизайн. Пока тлога автора
     */
    @Nullable
    public TlogDesign getDesign() {
        return mAuthor == null ? null : mAuthor.getDesign();
    }

    @EntryPrivacy
    public String getPrivacy() {
        switch (mPrivacy) {
            case PRIVACY_PRIVATE:
            case "lock":
                return PRIVACY_PRIVATE;
            case PRIVACY_PUBLIC:
            case "unlock":
                return PRIVACY_PUBLIC;
            case PRIVACY_PUBLIC_WITH_VOTING:
                return PRIVACY_PUBLIC_WITH_VOTING;
            case "live":
                return mIsVoteable ? PRIVACY_PUBLIC_WITH_VOTING : PRIVACY_PUBLIC;
            default:
                if (BuildConfig.DEBUG) Assert.fail("unknown privacy " + mPrivacy);
                return PRIVACY_PUBLIC;
        }
    }

    @Nullable
    public String getVideoUrl() { return mVideoUrl; }

    @Nullable
    public synchronized Spanned getTextSpanned() {
        if (TextUtils.isEmpty(mText)) return null;
        if (mTextSpanned == null) {
            mTextSpanned = new SpannedString(Html.fromHtml(mText));
        }
        return mTextSpanned;
    }

    @Nullable
    public synchronized Spanned getSourceSpanned() {
        if (TextUtils.isEmpty(mSource)) return null;
        if (mSourceSpanned == null) {
            mSourceSpanned = new SpannedString(Html.fromHtml(mSource));
        }
        return mSourceSpanned;
    }

    @Nullable
    public synchronized Spanned getTitleSpanned() {
        if (TextUtils.isEmpty(mTitle)) return null;
        if (mTitleSpanned == null) {
            mTitleSpanned = new SpannedString(Html.fromHtml(mTitle));
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

    /**
     * @return Первая ссылка на картинку из images.
     */
    @Nullable
    public Uri getFirstImageUri() {
        if (mImages == null || mImages.isEmpty()) return null;
        String url = mImages.get(0).image.url;
        if (TextUtils.isEmpty(url)) return null;
        return Uri.parse(url);
    }

    /**
     * Все картинки из поста: imageUrl, images, из iframely и из текста
     * Не отсортированные и могут встречаться одинаковые урлы.
     * URL'ы не в thumbor
     * @param includeAnimatedGifs Включать анимированные гифки
     * @return
     */
    public ArrayList<String> getImageUrls(boolean includeAnimatedGifs) {
        final ArrayList<String> images = new ArrayList<>();

        if (mImageUrl != null) images.add(mImageUrl);
        if (mImages != null) {
            for (ImageInfo imageInfo : mImages) {
                if (!includeAnimatedGifs && imageInfo.isAnimatedGif()) continue;
                images.add(imageInfo.image.url);
            }
        }

        // Картинки из iframely
        if (mIframely != null && mIframely.links != null && mIframely.links.image != null) {
            for (Link l: mIframely.links.image) {
                images.add(l.getHref());
            }
        }

        // Картинки из текста
        Spanned s = getTextSpanned();
        if (!TextUtils.isEmpty(s)) images.addAll(UiUtils.getImageSpanUrls(s));

        return images;
    }

    /**
     * @return Изображение для аватарки в списе сообщений. Возвращается только
     * в списке сообщений, в других местах - null.
     */
    @Nullable
    public ImageInfo.Image2 getPreviewImage() {
        return mPreviewImage;
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

    public boolean isMyEntry() {
        long me = Session.getInstance().getCurrentUserId();
        return me != CurrentUser.USER_UNAUTHORIZED_ID && (mAuthor != null) && (me == mAuthor.getId());
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

    /**
     * @return Я могу голосовать за этот пост
     */
    public boolean canVote() {
        return mCanVote;
    }

    /**
     * @return Пост создавался как пост с голосованием
     */
    public boolean isVoteable() {
        // Убрать,когда сервер будет правильно возвращать правильное значение после создания анонимки
        if (isAnonymousPost()) return false;
        return mIsVoteable;
    }

    public boolean isFavorited() { return mFavorited;  }

    @Nullable
    public String getImageUrl() {
        return mImageUrl;
    }

    public Intent getShareIntent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (!TextUtils.isEmpty(getTitleSpanned())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, getTitleSpanned().toString());
        }
        intent.putExtra(Intent.EXTRA_TEXT, getEntryUrl());
        return intent;
    }

    public void initSpannedText() {
        getTitleSpanned();
        getTextSpanned();
        getSourceSpanned();
    }

    @Override
    public String toString() {
        return "Entry{" +
                "mAuthor=" + mAuthor +
                ", mId=" + mId +
                ", mType='" + mType + '\'' +
                ", mTlog=" + mTlog +
                ", mCommentsCount=" + mCommentsCount +
                ", mCreatedAt=" + mCreatedAt +
                ", mUpdatedAt=" + mUpdatedAt +
                ", mEntryUrl='" + mEntryUrl + '\'' +
                ", mRating=" + mRating +
                ", mTitle='" + mTitle + '\'' +
                ", mVideoUrl='" + mVideoUrl + '\'' +
                ", mCoverUrl='" + mCoverUrl + '\'' +
                ", mIframely=" + mIframely +
                ", mText='" + mText + '\'' +
                ", mSource='" + mSource + '\'' +
                ", mVia='" + mVia + '\'' +
                ", mPrivacy='" + mPrivacy + '\'' +
                ", mImages=" + mImages +
                ", mFavorited=" + mFavorited +
                ", mIsVoteable=" + mIsVoteable +
                ", mCanVote=" + mCanVote +
                ", mCanReport=" + mCanReport +
                ", mCanEdit=" + mCanEdit +
                ", mCanDelete=" + mCanDelete +
                ", mImageUrl='" + mImageUrl + '\'' +
                '}';
    }

    public Entry() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Entry entry = (Entry) o;

        if (mId != entry.mId) return false;
        if (mCommentsCount != entry.mCommentsCount) return false;
        if (mFavorited != entry.mFavorited) return false;
        if (mIsVoteable != entry.mIsVoteable) return false;
        if (mCanVote != entry.mCanVote) return false;
        if (mCanReport != entry.mCanReport) return false;
        if (mCanEdit != entry.mCanEdit) return false;
        if (mCanDelete != entry.mCanDelete) return false;
        if (mType != null ? !mType.equals(entry.mType) : entry.mType != null) return false;
        if (mAuthor != null ? !mAuthor.equals(entry.mAuthor) : entry.mAuthor != null) return false;
        if (mTlog != null ? !mTlog.equals(entry.mTlog) : entry.mTlog != null) return false;
        if (mCreatedAt != null ? !mCreatedAt.equals(entry.mCreatedAt) : entry.mCreatedAt != null)
            return false;
        if (mUpdatedAt != null ? !mUpdatedAt.equals(entry.mUpdatedAt) : entry.mUpdatedAt != null)
            return false;
        if (mEntryUrl != null ? !mEntryUrl.equals(entry.mEntryUrl) : entry.mEntryUrl != null)
            return false;
        if (mRating != null ? !mRating.equals(entry.mRating) : entry.mRating != null) return false;
        if (mTitle != null ? !mTitle.equals(entry.mTitle) : entry.mTitle != null) return false;
        if (mVideoUrl != null ? !mVideoUrl.equals(entry.mVideoUrl) : entry.mVideoUrl != null)
            return false;
        if (mCoverUrl != null ? !mCoverUrl.equals(entry.mCoverUrl) : entry.mCoverUrl != null)
            return false;
        if (mIframely != null ? !mIframely.equals(entry.mIframely) : entry.mIframely != null)
            return false;
        if (mText != null ? !mText.equals(entry.mText) : entry.mText != null) return false;
        if (mSource != null ? !mSource.equals(entry.mSource) : entry.mSource != null) return false;
        if (mVia != null ? !mVia.equals(entry.mVia) : entry.mVia != null) return false;
        if (mPrivacy != null ? !mPrivacy.equals(entry.mPrivacy) : entry.mPrivacy != null)
            return false;
        if (mImages != null ? !mImages.equals(entry.mImages) : entry.mImages != null) return false;
        if (mImageUrl != null ? !mImageUrl.equals(entry.mImageUrl) : entry.mImageUrl != null)
            return false;
        return mPreviewImage != null ? mPreviewImage.equals(entry.mPreviewImage) : entry.mPreviewImage == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (mId ^ (mId >>> 32));
        result = 31 * result + (mType != null ? mType.hashCode() : 0);
        result = 31 * result + (mAuthor != null ? mAuthor.hashCode() : 0);
        result = 31 * result + (mTlog != null ? mTlog.hashCode() : 0);
        result = 31 * result + mCommentsCount;
        result = 31 * result + (mCreatedAt != null ? mCreatedAt.hashCode() : 0);
        result = 31 * result + (mUpdatedAt != null ? mUpdatedAt.hashCode() : 0);
        result = 31 * result + (mEntryUrl != null ? mEntryUrl.hashCode() : 0);
        result = 31 * result + (mRating != null ? mRating.hashCode() : 0);
        result = 31 * result + (mTitle != null ? mTitle.hashCode() : 0);
        result = 31 * result + (mVideoUrl != null ? mVideoUrl.hashCode() : 0);
        result = 31 * result + (mCoverUrl != null ? mCoverUrl.hashCode() : 0);
        result = 31 * result + (mIframely != null ? mIframely.hashCode() : 0);
        result = 31 * result + (mText != null ? mText.hashCode() : 0);
        result = 31 * result + (mSource != null ? mSource.hashCode() : 0);
        result = 31 * result + (mVia != null ? mVia.hashCode() : 0);
        result = 31 * result + (mPrivacy != null ? mPrivacy.hashCode() : 0);
        result = 31 * result + (mImages != null ? mImages.hashCode() : 0);
        result = 31 * result + (mFavorited ? 1 : 0);
        result = 31 * result + (mIsVoteable ? 1 : 0);
        result = 31 * result + (mCanVote ? 1 : 0);
        result = 31 * result + (mCanReport ? 1 : 0);
        result = 31 * result + (mCanEdit ? 1 : 0);
        result = 31 * result + (mCanDelete ? 1 : 0);
        result = 31 * result + (mImageUrl != null ? mImageUrl.hashCode() : 0);
        result = 31 * result + (mPreviewImage != null ? mPreviewImage.hashCode() : 0);
        return result;
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
        dest.writeParcelable(this.mTlog, 0);
        dest.writeInt(this.mCommentsCount);
        dest.writeLong(mCreatedAt != null ? mCreatedAt.getTime() : -1);
        dest.writeLong(mUpdatedAt != null ? mUpdatedAt.getTime() : -1);
        dest.writeString(this.mEntryUrl);
        dest.writeParcelable(this.mRating, 0);
        dest.writeString(this.mTitle);
        dest.writeString(this.mVideoUrl);
        dest.writeString(this.mCoverUrl);
        dest.writeParcelable(this.mIframely, 0);
        dest.writeString(this.mText);
        dest.writeString(this.mSource);
        dest.writeString(this.mVia);
        dest.writeString(this.mPrivacy);
        dest.writeTypedList(mImages);
        dest.writeByte(mFavorited ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsVoteable ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanVote ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanReport ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanEdit ? (byte) 1 : (byte) 0);
        dest.writeByte(mCanDelete ? (byte) 1 : (byte) 0);
        dest.writeString(this.mImageUrl);
        dest.writeParcelable(mPreviewImage, 0);
    }

    protected Entry(Parcel in) {
        this.mId = in.readLong();
        this.mType = in.readString();
        this.mAuthor = in.readParcelable(User.class.getClassLoader());
        this.mTlog = in.readParcelable(EntryTlog.class.getClassLoader());
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
        this.mFavorited = in.readByte() != 0;
        this.mIsVoteable = in.readByte() != 0;
        this.mCanVote = in.readByte() != 0;
        this.mCanReport = in.readByte() != 0;
        this.mCanEdit = in.readByte() != 0;
        this.mCanDelete = in.readByte() != 0;
        this.mImageUrl = in.readString();
        this.mPreviewImage = in.readParcelable(ImageInfo.Image2.class.getClassLoader());
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
