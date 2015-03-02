package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

public class PostAnonymousTextForm extends PostForm {

    public CharSequence title = "";

    public CharSequence text = "";


    public PostAnonymousTextForm() {
        super();
    }

    private PostAnonymousTextForm(Parcel in) {
        this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
    }

    public static final Parcelable.Creator<PostAnonymousTextForm> CREATOR = new Parcelable.Creator<PostAnonymousTextForm>() {
        public PostAnonymousTextForm createFromParcel(Parcel source) {
            return new PostAnonymousTextForm(source);
        }

        public PostAnonymousTextForm[] newArray(int size) {
            return new PostAnonymousTextForm[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(title, dest, flags);
        TextUtils.writeToParcel(text, dest, flags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostAnonymousTextForm that = (PostAnonymousTextForm) o;

        if (!TextUtils.equals(text, that.text)) return false;
        if (!TextUtils.equals(title, that.title)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
