package ru.taaasty.model;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;

public class PostImageEntry extends PostEntry implements android.os.Parcelable {

    // XXX: file, files

    @Nullable
    public String title;

    @Nullable
    public Uri imageUri;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeParcelable(this.imageUri, flags);
        dest.writeString(this.privacy);
    }

    public PostImageEntry() {
    }

    private PostImageEntry(Parcel in) {
        this.title = in.readString();
        this.imageUri = in.readParcelable(Uri.class.getClassLoader());
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
