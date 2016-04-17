package ru.taaasty.rest.model2;

import android.graphics.Color;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.auto.value.AutoValue;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;

/**
* Created by alexey on 10.07.14.
*/
@AutoValue
public abstract class Userpic implements Parcelable {

    public static Userpic create() {
        return create("", DefaultColors.DUMMY);
    }

    public static Userpic create(String originalUrl, DefaultColors defaultColors) {
        return new AutoValue_Userpic(originalUrl, defaultColors);
    }

    public static TypeAdapter<Userpic> typeAdapter(Gson gson) {
        return new AutoValue_Userpic.GsonTypeAdapter(gson);
    }


    /**
     * URL картинки. Может быть null, если пользователь её себе не установил,
     * или если картинка на сервере внезапно пропала (да, такое реально бывает)
     */
    @Nullable
    @SerializedName("original_url")
    public abstract String originalUrl();

    @SerializedName("default_colors")
    public abstract DefaultColors defaultColors();

    @AutoValue
    public static abstract class DefaultColors implements Parcelable {

        public static final DefaultColors DUMMY = create("#44d068", "#ffffffff");

        public static TypeAdapter<DefaultColors> typeAdapter(Gson gson) {
            return new AutoValue_Userpic_DefaultColors.GsonTypeAdapter(gson);
        }

        public static DefaultColors create(String background, String name) {
            return new AutoValue_Userpic_DefaultColors(background, name);
        }

        public static DefaultColors create() {
            return create("#44d068", "#ffffffff");
        }

        @Nullable
        @SerializedName("background")
        abstract String background();

        @Nullable
        @SerializedName("name")
        abstract String name();

        public int getBackgroundColor() {
            if (TextUtils.isEmpty(background())) return Color.TRANSPARENT;
            try {
                return Color.parseColor(background());
            } catch (IllegalArgumentException iae) {
                Log.e("DefaultColors", "Color.parseColor() error. Color: " + background(), iae);
                return Color.TRANSPARENT;
            }
        }

        public int getTextColor() {
            if (TextUtils.isEmpty(name())) return Color.WHITE; // Задолбало исключение в логах
            try {
                return Color.parseColor(name());
            } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
                Log.e("DefaultColors", "Color.parseColor() error. Color: " + name(), e);
                return Color.WHITE;
            }
        }
    }
}
