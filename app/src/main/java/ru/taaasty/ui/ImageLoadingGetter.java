package ru.taaasty.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.utils.ImageUtils;

public class ImageLoadingGetter implements Html.ImageGetter {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ImageLoadingImageGetter";

    private final Drawable mPlaceholderDrawable;

    private final int mDstWidth;

    /**
     * {@linkto Html.ImageGetter} - placeholder для картинок, которые тегами img в тексте.
     * @param width Ширина placeholder'а. Может быть 0
     * @param context
     */
    public ImageLoadingGetter(int width, Context context) {
        if (width <= 0) {
            mDstWidth = context.getResources().getDimensionPixelSize(R.dimen.image_loading_placeholder_default_size);
        } else {
            mDstWidth = width;
        }

        int height = Math.round(mDstWidth / Constants.DEFAULT_IMAGE_ASPECT_RATIO);
        Drawable drawable = context.getResources().getDrawable(R.drawable.image_loading_drawable);
        mPlaceholderDrawable = ImageUtils.changeDrawableIntristicSizeAndBounds(drawable, mDstWidth, height);
    }

    public int getWidth() {
        return mDstWidth;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (DBG) Log.v(TAG, "getDrawable() " + source);
        return mPlaceholderDrawable;
    }
}
