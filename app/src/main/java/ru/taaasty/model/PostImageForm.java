package ru.taaasty.model;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

public class PostImageForm extends PostForm implements android.os.Parcelable {

    // XXX: file, files

    @Nullable
    public CharSequence title;

    @Nullable
    public Uri imageUri;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        TextUtils.writeToParcel(title, dest, flags);
        dest.writeParcelable(this.imageUri, flags);
        dest.writeString(this.privacy);
    }

    public PostImageForm() {
    }


    private PostImageForm(Parcel in) {
        this.title = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.imageUri = in.readParcelable(Uri.class.getClassLoader());
        this.privacy = in.readString();
    }

    public static final Creator<PostImageForm> CREATOR = new Creator<PostImageForm>() {
        public PostImageForm createFromParcel(Parcel source) {
            return new PostImageForm(source);
        }

        public PostImageForm[] newArray(int size) {
            return new PostImageForm[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostImageForm that = (PostImageForm) o;

        if (!TextUtils.equals(title, that.title)) return false;
        if (imageUri != null ? !imageUri.equals(that.imageUri) : that.imageUri != null) return false;
        if (!TextUtils.equals(privacy, that.privacy)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
        result = 31 * result + (privacy != null ? privacy.hashCode() : 0);
        return result;
    }
}
