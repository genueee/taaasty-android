package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

import ru.taaasty.utils.ImageSize;

/**
* Created by alexey on 12.08.14.
*/
public class ImageInfo implements Parcelable {
    public long id;

    public Date createAt;

    String title;

    String source;

    public Image2 image = Image2.DUMMY;


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

    public static class Image2 implements Parcelable {

        public static final Image2 DUMMY = new Image2();

        public String url = "";

        /**
         * Thumbor path
         */
        public String path = "";

        public ImageGeometry geometry = new ImageGeometry();


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeString(this.path);
            dest.writeParcelable(this.geometry, flags);
        }

        public Image2() {
        }

        private Image2(Parcel in) {
            this.url = in.readString();
            this.path = in.readString();
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
    }

}
