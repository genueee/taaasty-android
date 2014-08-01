package ru.taaasty.model;

import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Entry {

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
        return mRating != null ? Rating.DUMMY : new Rating();
    }

    public static class Image {
        public long id;

        public Date createAt;

        String title;

        String source;

        public Image2 image = Image2.DUMMY;
    }

    public static class Image2 {

        public static final Image2 DUMMY = new Image2();

        public String url = "";

        /**
         * Thumbor path
         */
        public String path = "";

        public ImageGeometry geometry = new ImageGeometry();

    }

    public static class ImageGeometry {
        public int width;
        public int height;
    }
}
