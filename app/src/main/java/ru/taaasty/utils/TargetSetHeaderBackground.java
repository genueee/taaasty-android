package ru.taaasty.utils;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.BuildConfig;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.widgets.BackgroundBitmapDrawable;

/**
* Created by alexey on 18.07.14.
*/
public class TargetSetHeaderBackground implements Target {
    private static final String TAG = "TargetSetHeaderBackground";
    private static final boolean DBG = BuildConfig.DEBUG;
    private final View mTarget;
    private final TlogDesign mDesign;
    private final int mForegroundColor;
    private final int mBlurRadius;

    public TargetSetHeaderBackground(View target, TlogDesign design, int foregroundColor) {
        this(target, design, foregroundColor, 0);
    }

    public TargetSetHeaderBackground(View target, TlogDesign design, int foregroundColor, int blurRadius) {
        if (target == null) throw new NullPointerException();
        mTarget = target;
        mDesign = design;
        mBlurRadius = blurRadius;
        mForegroundColor = foregroundColor;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        BackgroundBitmapDrawable background = new BackgroundBitmapDrawable(mTarget.getResources(), bitmap);
        background.setBlurRadius(mBlurRadius);
        ColorDrawable foreground = new ColorDrawable(mForegroundColor);
        if (TlogDesign.COVER_ALIGN_CENTER.equals(mDesign.getCoverAlign())) {
            background.setCoverAlign(BackgroundBitmapDrawable.COVER_ALIGN_CENTER_CROP);
        } else {
            background.setCoverAlign(BackgroundBitmapDrawable.COVER_ALIGN_STRETCH);
        }
        if (DBG) Log.v(TAG, "setBackgroundDrawable design: " + mDesign +
                " foregroundColor: 0x" + Integer.toHexString(mForegroundColor) + "blur radius: " + mBlurRadius);
        mTarget.setBackgroundDrawable(
                new LayerDrawable(new Drawable[] {background, foreground}));
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        if (DBG) Log.v(TAG, "onBitmapFailed");
        // XXX
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}