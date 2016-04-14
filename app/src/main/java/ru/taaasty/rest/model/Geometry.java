package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

import ru.taaasty.utils.Size;


public class Geometry implements Parcelable {
    public int width;
    public int height;


    @Override
    public int describeContents() {
        return 0;
    }

    public Size toImageSize() {
        return new Size(width, height);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.width);
        dest.writeInt(this.height);
    }

    public Geometry() {
    }

    private Geometry(Parcel in) {
        this.width = in.readInt();
        this.height = in.readInt();
    }

    public static final Creator<Geometry> CREATOR = new Creator<Geometry>() {
        public Geometry createFromParcel(Parcel source) {
            return new Geometry(source);
        }

        public Geometry[] newArray(int size) {
            return new Geometry[size];
        }
    };

    @Override
    public String toString() {
        return "Geometry{" +
                "height=" + height +
                ", width=" + width +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Geometry that = (Geometry) o;

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
