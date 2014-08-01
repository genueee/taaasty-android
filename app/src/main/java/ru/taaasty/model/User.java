package ru.taaasty.model;

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
* Created by alexey on 01.08.14.
*/
public class User {

    public static User DUMMY = new User();

    @SerializedName("id")
    long mId = -1;

    @SerializedName("name")
    String mName;

    @SerializedName("slug")
    String mSlug;

    @SerializedName("title")
    String mTitle;

    @SerializedName("is_female")
    boolean mIsFemale;

    @SerializedName("is_daylog")
    boolean mIsDaylog;

    @SerializedName("tlog_url")
    String mTlogUrl;

    @SerializedName("created_at")
    Date mCreateAt;

    @SerializedName("updated_at")
    Date mUpdatedAt;

    @SerializedName("email")
    String mEmail;

    @SerializedName("is_privacy")
    boolean mIsPrivacy;

    @SerializedName("total_entries_count")
    long totalEntriesCount;

    @SerializedName("private_entries_count")
    long privateEntriesCount;

    @SerializedName("public_entries_count")
    long publicEntriesCount;

    @SerializedName("userpic")
    Userpic mUserpic = Userpic.DUMMY;

    public long getId() {
        return mId;
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
