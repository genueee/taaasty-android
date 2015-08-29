package ru.taaasty.rest.model;

import android.os.Parcel;

import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 03.09.14.
 */
public class PostTextForm extends PostForm {

    public CharSequence title = "";

    public CharSequence text = "";

    public Long tlogId;

    public PostTextForm() {
    }

    @Override
    public AsHtml asHtmlForm() {
        return new AsHtml(this);
    }

    public static class AsHtml implements PostFormHtml {

        public final String title;

        public final String text;

        public final String privacy;

        public final Long tlogId;

        private AsHtml(PostTextForm source) {
            this.title = source.title == null ? null : UiUtils.safeToHtml(source.title);
            this.text = source.text == null ? null : UiUtils.safeToHtml(source.text);
            this.privacy = source.privacy;
            this.tlogId = source.tlogId;
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
            dest.writeValue(this.tlogId);
        }

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.text = in.readString();
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PostTextForm that = (PostTextForm) o;

            if (title != null ? !title.equals(that.title) : that.title != null) return false;
            if (text != null ? !text.equals(that.text) : that.text != null) return false;
            return !(tlogId != null ? !tlogId.equals(that.tlogId) : that.tlogId != null);

        }

        @Override
        public int hashCode() {
            int result = title != null ? title.hashCode() : 0;
            result = 31 * result + (text != null ? text.hashCode() : 0);
            result = 31 * result + (tlogId != null ? tlogId.hashCode() : 0);
            return result;
        }
    }
}
