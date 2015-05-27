package ru.taaasty.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.widgets.BackgroundBitmapDrawable;

/**
* Created by alexey on 18.07.14.
*/
public class TargetSetHeaderBackground implements Target {
    private static final String TAG = "TargSetHeaderBckgnd";
    private static final boolean DBG = BuildConfig.DEBUG;
    private final View mTarget;
    private final TlogDesign mDesign;
    private final int mForegroundColor;
    private final int mBlurRadius;
    private boolean mForceDisableAnimation;

    public TargetSetHeaderBackground(View target, TlogDesign design, int foregroundColor) {
        this(target, design, foregroundColor, 0);
    }

    public TargetSetHeaderBackground(View target, TlogDesign design, int foregroundColorRes, int blurRadius) {
        if (target == null) throw new NullPointerException();
        mTarget = target;
        mDesign = design;
        mBlurRadius = blurRadius;
        mForegroundColor = target.getResources().getColor(foregroundColorRes);
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        setBackground(bitmap, !mForceDisableAnimation && (from == Picasso.LoadedFrom.NETWORK));
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        if (DBG) Log.v(TAG, "onBitmapFailed");
        // XXX
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }

    public void setForceDisableAnimate(boolean disable) {
        mForceDisableAnimation = disable;
    }

    public void setBackground(Bitmap bitmap, boolean animate) {
        BackgroundBitmapDrawable background;
        Drawable backgroundDrawable;

        background = new BackgroundBitmapDrawable(mTarget.getResources(), bitmap);
        background.setBlurRadius(mBlurRadius);
        // Игнорируем настройки дизайна. Всегда ставим бэграундом COVER_ALIGN_CENTER_CROP,
        // чтобы соотношение сторон изображения не изменялось.
        background.setCoverAlign(BackgroundBitmapDrawable.COVER_ALIGN_CENTER_CROP);
        if (DBG) Log.v(TAG, "setBackgroundDrawable design: " + mDesign +
                " foregroundColor: 0x" + Integer.toHexString(mForegroundColor) + "blur radius: " + mBlurRadius);

        if (mForegroundColor != Color.TRANSPARENT) {
            ColorDrawable foreground = new ColorDrawable(mForegroundColor);
            backgroundDrawable = new LayerDrawable(new Drawable[] {background, foreground});
        } else {
            backgroundDrawable = background;
        }

        if (!animate) {
            mTarget.setBackgroundDrawable(backgroundDrawable);
        } else {
            Drawable oldBackground = mTarget.getBackground();
            if (oldBackground == null) {
                mTarget.setBackgroundDrawable(backgroundDrawable);
            } else {
                if (oldBackground instanceof AnimationDrawable) {
                    ((AnimationDrawable) oldBackground).stop();
                }
                TransitionDrawable td = new TransitionDrawable(new Drawable[]{oldBackground, backgroundDrawable});
                mTarget.setBackgroundDrawable(td);
                td.startTransition(Constants.IMAGE_FADE_IN_DURATION);
            }
        }
    }
}
