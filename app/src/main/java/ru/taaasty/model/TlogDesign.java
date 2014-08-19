package ru.taaasty.model;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;

/**
 * Дизайн дневника
 */
public class TlogDesign implements Parcelable {

    public static final String HEADER_COLOR_BLACK = "black";
    public static final String HEADER_COLOR_WHITE = "white";
    public static final String HEADER_COLOR_WHITE_ON_BLACK = "white_on_black";
    public static final String HEADER_COLOR_BLACK_ON_WHITE = "black_on_white";

    public static final String COVER_ALIGN_CENTER = "center";
    public static final String COVER_ALIGN_JUSTIFY = "justify";

    public static final String FEED_BACKGROUND_COLOR_BLACK = "black";
    public static final String FEED_BACKGROUND_COLOR_WHITE = "white";

    public static final String FONT_TYPE_SANS  = "sans";
    public static final String FONT_TYPE_SERIF  = "serif";

    public static final  TlogDesign DUMMY = new TlogDesign();

    /**
     * Бэкграунд ленты, фон блога
     */
    @SerializedName("background_url")
    String mBackgroundUrl = "";

    /**
     * Заполнение бэкграунда: {@value #COVER_ALIGN_CENTER}, {@value #COVER_ALIGN_JUSTIFY}
     */
    @SerializedName("coverAlign")
    String mCoverAlign = COVER_ALIGN_CENTER;

    /**
     * Цвет ленты
     */
    @SerializedName("feedColor")
    String mFeedColor = FEED_BACKGROUND_COLOR_WHITE;

    /**
     * Увет заголовка (титул)
     * {@value #HEADER_COLOR_WHITE}, {@value #HEADER_COLOR_BLACK}, {@value #HEADER_COLOR_BLACK_ON_WHITE},
     * {@value #HEADER_COLOR_WHITE_ON_BLACK}
     */
    @SerializedName("headerColor")
    String mHeaderColor = HEADER_COLOR_BLACK;

    /**
     * Шрифт ленты {@value #FONT_TYPE_SANS}, {@value #FONT_TYPE_SERIF}
     */
    @SerializedName("fontType")
    String mFontTypeface = FONT_TYPE_SANS;

    /**
     * Прозрачность бэкраунда ленты, 0..1
     */
    @SerializedName("feedOpacity")
    float mFeedOpacity = 1;

    /**
     * Цвет текста
     * @param r
     * @return
     */
    public int getFeedTextColor(Resources r) {
        int color;
        if (isLightTheme()) {
            color = r.getColor(R.color.feed_black_text);
        } else {
            color = r.getColor(R.color.feed_white_text);
        }
        return color;
    }

    /**
     * Цвет фона, без прозрачности
     * @param r
     * @return
     */
    public int getFeedBackgroundColor(Resources r) {
        int bgColor;
        if (isLightTheme()) {
            bgColor = r.getColor(R.color.feed_light_background_color);
        } else {
            bgColor = r.getColor(R.color.feed_dark_background_color);
        }
        return bgColor;
    }

    public int getTitleForegroundColor(Resources r) {
        int color;

        switch (mHeaderColor) {
            case HEADER_COLOR_BLACK:
            case HEADER_COLOR_WHITE_ON_BLACK:
                color = r.getColor(R.color.feed_white_text);
                break;
            case HEADER_COLOR_WHITE:
            case HEADER_COLOR_BLACK_ON_WHITE:
            default:
                color = r.getColor(R.color.feed_black_text);
                break;
        }
        int alpha = 200;
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    public int getFeedAlpha() {
        int alpha = (int)Math.ceil(mFeedOpacity * 255.0);
        if (alpha > 255) alpha = 255;
        if (alpha < 0) alpha = 0;
        return alpha;
    }

    public String getBackgroundUrl() {
        if (TextUtils.isEmpty(mBackgroundUrl)) return null;
        if (!mBackgroundUrl.startsWith("http:")) {
            return BuildConfig.API_SERVER_ADDRESS + "/" + mBackgroundUrl;
        } else {
            return mBackgroundUrl;
        }
    }

    public String getCoverAlign() {
        return mCoverAlign;
    }

    public boolean isFontTypefaceSerif() {
        return FONT_TYPE_SERIF.equals(mFontTypeface);
    }

    /**
     * @return тёмный текст на светлом фоне
     */
    public boolean isLightTheme() {
        return FEED_BACKGROUND_COLOR_WHITE.equals(mFeedColor);
    }

    /**
     * @return светлый текст на темном фоне
     */
    public boolean isDarkTheme() {
        return !isLightTheme();
    }

    public void setFontTypeface(boolean serif) {
        mFontTypeface = serif ? FONT_TYPE_SERIF : FONT_TYPE_SANS;
    }

    @Override
    public String toString() {
        return "TlogDesign[backgroundUrl: " + getBackgroundUrl() + ", coverAlign: " + mCoverAlign
                + ", feed color: " + mFeedColor + ", header color: " + mHeaderColor
                + ", font typeface: " + mFontTypeface + ", feedOpacity: " + mFeedOpacity + "]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mBackgroundUrl);
        dest.writeString(this.mCoverAlign);
        dest.writeString(this.mFeedColor);
        dest.writeString(this.mHeaderColor);
        dest.writeString(this.mFontTypeface);
        dest.writeFloat(this.mFeedOpacity);
    }

    public TlogDesign() {
    }

    private TlogDesign(Parcel in) {
        this.mBackgroundUrl = in.readString();
        this.mCoverAlign = in.readString();
        this.mFeedColor = in.readString();
        this.mHeaderColor = in.readString();
        this.mFontTypeface = in.readString();
        this.mFeedOpacity = in.readFloat();
    }

    public static final Parcelable.Creator<TlogDesign> CREATOR = new Parcelable.Creator<TlogDesign>() {
        public TlogDesign createFromParcel(Parcel source) {
            return new TlogDesign(source);
        }

        public TlogDesign[] newArray(int size) {
            return new TlogDesign[size];
        }
    };
}
