package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by alexey on 03.09.14.
 */
public class PostTextEntry extends PostEntry implements Parcelable {

    public String title = "";

    public String text = "";

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.text);
        dest.writeString(this.privacy);
    }

    public PostTextEntry() {
    }

    private PostTextEntry(Parcel in) {
        this.title = in.readString();
        this.text = in.readString();
        this.privacy = in.readString();
    }

    public static final Parcelable.Creator<PostTextEntry> CREATOR = new Parcelable.Creator<PostTextEntry>() {
        public PostTextEntry createFromParcel(Parcel source) {
            return new PostTextEntry(source);
        }

        public PostTextEntry[] newArray(int size) {
            return new PostTextEntry[size];
        }
    };
}
