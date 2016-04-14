package ru.taaasty.rest.model.conversations;


import android.os.Parcel;
import android.os.Parcelable;

import ru.taaasty.rest.model.Geometry;

public class Attachment implements Parcelable {

    public long id;

    public String url;

    public String contentType;

    /**
     *  Размер файла
     */
    public long size;

    public Geometry geometry;

    public Attachment() {
    }


    /**
     * @return content-type - поддерживаемый тип картинок (растровые, вектор не поддерживается)
     */
    public boolean isImage() {
        return "image/bmp".equals(contentType)
                || "image/gif".equals(contentType)
                || "image/jpeg".equals(contentType)
                || "image/pjpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/x-ms-bmp".equals(contentType)
                || "image/webp".equals(contentType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attachment that = (Attachment) o;

        if (id != that.id) return false;
        if (size != that.size) return false;
        if (!url.equals(that.url)) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null)
            return false;
        return geometry != null ? geometry.equals(that.geometry) : that.geometry == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + url.hashCode();
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (geometry != null ? geometry.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.url);
        dest.writeString(this.contentType);
        dest.writeLong(this.size);
        dest.writeParcelable(this.geometry, flags);
    }

    protected Attachment(Parcel in) {
        this.id = in.readLong();
        this.url = in.readString();
        this.contentType = in.readString();
        this.size = in.readLong();
        this.geometry = in.readParcelable(Geometry.class.getClassLoader());
    }

    public static final Parcelable.Creator<Attachment> CREATOR = new Parcelable.Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel source) {
            return new Attachment(source);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };
}
