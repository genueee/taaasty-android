package ru.taaasty.model.iframely;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

/**
* <a href="https://iframely.com/docs/links">https://iframely.com/docs/links</a>
*/
public class Media implements Parcelable {

    public static final Media DUMMY = new Media();

    @SerializedName("aspect-ratio")
    public float aspect_ratio;

    public int width;

    public int height;

    @SerializedName("max-width")
    public int max_width;

    @SerializedName("min-width")
    public int min_width;

    @SerializedName("max-height")
    public int max_height;

    @SerializedName("min-height")
    public int min_height;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(this.aspect_ratio);
        dest.writeInt(width);
        dest.writeInt(height);
        dest.writeInt(max_width);
        dest.writeInt(max_height);
        dest.writeInt(min_width);
        dest.writeInt(min_height);
    }

    public Media() {
    }

    Media(Parcel in) {
        this.aspect_ratio = in.readFloat();
        this.width = in.readInt();
        this.height = in.readInt();
        this.max_width = in.readInt();
        this.max_height = in.readInt();
        this.min_width = in.readInt();
        this.min_height = in.readInt();
    }

    public static final Creator<Media> CREATOR = new Creator<Media>() {
        public Media createFromParcel(Parcel source) {
            return new Media(source);
        }

        public Media[] newArray(int size) {
            return new Media[size];
        }
    };

    @Override
    public String toString() {
        return "Media{" +
                "aspect_ratio=" + aspect_ratio +
                ", width=" + width +
                ", height=" + height +
                ", max_width=" + max_width +
                ", min_width=" + min_width +
                ", max_height=" + max_height +
                ", min_height=" + min_height +
                '}';
    }
}
