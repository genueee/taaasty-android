package ru.taaasty.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;

public class ImageLoadingGetter implements Html.ImageGetter {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ImageLoadingImageGetter";

    private static final float DEFAULT_ASPECT_RATIO = 4f/3f;

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

        mPlaceholderDrawable = context.getResources().getDrawable(R.drawable.image_loading_drawable);

        int height = Math.round(mDstWidth / DEFAULT_ASPECT_RATIO);
        mPlaceholderDrawable.setBounds(0, 0, mDstWidth, height);
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
