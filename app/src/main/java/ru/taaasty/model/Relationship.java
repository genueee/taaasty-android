package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Relationship implements Parcelable {

    @SerializedName("id")
    private long mId;

    @SerializedName("user_id")
    private long mUserId;

    @SerializedName("reader_id")
    private long mReaderId;

    @SerializedName("state")
    private String mState = "";

    @SerializedName("reader")
    private User mReader = User.DUMMY;

    public Relationship() {
    }

    public long getId() {
        return mId;
    }

    public long getUserId() {
        return mUserId;
    }

    public long getReaderId() {
        return mReaderId;
    }

    public String getState() {
        return mState;
    }

    public User getReader() {
        return mReader;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeLong(this.mUserId);
        dest.writeLong(this.mReaderId);
        dest.writeString(this.mState);
        dest.writeParcelable(this.mReader, 0);
    }

    private Relationship(Parcel in) {
        this.mId = in.readLong();
        this.mUserId = in.readLong();
        this.mReaderId = in.readLong();
        this.mState = in.readString();
        this.mReader = in.readParcelable(User.class.getClassLoader());
    }

    public static final Parcelable.Creator<Relationship> CREATOR = new Parcelable.Creator<Relationship>() {
        public Relationship createFromParcel(Parcel source) {
            return new Relationship(source);
        }

        public Relationship[] newArray(int size) {
            return new Relationship[size];
        }
    };
}
