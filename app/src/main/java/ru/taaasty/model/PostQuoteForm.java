package ru.taaasty.model;

import android.os.Parcel;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import ru.taaasty.utils.UiUtils;

public class PostQuoteForm extends PostForm {

    public CharSequence text = "";

    @Nullable
    public CharSequence source;

    public PostQuoteForm() {
    }

    @Override
    public AsHtml asHtmlForm() {
        return new AsHtml(this);
    }

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

    public static class AsHtml implements PostFormHtml {

        public final String text;

        public final String source;

        public final String privacy;

        private AsHtml(PostQuoteForm source) {
            this.text = source.text == null ? null : UiUtils.safeToHtml(source.text);
            this.source = source.source == null ? null : UiUtils.safeToHtml(source.source);
            this.privacy = source.privacy;
        }

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

        private AsHtml(Parcel in) {
            this.text = in.readString();
            this.source = in.readString();
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
