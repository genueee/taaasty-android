package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by alexey on 01.08.14.
 */
public class Comment {

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

    public long getId() {
        return mId;
    }

    public User getAuthor() {
        return mAuthor;
    }

    public String getText() {
        return mText;
    }

    public boolean IsDisabled() {
        return mIsDisabled;
    }
}
