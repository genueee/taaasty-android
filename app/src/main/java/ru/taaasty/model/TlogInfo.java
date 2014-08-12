package ru.taaasty.model;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

public class TlogInfo implements Parcelable {

    public User author = User.DUMMY;

    public TlogDesign design = TlogDesign.DUMMY;

    public RelationshipsSummary relationshipsSummary = RelationshipsSummary.DUMMY;

    @SerializedName("my_relationship")
    String mMyRelationship = Relationship.RELATIONSHIP_NONE;

    @SerializedName("his_relationship")
    String mHisRelationship = Relationship.RELATIONSHIP_NONE;

    public String getMyRelationship() {
        return mMyRelationship;
    }

    public String getHisRelationship() {
        return mHisRelationship;
    }

    public boolean isMeSubscribed() {
        return Relationship.RELATIONSHIP_FRIEND.equals(mMyRelationship)
                || Relationship.RELATIONSHIP_REQUESTED.equals(mMyRelationship);
    }

    public void setMyRelationship(String relationship) {
        mMyRelationship = relationship;
    }

    public void setHisRelationship(String relationship) {
        mHisRelationship = relationship;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.author, 0);
        dest.writeParcelable(this.design, 0);
        dest.writeParcelable(this.relationshipsSummary, 0);
        dest.writeString(this.mMyRelationship);
        dest.writeString(this.mHisRelationship);
    }

    public TlogInfo() {
    }

    private TlogInfo(Parcel in) {
        this.author = in.readParcelable(User.class.getClassLoader());
        this.design = in.readParcelable(TlogDesign.class.getClassLoader());
        this.relationshipsSummary = in.readParcelable(RelationshipsSummary.class.getClassLoader());
        this.mMyRelationship = in.readString();
        this.mHisRelationship = in.readString();
    }

    public static final Parcelable.Creator<TlogInfo> CREATOR = new Parcelable.Creator<TlogInfo>() {
        public TlogInfo createFromParcel(Parcel source) {
            return new TlogInfo(source);
        }

        public TlogInfo[] newArray(int size) {
            return new TlogInfo[size];
        }
    };
}
