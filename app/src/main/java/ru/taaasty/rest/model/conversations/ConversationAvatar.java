package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;

import ru.taaasty.rest.model.Geometry;

/**
 * Иконка чата
 */
public class ConversationAvatar implements Parcelable {

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

    public ConversationAvatar() {
    }

    protected ConversationAvatar(Parcel in) {
        this.url = in.readString();
    }

    public static final Creator<ConversationAvatar> CREATOR = new Creator<ConversationAvatar>() {
        @Override
        public ConversationAvatar createFromParcel(Parcel source) {
            return new ConversationAvatar(source);
        }

        @Override
        public ConversationAvatar[] newArray(int size) {
            return new ConversationAvatar[size];
        }
    };
}
