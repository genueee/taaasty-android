package ru.taaasty.rest.model;

import android.os.Parcel;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

import ru.taaasty.rest.model2.Userpic;

/**
 * {
 "id": 232419,
 "name": "brandytest",
 "slug": "brandytest",
 "title": null,
 "is_female": true,
 "is_daylog": false,
 "tlog_url": "http://brandytest.taaasty.com/",
 "created_at": "2014-06-09T05:16:47.000+04:00",
 "updated_at": "2014-07-10T10:52:53.000+04:00",
 "email": "test@brandymint.com",
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

    public static final int USER_UNAUTHORIZED_ID = -1;

    public static CurrentUser UNAUTHORIZED = new CurrentUser();

    static {
        UNAUTHORIZED.mId = -1;
        UNAUTHORIZED.mTitle = "";
        UNAUTHORIZED.mName = "";
        UNAUTHORIZED.mSlug = "";
        UNAUTHORIZED.mTlogUrl = "";
        UNAUTHORIZED.mCreateAt = new Date(1167609600);
        UNAUTHORIZED.mUserpic = Userpic.create("", Userpic.DefaultColors.create("#00000000", "#ffffff"));
        UNAUTHORIZED.mIsConfirmed = false;
        UNAUTHORIZED.mRelationshipsSummary = new RelationshipsSummary();
        UNAUTHORIZED.mDesign = TlogDesign.createLightTheme(TlogDesign.DUMMY,
                "http://taaasty.com/images/hero-cover.jpg");
    }


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
