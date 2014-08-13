package ru.taaasty.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;


public class FontManager {

    private static FontManager mFontManager;

    private final Typeface mFontSans;
    private final Typeface mFontSerif;

    private FontManager(Context context) {
        AssetManager asm = context.getApplicationContext().getAssets();
        mFontSans = Typeface.createFromAsset(asm, "fonts/PTS55F_W.ttf");
        mFontSerif = Typeface.createFromAsset(asm, "fonts/PTF55F_W.ttf");
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
