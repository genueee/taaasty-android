package ru.taaasty.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Typeface;


public class FontManager {

    public static final String FONT_SYSTEM_DEFAULT_PATH = "fonts/ProximaNova-Reg.otf";

    public static final String FONT_POST_SANS_PATH = "fonts/PTS55F_W.ttf";
    public static final String FONT_POST_SERIF_PATH = "fonts/PTF55F_W.ttf";

    public static final String FONT_SYSTEM_BOLD_PATH = "fonts/ProximaNova-Bold.otf";

    private static FontManager sFontManager;
    private final AssetManager mAssetsManager;

    private final Typeface mFontSystemDefault;
    private final Typeface mFontSans;
    private final Typeface mFontSerif;
    private Typeface mFontSystemBold;

    private FontManager(Context context) {
        mAssetsManager = context.getApplicationContext().getAssets();
        mFontSystemDefault = Typeface.createFromAsset(mAssetsManager, FONT_SYSTEM_DEFAULT_PATH);
        mFontSans = Typeface.createFromAsset(mAssetsManager, FONT_POST_SANS_PATH);
        mFontSerif = Typeface.createFromAsset(mAssetsManager, FONT_POST_SERIF_PATH);
    }

    public static void onAppInit(Context appContext) {
        sFontManager = new FontManager(appContext);
    }

    public static FontManager getInstance() {
        return sFontManager;
    }

    /**
     * @return Основной шрифт для элементов интерфейса
     */
    public Typeface getMainFont() {
        return mFontSystemDefault;
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

    public synchronized Typeface getFontSystemBold() {
        if (mFontSystemBold == null) {
            mFontSystemBold = Typeface.createFromAsset(mAssetsManager, FONT_SYSTEM_BOLD_PATH);
        }
        return mFontSystemBold;
    }
}
