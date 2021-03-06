package ru.taaasty.rest.model;

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

    public static final String KIND_USER = "user";

    public static final String KIND_ANONYMOUS = "anonymous";

    /**
     * URL картинки. Может быть null, если пользователь её себе не установил,
     * или если картинка на сервере внезапно пропала (да, такое реально бывает)
     */
    @Nullable
    public String originalUrl;

    public DefaultColors defaultColors = DefaultColors.DUMMY;

    public String symbol;

    public String kind;

    public static class DefaultColors implements Parcelable {

        public static final DefaultColors DUMMY = new DefaultColors();

        String background = "#44d068";
        String name = "#ffffffff";

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

    public Userpic() {
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.originalUrl);
        dest.writeParcelable(this.defaultColors, flags);
        dest.writeString(this.symbol);
        dest.writeString(this.kind);
    }

    protected Userpic(Parcel in) {
        this.originalUrl = in.readString();
        this.defaultColors = in.readParcelable(DefaultColors.class.getClassLoader());
        this.symbol = in.readString();
        this.kind = in.readString();
    }

    public static final Creator<Userpic> CREATOR = new Creator<Userpic>() {
        @Override
        public Userpic createFromParcel(Parcel source) {
            return new Userpic(source);
        }

        @Override
        public Userpic[] newArray(int size) {
            return new Userpic[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Userpic userpic = (Userpic) o;

        if (originalUrl != null ? !originalUrl.equals(userpic.originalUrl) : userpic.originalUrl != null)
            return false;
        if (defaultColors != null ? !defaultColors.equals(userpic.defaultColors) : userpic.defaultColors != null)
            return false;
        if (symbol != null ? !symbol.equals(userpic.symbol) : userpic.symbol != null) return false;
        return kind != null ? kind.equals(userpic.kind) : userpic.kind == null;

    }

    @Override
    public int hashCode() {
        int result = originalUrl != null ? originalUrl.hashCode() : 0;
        result = 31 * result + (defaultColors != null ? defaultColors.hashCode() : 0);
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        return result;
    }
}
