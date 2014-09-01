package ru.taaasty.model;

import android.os.Parcel;
import android.support.annotation.Nullable;

public class PostImageEntry extends PostEntry implements android.os.Parcelable {

    // XXX: file, files

    @Nullable
    public String title;

    @Nullable
    public String imageUrl;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.imageUrl);
        dest.writeString(this.privacy);
    }

    public PostImageEntry() {
    }

    private PostImageEntry(Parcel in) {
        this.title = in.readString();
        this.imageUrl = in.readString();
        this.privacy = in.readString();
    }

    public static final Creator<PostImageEntry> CREATOR = new Creator<PostImageEntry>() {
        public PostImageEntry createFromParcel(Parcel source) {
            return new PostImageEntry(source);
        }

        public PostImageEntry[] newArray(int size) {
            return new PostImageEntry[size];
        }
    };
}
