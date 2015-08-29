package ru.taaasty.rest.model;

import android.os.Parcel;
import android.support.annotation.Nullable;

import ru.taaasty.utils.UiUtils;

public class PostQuoteForm extends PostForm {

    public CharSequence text = "";

    @Nullable
    public CharSequence source;

    @Nullable
    public Long tlogId;

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

        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        if (source != null ? !source.equals(that.source) : that.source != null) return false;
        return !(tlogId != null ? !tlogId.equals(that.tlogId) : that.tlogId != null);

    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (tlogId != null ? tlogId.hashCode() : 0);
        return result;
    }

    public static class AsHtml implements PostFormHtml {

        public final String text;

        public final String source;

        public final String privacy;

        @Nullable
        public final Long tlogId;

        private AsHtml(PostQuoteForm source) {
            this.text = source.text == null ? null : UiUtils.safeToHtml(source.text);
            this.source = source.source == null ? null : UiUtils.safeToHtml(source.source);
            this.privacy = source.privacy;
            this.tlogId = source.tlogId;
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
            dest.writeValue(this.tlogId);
        }

        private AsHtml(Parcel in) {
            this.text = in.readString();
            this.source = in.readString();
            this.privacy = in.readString();
            this.tlogId = (Long)in.readValue(Long.class.getClassLoader());
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
