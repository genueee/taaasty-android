package ru.taaasty.rest.model;

import android.net.Uri;
import android.os.Parcel;
import android.support.annotation.Nullable;

import ru.taaasty.utils.UiUtils;

public class PostFlowForm extends PostForm {

    public CharSequence title;

    @Nullable
    public CharSequence description;

    @Nullable
    public Uri imageUri;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostFlowForm that = (PostFlowForm) o;

        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        return !(imageUri != null ? !imageUri.equals(that.imageUri) : that.imageUri != null);

    }

    @Override
    public int hashCode() {
        int result = title != null ? title.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (imageUri != null ? imageUri.hashCode() : 0);
        return result;
    }

    @Override
    public PostFormHtml asHtmlForm() {
        return new AsHtml(this);
    }

    public static class AsHtml implements PostFormHtml {

        public final String title;

        public final String description;

        public final Uri imageUri;

        private AsHtml(PostFlowForm source) {
            this.title = source.title == null ? null : source.title.toString();
            this.description = source.description == null ? null : source.description.toString();
            this.imageUri = source.imageUri;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.title);
            dest.writeString(this.description);
            dest.writeParcelable(this.imageUri, 0);
        }

        private AsHtml(Parcel in) {
            this.title = in.readString();
            this.description = in.readString();
            this.imageUri = in.readParcelable(Uri.class.getClassLoader());
        }

        @Override
        public boolean isPrivatePost() {
            return false;
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
