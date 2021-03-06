package ru.taaasty.rest.model;

import android.os.Parcel;
import android.support.annotation.Nullable;

import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 13.03.15.
 */
public class PostEmbeddForm extends PostForm {

    @Nullable
    public CharSequence title;

    @Nullable
    public String url;

    @Nullable
    public Long tlogId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostEmbeddForm that = (PostEmbeddForm) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        return !(tlogId != null ? !tlogId.equals(that.tlogId) : that.tlogId != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (tlogId != null ? tlogId.hashCode() : 0);
        return result;
    }

    @Override
    public PostFormHtml asHtmlForm() {
        return new AsHtml(this);
    }

    public static class AsHtml implements PostFormHtml {

        public final String title;

        public final String url;

        public final String privacy;

        @Nullable
        public final Long tlogId;

        private AsHtml(PostEmbeddForm source) {
            this.title = source.title == null ? null : UiUtils.safeToHtml(source.title);
            this.url = source.url;
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
            dest.writeString(this.url);
            dest.writeString(this.privacy);
            dest.writeValue(this.tlogId);
        }

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.url = in.readString();
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
