package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

/**
 * Created by alexey on 03.09.14.
 */
public class PostTextForm extends PostForm implements Parcelable {

    public CharSequence title = "";

    public CharSequence text = "";

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(title, dest, flags);
        TextUtils.writeToParcel(text, dest, flags);
        dest.writeString(this.privacy);
    }

    public PostTextForm() {
    }

    private PostTextForm(Parcel in) {
        this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.privacy = in.readString();
    }

    public static final Parcelable.Creator<PostTextForm> CREATOR = new Parcelable.Creator<PostTextForm>() {
        public PostTextForm createFromParcel(Parcel source) {
            return new PostTextForm(source);
        }

        public PostTextForm[] newArray(int size) {
            return new PostTextForm[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostTextForm that = (PostTextForm) o;

        if (!TextUtils.equals(text, that.text)) return false;
        if (!TextUtils.equals(title, that.title)) return false;
        if (!TextUtils.equals(privacy, that.privacy)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (privacy != null ? privacy.hashCode() : 0);
        return result;
    }
}
