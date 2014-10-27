package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class Relationship implements Parcelable {

    public static final String RELATIONSHIP_NONE = "none";
    public static final String RELATIONSHIP_FRIEND = "friend";
    public static final String RELATIONSHIP_GUESSED = "guessed";
    public static final String RELATIONSHIP_IGNORED = "ignored";
    public static final String RELATIONSHIP_REQUESTED = "requested";
    @SerializedName("id")
    private long mId;

    @SerializedName("user_id")
    private long mUserId = -1;

    @SerializedName("reader_id")
    private long mReaderId = -1;

    @SerializedName("state")
    private String mState = "";

    @SerializedName("reader")
    private User mReader = User.DUMMY;

    @SerializedName("user")
    private User mUser = User.DUMMY;

    public Relationship() {
    }

    public static boolean isMeSubscribed(String myRelationship) {
        return myRelationship != null && (RELATIONSHIP_FRIEND.equals(myRelationship)
                || RELATIONSHIP_REQUESTED.equals(myRelationship));
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

    public User getUser() {
        return mUser;
    }

    public long getFromId() {
        return mReaderId;
    }

    public long getToId() {
        return mUserId;
    }

    public boolean isMyRelation(long myUserId) {
        return isMyRelationToHim(myUserId) || isHisRelationToMe(myUserId);
    }

    public boolean isMyRelationToHim(long myUserId) {
        return (myUserId != -1) && (myUserId == getFromId());
    }

    public boolean isHisRelationToMe(long myUserId) {
        return (myUserId != -1) && (myUserId == getToId());
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
        dest.writeParcelable(this.mUser, 0);
    }

    private Relationship(Parcel in) {
        this.mId = in.readLong();
        this.mUserId = in.readLong();
        this.mReaderId = in.readLong();
        this.mState = in.readString();
        this.mReader = in.readParcelable(User.class.getClassLoader());
        this.mUser = in.readParcelable(User.class.getClassLoader());
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
