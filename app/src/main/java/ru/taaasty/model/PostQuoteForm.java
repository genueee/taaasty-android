package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

public class PostQuoteForm extends PostForm implements Parcelable {

    public CharSequence text = "";

    @Nullable
    public CharSequence source;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(text, dest, flags);
        TextUtils.writeToParcel(source, dest, flags);
        dest.writeString(this.privacy);
    }

    public PostQuoteForm() {
    }

    private PostQuoteForm(Parcel in) {
        this.text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.source = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.privacy = in.readString();
    }

    public static final Parcelable.Creator<PostQuoteForm> CREATOR = new Parcelable.Creator<PostQuoteForm>() {
        public PostQuoteForm createFromParcel(Parcel source) {
            return new PostQuoteForm(source);
        }

        public PostQuoteForm[] newArray(int size) {
            return new PostQuoteForm[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostQuoteForm that = (PostQuoteForm) o;

        if (!TextUtils.equals(text, that.text)) return false;
        if (!TextUtils.equals(source, that.source)) return false;
        if (!TextUtils.equals(privacy, that.privacy)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = source != null ? source.hashCode() : 0;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (privacy != null ? privacy.hashCode() : 0);
        return result;
    }
}
