package ru.taaasty.model;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.annotations.SerializedName;

/**
* Created by alexey on 10.07.14.
*/
public class Userpic {
    public static Userpic DUMMY = new Userpic();

    @Nullable
    public String largeUrl;

    @Nullable
    public String thumb64Url;

    public DefaultColors defaultColors = DefaultColors.DUMMY;

    public static class DefaultColors {

        public static final DefaultColors DUMMY = new DefaultColors();

        public String background;
        public String name;

        public int backgroundColor() {
            if (TextUtils.isEmpty(background)) return Color.TRANSPARENT;
            try {
                return Color.parseColor(background);
            } catch (IllegalArgumentException iae) {
                Log.e("DefaultColors", "Color.parseColor() error. Color: " + background, iae);
                return Color.TRANSPARENT;
            }
        }
    }

}
