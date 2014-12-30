package ru.taaasty.model;

import android.os.Parcel;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

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
public class CurrentUser extends User implements android.os.Parcelable {

    @SerializedName("is_confirmed")
    boolean mIsConfirmed;

    @SerializedName("confirmation_email")
    String mConfirmationEmail;

    @SerializedName("available_notifications")
    boolean mAvailableNotifications;

    @SerializedName("api_key")
    ApiKey mApiKey = ApiKey.DUMMY;

    public ApiKey getApiKey() {
        return mApiKey;
    }

    public static class ApiKey implements android.os.Parcelable {

        public static ApiKey DUMMY = new ApiKey();

        public String accessToken = "";

        public long userId;

        private Date expiredAt;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.accessToken);
            dest.writeLong(this.userId);
            dest.writeLong(expiredAt != null ? expiredAt.getTime() : -1);
        }

        public ApiKey() {
        }

        private ApiKey(Parcel in) {
            this.accessToken = in.readString();
            this.userId = in.readLong();
            long tmpExpiredAt = in.readLong();
            this.expiredAt = tmpExpiredAt == -1 ? null : new Date(tmpExpiredAt);
        }

        public static final Creator<ApiKey> CREATOR = new Creator<ApiKey>() {
            public ApiKey createFromParcel(Parcel source) {
                return new ApiKey(source);
            }

            public ApiKey[] newArray(int size) {
                return new ApiKey[size];
            }
        };
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

    public boolean isEmailConfirmed() {
        return mIsConfirmed;
    }

    public String getConfirmationEmail() {
        return mConfirmationEmail;
    }

    public String getLastEmail() {
        return mIsConfirmed || TextUtils.isEmpty(mConfirmationEmail) ? mEmail : mConfirmationEmail;
    }

    public boolean isPrivacy() {
        return mIsPrivacy;
    }

    public boolean areNotificationsAvailable() {
        return mAvailableNotifications;
    }

    public Userpic getUserpic() {
        return mUserpic;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);

        dest.writeByte(mIsConfirmed ? (byte) 1 : (byte) 0);
        dest.writeByte(mAvailableNotifications ? (byte) 1 : (byte) 0);
        dest.writeString(mConfirmationEmail);
        dest.writeParcelable(this.mApiKey, flags);
    }

    public CurrentUser() {
    }

    private CurrentUser(Parcel in) {
        super(in);
        this.mIsConfirmed = in.readByte() != 0;
        this.mAvailableNotifications = in.readByte() != 0;
        this.mConfirmationEmail = in.readString();
        this.mApiKey = in.readParcelable(ApiKey.class.getClassLoader());
    }

    public static final Creator<CurrentUser> CREATOR = new Creator<CurrentUser>() {
        public CurrentUser createFromParcel(Parcel source) {
            return new CurrentUser(source);
        }

        public CurrentUser[] newArray(int size) {
            return new CurrentUser[size];
        }
    };
}
