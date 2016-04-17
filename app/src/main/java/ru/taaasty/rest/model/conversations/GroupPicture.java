package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Иконка чата
 */
public class GroupPicture implements Parcelable {

    public String url;
    //public String path;
    //public Geometry geometry;
    //public String title;
    //public String source;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
    }

    public GroupPicture() {
    }

    protected GroupPicture(Parcel in) {
        this.url = in.readString();
    }

    public static final Creator<GroupPicture> CREATOR = new Creator<GroupPicture>() {
        @Override
        public GroupPicture createFromParcel(Parcel source) {
            return new GroupPicture(source);
        }

        @Override
        public GroupPicture[] newArray(int size) {
            return new GroupPicture[size];
        }
    };
}
