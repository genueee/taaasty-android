package ru.taaasty.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;


public class FontManager {

    private static FontManager mFontManager;

    private final Typeface mDefaultFont;
    private final Typeface mDefaultFontBold;
    private final Typeface mDefaultFontLight;

    private FontManager(Context context) {
        AssetManager asm = context.getApplicationContext().getAssets();
        mDefaultFont = Typeface.createFromAsset(asm, "fonts/ProximaNova-Reg.otf");
        mDefaultFontBold = Typeface.createFromAsset(asm, "fonts/ProximaNova-Bold.otf");
        mDefaultFontLight = Typeface.createFromAsset(asm, "fonts/ProximaNova-Light.otf");
    }

    public static FontManager getInstance(Context context) {
        if (mFontManager == null) {
            mFontManager = new FontManager(context);
        }
        return mFontManager;
    }

    public Typeface getMainFont() {
        return mDefaultFont;
    }

    public Typeface getMainFontBold() {
        return mDefaultFontBold;
    }

    public Typeface getDefaultFontLight() {
        return mDefaultFontLight;
    }

    public Typeface getDefaultSansSerifTypeface() {
        return mDefaultFont;
    }

    public Typeface getDefaultSerifTypeface() {
        return Typeface.SERIF;
    }
}
