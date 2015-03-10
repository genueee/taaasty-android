package ru.taaasty.model;

import android.os.Parcel;
import android.text.TextUtils;

import ru.taaasty.utils.UiUtils;

public class PostAnonymousTextForm extends PostForm {

    public CharSequence title = "";

    public CharSequence text = "";


    public PostAnonymousTextForm() {
        super();
    }

    @Override
    public AsHtml asHtmlForm() {
        return new AsHtml(this);
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

    public static class AsHtml implements PostForm.PostFormHtml {

        public final String title;

        public final String text;

        private AsHtml(PostAnonymousTextForm source) {
            this.title = source.title == null ? null : UiUtils.safeToHtml(source.title);
            this.text = source.text == null ? null : UiUtils.safeToHtml(source.text);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.title);
            dest.writeString(this.text);
        }

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.text = in.readString();
        }

        public static final Creator<AsHtml> CREATOR = new Creator<AsHtml>() {
            public AsHtml createFromParcel(Parcel source) {
                return new AsHtml(source);
            }

            public AsHtml[] newArray(int size) {
                return new AsHtml[size];
            }
        };

        @Override
        public boolean isPrivatePost() {
            return false;
        }
    }
}
