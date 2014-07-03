package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * Created by alexey on 10.07.14.
 */
public class CurrentUser {

    @SerializedName("id")
    private long mId = -1;

    @SerializedName("name")
    private String mName;

    @SerializedName("slug")
    private String mSlug;

    @SerializedName("title")
    private String mTitle;

    @SerializedName("is_female")
    private boolean mIsFemale;

    @SerializedName("is_daylog")
    private boolean mIsDaylog;

    @SerializedName("tlog_url")
    private boolean mTlogUrl;

    @SerializedName("created_at")
    private Date mCreateAt;

    @SerializedName("updated_at")
    private Date mUpdatedAt;

    @SerializedName("email")
    private String mEmail;

    @SerializedName("is_privacy")
    private boolean mIsPrivacy;

    @SerializedName("is_confirmed")
    private boolean mIsConfirmed;

    @SerializedName("available_notifications")
    private boolean mAvailableNotifications;

    @SerializedName("authentications")
    private List<Object> mAuthentifications;

    @SerializedName("api_key")
    private ApiKey mApiKey = ApiKey.DUMMY;

    @SerializedName("userpic")
    private Userpic mUserpic = Userpic.DUMMY;

    public ApiKey getApiKey() {
        return mApiKey;
    }

    public static class ApiKey {

        public static ApiKey DUMMY = new ApiKey();

        public String accessToken = "";

        public long userId;

        private Date expiredAt;
    }
}
