package ru.taaasty.model;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import ru.taaasty.utils.UiUtils;

public class PostImageForm extends PostForm {

    // XXX: file, files

    @Nullable
    public CharSequence title;

    @Nullable
    public Uri imageUri;


    public PostImageForm() {
    }

    @Override
    public AsHtml asHtmlForm() {
        return new AsHtml(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostImageForm that = (PostImageForm) o;

        if (!TextUtils.equals(title, that.title)) return false;
        if (imageUri != null ? !imageUri.equals(that.imageUri) : that.imageUri != null)
            return false;
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

    public static class AsHtml implements PostFormHtml {

        public final String title;

        public final Uri imageUri;

        public final String privacy;

        private AsHtml(PostImageForm source) {
            this.title = source.title == null ? null : UiUtils.safeToHtml(source.title);
            this.imageUri = source.imageUri;
            this.privacy = source.privacy;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.title);
            dest.writeParcelable(this.imageUri, 0);
            dest.writeString(this.privacy);
        }

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.imageUri = in.readParcelable(Uri.class.getClassLoader());
            this.privacy = in.readString();
        }

        @Override
        public boolean isPrivatePost() {
            return Entry.PRIVACY_PRIVATE.equals(privacy);
        }

        public static final Creator<AsHtml> CREATOR = new Creator<AsHtml>() {
            public AsHtml createFromParcel(Parcel source) {
                return new AsHtml(source);
            }

            public AsHtml[] newArray(int size) {
                return new AsHtml[size];
            }
        };
    }

}
