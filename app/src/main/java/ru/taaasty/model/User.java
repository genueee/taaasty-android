package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
* Created by alexey on 01.08.14.
*/
public class User implements Parcelable {

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
    long mTotalEntriesCount;

    @SerializedName("private_entries_count")
    long privateEntriesCount;

    @SerializedName("public_entries_count")
    long publicEntriesCount;

    @SerializedName("userpic")
    Userpic mUserpic = Userpic.DUMMY;

    @SerializedName("relationships_summary")
    RelationshipsSummary mRelationshipsSummary;

    @SerializedName("design")
    TlogDesign mDesign;

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

    public String getTitle() {
        return mTitle;
    }

    public Date getCreatedAt() {
        return mCreateAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public long getTotalEntriesCount() {
        return mTotalEntriesCount;
    }

    public long getDaysOnTasty() {
        long diffMs = Math.abs(System.currentTimeMillis() - mCreateAt.getTime());
        return Math.round(diffMs / (24f * 60f * 60f * 1000f)); // XXX: wrong
    }

    @Nullable
    public RelationshipsSummary getRelationshipsSummary() {
        return mRelationshipsSummary;
    }

    @Nullable
    public TlogDesign getDesign() {
        return mDesign;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.mId);
        dest.writeString(this.mName);
        dest.writeString(this.mSlug);
        dest.writeString(this.mTitle);
        dest.writeByte(mIsFemale ? (byte) 1 : (byte) 0);
        dest.writeByte(mIsDaylog ? (byte) 1 : (byte) 0);
        dest.writeString(this.mTlogUrl);
        dest.writeLong(mCreateAt != null ? mCreateAt.getTime() : -1);
        dest.writeLong(mUpdatedAt != null ? mUpdatedAt.getTime() : -1);
        dest.writeString(this.mEmail);
        dest.writeByte(mIsPrivacy ? (byte) 1 : (byte) 0);
        dest.writeLong(this.mTotalEntriesCount);
        dest.writeLong(this.privateEntriesCount);
        dest.writeLong(this.publicEntriesCount);
        dest.writeParcelable(this.mUserpic, flags);
        dest.writeParcelable(this.mDesign, flags);
        dest.writeParcelable(this.mRelationshipsSummary, flags);
    }

    public User() {
    }

    private User(Parcel in) {
        this.mId = in.readLong();
        this.mName = in.readString();
        this.mSlug = in.readString();
        this.mTitle = in.readString();
        this.mIsFemale = in.readByte() != 0;
        this.mIsDaylog = in.readByte() != 0;
        this.mTlogUrl = in.readString();
        long tmpMCreateAt = in.readLong();
        this.mCreateAt = tmpMCreateAt == -1 ? null : new Date(tmpMCreateAt);
        long tmpMUpdatedAt = in.readLong();
        this.mUpdatedAt = tmpMUpdatedAt == -1 ? null : new Date(tmpMUpdatedAt);
        this.mEmail = in.readString();
        this.mIsPrivacy = in.readByte() != 0;
        this.mTotalEntriesCount = in.readLong();
        this.privateEntriesCount = in.readLong();
        this.publicEntriesCount = in.readLong();
        this.mUserpic = in.readParcelable(Userpic.class.getClassLoader());
        this.mDesign = in.readParcelable(TlogDesign.class.getClassLoader());
        this.mRelationshipsSummary = in.readParcelable(RelationshipsSummary.class.getClassLoader());
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        public User[] newArray(int size) {
            return new User[size];
        }
    };


    @Override
    public String toString() {
        return "User{" +
                "mId=" + mId +
                ", mName='" + mName + '\'' +
                ", mSlug='" + mSlug + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mIsFemale=" + mIsFemale +
                ", mIsDaylog=" + mIsDaylog +
                ", mTlogUrl='" + mTlogUrl + '\'' +
                ", mCreateAt=" + mCreateAt +
                ", mUpdatedAt=" + mUpdatedAt +
                ", mEmail='" + mEmail + '\'' +
                ", mIsPrivacy=" + mIsPrivacy +
                ", mTotalEntriesCount=" + mTotalEntriesCount +
                ", privateEntriesCount=" + privateEntriesCount +
                ", publicEntriesCount=" + publicEntriesCount +
                ", mUserpic=" + mUserpic +
                ", mDesign=" + mDesign +
                ", mRelationshipsSummary=" + mRelationshipsSummary +
                '}';
    }
}
