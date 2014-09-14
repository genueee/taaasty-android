package ru.taaasty.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;


public class FontManager {

    public static final String FONT_SYSTEM_DEFAULT_PATH = "fonts/ProximaNova-Reg.otf";

    public static final String FONT_POST_SANS_PATH = "fonts/PTS55F_W.ttf";
    public static final String FONT_POST_SERIF_PATH = "fonts/PTF55F_W.ttf";

    private static FontManager mFontManager;

    private final Typeface mFontSystemDefault;
    private final Typeface mFontSans;
    private final Typeface mFontSerif;

    private FontManager(Context context) {
        AssetManager asm = context.getApplicationContext().getAssets();
        mFontSystemDefault = Typeface.createFromAsset(asm, FONT_SYSTEM_DEFAULT_PATH);
        mFontSans = Typeface.createFromAsset(asm, FONT_POST_SANS_PATH);
        mFontSerif = Typeface.createFromAsset(asm, FONT_POST_SERIF_PATH);
    }

    public static FontManager getInstance(Context context) {
        if (mFontManager == null) {
            mFontManager = new FontManager(context);
        }
        return mFontManager;
    }

    /**
     * @return Основной шрифт для элементов интерфейса
     */
    public Typeface getMainFont() {
        return mFontSans;
    }

    /**
     * @return Шрифт без засечек для текста постов
     */
    public Typeface getPostSansSerifTypeface() {
        return mFontSans;
    }

    /**
     * @return Шрифт с засечками для текста постов
     */
    public Typeface getPostSerifTypeface() {
        return mFontSerif;
    }
}
