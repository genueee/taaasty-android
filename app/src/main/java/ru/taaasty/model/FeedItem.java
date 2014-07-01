package ru.taaasty.model;

import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FeedItem {

    @SerializedName("id")
    private long mId;

    @SerializedName("type")
    private String mType;

    @SerializedName("author")
    private Author mAuthor;

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

    @SerializedName("images")
    private List<Image> mImages;

    private transient Spanned mTextSpanned = null;

    private transient Spanned mSourceSpanned = null;

    public long getId() {
        return mId;
    }

    public String getType() {
        return mType;
    }

    public Author getAuthor() {
        return mAuthor == null ? Author.DUMMY : mAuthor;
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

    public static class Author {

        public static Author DUMMY = new Author();

        @SerializedName("id")
        private long mId = -1;

        @SerializedName("gender")
        private String mGender = "m";

        @SerializedName("name")
        private String mName = "";

        @SerializedName("slug")
        private String mSlug = "";

        @SerializedName("userpic")
        @Nullable
        private Userpic mUserpic = null;

        @SerializedName("tlog_url")
        private String mTlogUrl = "";

        public long getId() {
            return mId;
        }

        // XXX
        public String getGender() {
            return mGender;
        }

        public String getName() {
            return mName;
        }

        public String getSlug() {
            return mSlug;
        }

        public Userpic getUserpic() {
            return mUserpic == null ? Userpic.DUMMY : mUserpic;
        }

        public String getTlogUrl() {
            return mTlogUrl;
        }

    }

    public static class Userpic {
        public static Userpic DUMMY = new Userpic();

        @Nullable
        public String largeUrl;

        @Nullable
        public String thumb128Url;

        @Nullable
        public String thumb64Url;

        @Nullable
        public String thumb32Url;

        @Nullable
        public String thumb16Url;

        @Nullable
        public String touchUrl;

    }

    public static class Rating {

        public static final Rating DUMMY = new Rating();

        public int votes;

        public float rating;

        public long entryId;

        public boolean isVoted;

        public boolean isVoteable;

    }

    public static class Image {
        public long id;
        public Date createAt;
        public String thumbUrl;
        public String mediumUrl;
        public String tlogUrl;
    }
}
