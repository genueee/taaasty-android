package ru.taaasty.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;


public class FontManager {

    public static final String FONT_SANS_PATH = "fonts/PTS55F_W.ttf";
    public static final String FONT_SERIF_PATH = "fonts/PTF55F_W.ttf";

    private static FontManager mFontManager;

    private final Typeface mFontSans;
    private final Typeface mFontSerif;

    private FontManager(Context context) {
        AssetManager asm = context.getApplicationContext().getAssets();
        mFontSans = Typeface.createFromAsset(asm, FONT_SANS_PATH);
        mFontSerif = Typeface.createFromAsset(asm, FONT_SERIF_PATH);
    }

    public static FontManager getInstance(Context context) {
        if (mFontManager == null) {
            mFontManager = new FontManager(context);
        }
        return mFontManager;
    }

    public Typeface getMainFont() {
        return mFontSans;
    }

    public Typeface getDefaultSansSerifTypeface() {
        return mFontSans;
    }

    public Typeface getDefaultSerifTypeface() {
        return mFontSerif;
    }
}
