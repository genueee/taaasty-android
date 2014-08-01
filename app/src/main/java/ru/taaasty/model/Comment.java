package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by alexey on 01.08.14.
 */
public class Comment {

    @SerializedName("id")
    private long mId;

    @SerializedName("type")
    String mType;

    @SerializedName("author")
    User mAuthor = User.DUMMY;

    @SerializedName("comments_count")
    long commentsCount;

    @SerializedName("created_at")
    private Date mCreatedAt;

    @SerializedName("updated_at")
    private Date mUpdatedAt;

    @SerializedName("entry_url")
    private String mEntryUrl;

    @SerializedName("privacy")
    String mPrivacy;

    Rating mRating = Rating.DUMMY;

    @SerializedName("title")
    String mTitle;

    @SerializedName("text")
    String mText;
}
