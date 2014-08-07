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
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.largeUrl);
        dest.writeString(this.thumb64Url);
        dest.writeParcelable(this.defaultColors, 0);
    }

    public Userpic() {
    }

    private Userpic(Parcel in) {
        this.largeUrl = in.readString();
        this.thumb64Url = in.readString();
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

    @Override
    public String toString() {
        return "Userpic{" +
                "largeUrl='" + largeUrl + '\'' +
                ", thumb64Url='" + thumb64Url + '\'' +
                ", defaultColors=" + defaultColors +
                '}';
    }
}
