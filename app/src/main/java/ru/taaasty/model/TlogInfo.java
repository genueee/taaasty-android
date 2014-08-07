package ru.taaasty.model;


import android.os.Parcel;
import android.os.Parcelable;

public class TlogInfo implements Parcelable {

    public User author = User.DUMMY;

    public TlogDesign design = TlogDesign.DUMMY;

    public RelationshipsSummary relationshipsSummary = RelationshipsSummary.DUMMY;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.author, 0);
        dest.writeParcelable(this.design, 0);
        dest.writeParcelable(this.relationshipsSummary, 0);
    }

    public TlogInfo() {
    }

    private TlogInfo(Parcel in) {
        this.author = in.readParcelable(User.class.getClassLoader());
        this.design = in.readParcelable(TlogDesign.class.getClassLoader());
        this.relationshipsSummary = in.readParcelable(RelationshipsSummary.class.getClassLoader());
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
