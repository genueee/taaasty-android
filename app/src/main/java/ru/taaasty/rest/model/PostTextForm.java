package ru.taaasty.rest.model;

import android.os.Parcel;
import android.text.TextUtils;

import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 03.09.14.
 */
public class PostTextForm extends PostForm {

    public CharSequence title = "";

    public CharSequence text = "";

    public PostTextForm() {
    }

    @Override
    public AsHtml asHtmlForm() {
        return new AsHtml(this);
    }

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

    public static class AsHtml implements PostFormHtml {

        public final String title;

        public final String text;

        public final String privacy;

        private AsHtml(PostTextForm source) {
            this.title = source.title == null ? null : UiUtils.safeToHtml(source.title);
            this.text = source.text == null ? null : UiUtils.safeToHtml(source.text);
            this.privacy = source.privacy;
        }

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

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.text = in.readString();
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
