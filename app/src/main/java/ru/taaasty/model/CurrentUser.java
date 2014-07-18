package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

/**
 * {
 "id": 232419,
 "name": "brandytest",
 "slug": "brandytest",
 "title": null,
 "is_female": true,
 "is_daylog": false,
 "tlog_url": "http://brandytest.taaasty.ru/",
 "created_at": "2014-06-09T05:16:47.000+04:00",
 "updated_at": "2014-07-10T10:52:53.000+04:00",
 "email": "test@brandymint.ru",
 "is_privacy": false,
 "is_confirmed": true,
 "available_notifications": false,
 "authentications": [],
 "api_key": {
 "access_token": "",
 "user_id": 232419,
 "expires_at": "2014-12-06T05:30:15.000+04:00"
 },
 "userpic": {
 "large_url": "/images/userpics/large/missing.png",
 "thumb128_url": "/images/userpics/thumb128/missing.png",
 "thumb64_url": "/images/userpics/thumb64/missing.png",
 "thumb32_url": "/images/userpics/thumb32/missing.png",
 "thumb16_url": "/images/userpics/thumb16/missing.png",
 "touch_url": "/images/userpics/touch/missing.png"
 }
 }
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
    private String mTlogUrl;

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

    @SerializedName("design")
    private TlogDesign mTlogDesign = TlogDesign.DUMMY;

    public ApiKey getApiKey() {
        return mApiKey;
    }

    public static class ApiKey {

        public static ApiKey DUMMY = new ApiKey();

        public String accessToken = "";

        public long userId;

        private Date expiredAt;
    }

    public long getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getSlug() {
        return mSlug;
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean isFemale() {
        return mIsFemale;
    }

    public boolean isDaylog() {
        return mIsDaylog;
    }

    public String getTlogUrl() {
        return mTlogUrl;
    }

    public Date getmCreateAt() {
        return mCreateAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public String getEmail() {
        return mEmail;
    }

    public boolean isPrivacy() {
        return mIsPrivacy;
    }

    public boolean isConfirmed() {
        return mIsConfirmed;
    }

    public boolean areNotificationsAvailable() {
        return mAvailableNotifications;
    }

    public List<Object> getmAuthentifications() {
        return mAuthentifications;
    }

    public Userpic getUserpic() {
        return mUserpic;
    }

    public TlogDesign getDesign() {
        return mTlogDesign;
    }
}
