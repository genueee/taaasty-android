package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
* Created by alexey on 12.08.14.
*/
public class ImageInfo implements Parcelable {
    public long id;

    public static final String CONTENT_TYPE_GIF = "image/gif";

    //Date createdAt;

    //String title;

    //String source;

    public Image2 image = Image2.DUMMY;

    public String contentType;

    public int framesCount = 1;

    public boolean isAnimatedGif() {
        return  framesCount > 1 && (CONTENT_TYPE_GIF.equals(contentType));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.contentType);
        dest.writeInt(framesCount);
        dest.writeParcelable(this.image, flags);
    }

    public ImageInfo() {
    }

    ImageInfo(Parcel in) {
        this.id = in.readLong();
        this.contentType = in.readString();
        this.framesCount = in.readInt();
        this.image = in.readParcelable(Image2.class.getClassLoader());
    }

    public static final Creator<ImageInfo> CREATOR = new Creator<ImageInfo>() {
        public ImageInfo createFromParcel(Parcel source) {
            return new ImageInfo(source);
        }

        public ImageInfo[] newArray(int size) {
            return new ImageInfo[size];
        }
    };

    @Override
    public String toString() {
        return "ImageInfo{" +
                "contentType='" + contentType + '\'' +
                ", id=" + id +
                ", image=" + image +
                ", framesCount=" + framesCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImageInfo imageInfo = (ImageInfo) o;

        if (id != imageInfo.id) return false;
        if (framesCount != imageInfo.framesCount) return false;
        if (image != null ? !image.equals(imageInfo.image) : imageInfo.image != null) return false;
        return !(contentType != null ? !contentType.equals(imageInfo.contentType) : imageInfo.contentType != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + framesCount;
        return result;
    }

    public static class Image2 implements Parcelable {

        public static final Image2 DUMMY = new Image2();

        public String url = "";

        public Geometry geometry = new Geometry();

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeParcelable(this.geometry, flags);
        }

        public Image2() {
        }

        private Image2(Parcel in) {
            this.url = in.readString();
            this.geometry = in.readParcelable(Geometry.class.getClassLoader());
        }

        public static final Parcelable.Creator<Image2> CREATOR = new Parcelable.Creator<Image2>() {
            public Image2 createFromParcel(Parcel source) {
                return new Image2(source);
            }

            public Image2[] newArray(int size) {
                return new Image2[size];
            }
        };

        @Override
        public String toString() {
            return "Image2{" +
                    "geometry=" + geometry +
                    ", url='" + url + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Image2 image2 = (Image2) o;

            if (url != null ? !url.equals(image2.url) : image2.url != null) return false;
            return !(geometry != null ? !geometry.equals(image2.geometry) : image2.geometry != null);

        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + (geometry != null ? geometry.hashCode() : 0);
            return result;
        }
    }

}
