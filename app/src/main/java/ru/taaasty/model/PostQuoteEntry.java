package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

public class PostQuoteEntry extends PostEntry implements Parcelable {

    public String text = "";

    @Nullable
    public String source;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.text);
        dest.writeString(this.source);
        dest.writeString(this.privacy);
    }

    public PostQuoteEntry() {
    }

    private PostQuoteEntry(Parcel in) {
        this.text = in.readString();
        this.source = in.readString();
        this.privacy = in.readString();
    }

    public static final Parcelable.Creator<PostQuoteEntry> CREATOR = new Parcelable.Creator<PostQuoteEntry>() {
        public PostQuoteEntry createFromParcel(Parcel source) {
            return new PostQuoteEntry(source);
        }

        public PostQuoteEntry[] newArray(int size) {
            return new PostQuoteEntry[size];
        }
    };
}
