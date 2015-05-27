package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Date;

import ru.taaasty.utils.ImageSize;

/**
* Created by alexey on 12.08.14.
*/
public class ImageInfo implements Parcelable {
    public long id;

    public Date createAt;

    public static final String CONTENT_TYPE_GIF = "image/gif";

    String title;

    String source;

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
        dest.writeLong(createAt != null ? createAt.getTime() : -1);
        dest.writeString(this.title);
        dest.writeString(this.source);
        dest.writeString(this.contentType);
        dest.writeInt(framesCount);
        dest.writeParcelable(this.image, flags);
    }

    public ImageInfo() {
    }

    ImageInfo(Parcel in) {
        this.id = in.readLong();
        long tmpCreateAt = in.readLong();
        this.createAt = tmpCreateAt == -1 ? null : new Date(tmpCreateAt);
        this.title = in.readString();
        this.source = in.readString();
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
                ", createAt=" + createAt +
                ", title='" + title + '\'' +
                ", source='" + source + '\'' +
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
        if (createAt != null ? !createAt.equals(imageInfo.createAt) : imageInfo.createAt != null)
            return false;
        if (title != null ? !title.equals(imageInfo.title) : imageInfo.title != null) return false;
        if (source != null ? !source.equals(imageInfo.source) : imageInfo.source != null)
            return false;
        if (image != null ? !image.equals(imageInfo.image) : imageInfo.image != null) return false;
        return !(contentType != null ? !contentType.equals(imageInfo.contentType) : imageInfo.contentType != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (createAt != null ? createAt.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        result = 31 * result + (image != null ? image.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + framesCount;
        return result;
    }

    public static class Image2 implements Parcelable {

        public static final Image2 DUMMY = new Image2();

        public String url = "";

        /**
         * Thumbor path
         */
        public String path = "";

        public ImageGeometry geometry = new ImageGeometry();

        @Nullable
        public String title;

        public String source;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeString(this.path);
            dest.writeString(this.title);
            dest.writeString(this.source);
            dest.writeParcelable(this.geometry, flags);
        }

        public Image2() {
        }

        private Image2(Parcel in) {
            this.url = in.readString();
            this.path = in.readString();
            this.title = in.readString();
            this.source = in.readString();
            this.geometry = in.readParcelable(ImageGeometry.class.getClassLoader());
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
                    ", path='" + path + '\'' +
                    ", title='" + title + '\'' +
                    ", source='" + source + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Image2 image2 = (Image2) o;

            if (url != null ? !url.equals(image2.url) : image2.url != null) return false;
            if (path != null ? !path.equals(image2.path) : image2.path != null) return false;
            if (geometry != null ? !geometry.equals(image2.geometry) : image2.geometry != null)
                return false;
            if (title != null ? !title.equals(image2.title) : image2.title != null) return false;
            return !(source != null ? !source.equals(image2.source) : image2.source != null);
        }

        @Override
        public int hashCode() {
            int result = url != null ? url.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            result = 31 * result + (geometry != null ? geometry.hashCode() : 0);
            result = 31 * result + (title != null ? title.hashCode() : 0);
            result = 31 * result + (source != null ? source.hashCode() : 0);
            return result;
        }
    }

    public static class ImageGeometry implements Parcelable {
        public int width;
        public int height;


        @Override
        public int describeContents() {
            return 0;
        }

        public ImageSize toImageSize() {
            return new ImageSize(width, height);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(this.width);
            dest.writeInt(this.height);
        }

        public ImageGeometry() {
        }

        private ImageGeometry(Parcel in) {
            this.width = in.readInt();
            this.height = in.readInt();
        }

        public static final Parcelable.Creator<ImageGeometry> CREATOR = new Parcelable.Creator<ImageGeometry>() {
            public ImageGeometry createFromParcel(Parcel source) {
                return new ImageGeometry(source);
            }

            public ImageGeometry[] newArray(int size) {
                return new ImageGeometry[size];
            }
        };

        @Override
        public String toString() {
            return "ImageGeometry{" +
                    "height=" + height +
                    ", width=" + width +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ImageGeometry that = (ImageGeometry) o;

            if (width != that.width) return false;
            return height == that.height;

        }

        @Override
        public int hashCode() {
            int result = width;
            result = 31 * result + height;
            return result;
        }
    }

}
