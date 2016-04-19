package ru.taaasty.rest.model;

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Created by alexey on 18.11.15.
 */
public class ApiKey implements android.os.Parcelable {

    public static ApiKey DUMMY = new ApiKey();
    @SerializedName("access_token")
    public String accessToken = "";
    @SerializedName("user_id")
    public long userId;
    @SerializedName("expired_at")
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
