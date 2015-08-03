package ru.taaasty.rest.model;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.utils.ImageUtils;

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
     * Цвет ленты.
     * Устаревший, сломанный атрибут, не пользуемся. Сначла это был цвет самой ленты, затем, в
     * какой-то момент он стал цветом текста и хрен его знает, что с ним в будущем будет.
     */
    //@Deprecated
    //@SerializedName("feedColor")
    //String mFeedColor = FEED_BACKGROUND_COLOR_WHITE;

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
     * Цвет текста ленты
     */
    @SerializedName("feedFontColor")
    private String mFontColor = "#ffffff";

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

    /**
     * @return Drawable, устанавливаемый фоном: цвет ленты, цвет границ
     */
    @DrawableRes
    public int getFeedBackgroundDrawable() {
        if (isLightTheme()) {
            return R.drawable.feed_white_background;
        } else {
            return R.drawable.feed_black_background;
        }
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

    public int getFeedActionsTextColor(Resources r) {
        return r.getColor(isDarkTheme() ? R.color.text_color_feed_actions_dark_theme : R.color.text_color_feed_actions_light_theme);
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
        return !isDarkTheme();
    }

    /**
     * @return светлый текст на темном фоне
     */
    public boolean isDarkTheme() {
        return ImageUtils.isLightColor(mFontColor);
    }

    public static TlogDesign createLightTheme(TlogDesign src) {
        if (src.isLightTheme()) return src;
        TlogDesign res = new TlogDesign(src);
        res.mFontColor = "#000000";
        return res;
    }

    public void setFontTypeface(boolean serif) {
        mFontTypeface = serif ? FONT_TYPE_SERIF : FONT_TYPE_SANS;
    }

    @Override
    public String toString() {
        return "TlogDesign[backgroundUrl: " + getBackgroundUrl() + ", coverAlign: " + mCoverAlign
                + ", font color: " + mFontColor + ", header color: " + mHeaderColor
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
        dest.writeString(this.mFontColor);
        dest.writeString(this.mHeaderColor);
        dest.writeString(this.mFontTypeface);
        dest.writeFloat(this.mFeedOpacity);
    }

    public TlogDesign() {
    }

    public TlogDesign(TlogDesign source) {
        this.mBackgroundUrl = source.mBackgroundUrl;
        this.mCoverAlign = source.mCoverAlign;
        this.mFontColor = source.mFontColor;
        this.mHeaderColor = source.mHeaderColor;
        this.mFontTypeface = source.mFontTypeface;
        this.mFeedOpacity = source.mFeedOpacity;
    }

    private TlogDesign(Parcel in) {
        this.mBackgroundUrl = in.readString();
        this.mCoverAlign = in.readString();
        this.mFontColor = in.readString();
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

    public int getAuthorTextAppearance() {
        int appearance;
        if (isLightTheme()) {
            appearance = R.style.TextAppearanceSlugInlineBlack;
        } else {
            appearance = R.style.TextAppearanceSlugInlineWhite;
        }
        return appearance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TlogDesign that = (TlogDesign) o;

        if (Float.compare(that.mFeedOpacity, mFeedOpacity) != 0) return false;
        if (mBackgroundUrl != null ? !mBackgroundUrl.equals(that.mBackgroundUrl) : that.mBackgroundUrl != null)
            return false;
        if (mCoverAlign != null ? !mCoverAlign.equals(that.mCoverAlign) : that.mCoverAlign != null)
            return false;
        if (mFontColor != null ? !mFontColor.equals(that.mFontColor) : that.mFontColor != null)
            return false;
        if (mHeaderColor != null ? !mHeaderColor.equals(that.mHeaderColor) : that.mHeaderColor != null)
            return false;
        return !(mFontTypeface != null ? !mFontTypeface.equals(that.mFontTypeface) : that.mFontTypeface != null);

    }

    @Override
    public int hashCode() {
        int result = mBackgroundUrl != null ? mBackgroundUrl.hashCode() : 0;
        result = 31 * result + (mCoverAlign != null ? mCoverAlign.hashCode() : 0);
        result = 31 * result + (mFontColor != null ? mFontColor.hashCode() : 0);
        result = 31 * result + (mHeaderColor != null ? mHeaderColor.hashCode() : 0);
        result = 31 * result + (mFontTypeface != null ? mFontTypeface.hashCode() : 0);
        result = 31 * result + (mFeedOpacity != +0.0f ? Float.floatToIntBits(mFeedOpacity) : 0);
        return result;
    }
}
