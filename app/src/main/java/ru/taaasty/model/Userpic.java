package ru.taaasty.model;

import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
* Created by alexey on 10.07.14.
*/
public class Userpic implements Parcelable {
    public static Userpic DUMMY = new Userpic();

    @Nullable
    public String largeUrl;

    @Nullable
    public String thumb64Url;

    @Nullable
    public String originalUrl;

    @Nullable
    public String thumb128Url;

    @Nullable
    public String thumborPath;

    public DefaultColors defaultColors = DefaultColors.DUMMY;

    public static class DefaultColors implements Parcelable {

        public static final DefaultColors DUMMY = new DefaultColors();

        String background = "";
        String name = "";

        public int getBackgroundColor() {
            if (TextUtils.isEmpty(background)) return Color.TRANSPARENT;
            try {
                return Color.parseColor(background);
            } catch (IllegalArgumentException iae) {
                Log.e("DefaultColors", "Color.parseColor() error. Color: " + background, iae);
                return Color.TRANSPARENT;
            }
        }

        public int getTextColor() {
            if (TextUtils.isEmpty(name)) return Color.WHITE; // Задолбало исключение в логах
            try {
                return Color.parseColor(name);
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                Log.e("DefaultColors", "Color.parseColor() error. Color: " + name, e);
                return Color.WHITE;
            }
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.background);
            dest.writeString(this.name);
        }

        public DefaultColors() {
        }

        private DefaultColors(Parcel in) {
            this.background = in.readString();
            this.name = in.readString();
        }

        public static final Parcelable.Creator<DefaultColors> CREATOR = new Parcelable.Creator<DefaultColors>() {
            public DefaultColors createFromParcel(Parcel source) {
                return new DefaultColors(source);
            }

            public DefaultColors[] newArray(int size) {
                return new DefaultColors[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DefaultColors that = (DefaultColors) o;

            if (background != null ? !background.equals(that.background) : that.background != null)
                return false;
            return !(name != null ? !name.equals(that.name) : that.name != null);

        }

        @Override
        public int hashCode() {
            int result = background != null ? background.hashCode() : 0;
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.originalUrl);
        dest.writeString(this.largeUrl);
        dest.writeString(this.thumb64Url);
        dest.writeString(this.thumb128Url);
        dest.writeString(this.thumborPath);
        dest.writeParcelable(this.defaultColors, 0);
    }

    public Userpic() {
    }

    private Userpic(Parcel in) {
        this.originalUrl = in.readString();
        this.largeUrl = in.readString();
        this.thumb64Url = in.readString();
        this.thumb128Url = in.readString();
        this.thumborPath = in.readString();
        this.defaultColors = in.readParcelable(DefaultColors.class.getClassLoader());
    }

    public static final Parcelable.Creator<Userpic> CREATOR = new Parcelable.Creator<Userpic>() {
        public Userpic createFromParcel(Parcel source) {
            return new Userpic(source);
        }

        public Userpic[] newArray(int size) {
            return new Userpic[size];
        }
    };

    public String getOptimalUrlForSize(int width, int height) {
        String url;
        int diameter = Math.max(width, height);
        if (diameter <= 64 * 1.5 && !TextUtils.isEmpty(thumb64Url)) {
            url = thumb64Url;
        } else if (diameter <= 128 * 1.5 && !TextUtils.isEmpty(thumb128Url)) {
            url = thumb128Url;
        } else {
            url = largeUrl;
        }
        if (url == null) url = "";
        return url;
    }

    @Override
    public String toString() {
        return "Userpic{" +
                "originalUrl='" + originalUrl + '\'' +
                "largeUrl='" + largeUrl + '\'' +
                ", thumb64Url='" + thumb64Url + '\'' +
                ", thumb128Url='" + thumb128Url + '\'' +
                ", thumborPath='" + thumborPath + '\'' +
                ", defaultColors=" + defaultColors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Userpic userpic = (Userpic) o;

        if (largeUrl != null ? !largeUrl.equals(userpic.largeUrl) : userpic.largeUrl != null)
            return false;
        if (thumb64Url != null ? !thumb64Url.equals(userpic.thumb64Url) : userpic.thumb64Url != null)
            return false;
        if (originalUrl != null ? !originalUrl.equals(userpic.originalUrl) : userpic.originalUrl != null)
            return false;
        if (thumb128Url != null ? !thumb128Url.equals(userpic.thumb128Url) : userpic.thumb128Url != null)
            return false;
        if (thumborPath != null ? !thumborPath.equals(userpic.thumborPath) : userpic.thumborPath != null)
            return false;
        return !(defaultColors != null ? !defaultColors.equals(userpic.defaultColors) : userpic.defaultColors != null);

    }

    @Override
    public int hashCode() {
        int result = largeUrl != null ? largeUrl.hashCode() : 0;
        result = 31 * result + (thumb64Url != null ? thumb64Url.hashCode() : 0);
        result = 31 * result + (originalUrl != null ? originalUrl.hashCode() : 0);
        result = 31 * result + (thumb128Url != null ? thumb128Url.hashCode() : 0);
        result = 31 * result + (thumborPath != null ? thumborPath.hashCode() : 0);
        result = 31 * result + (defaultColors != null ? defaultColors.hashCode() : 0);
        return result;
    }
}
