package ru.taaasty.ui.feeds;

import android.graphics.Bitmap;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.model.TlogDesign;

/**
* Created by alexey on 18.07.14.
*/
class TargetSetHeaderBackground implements Target {

    private final View mTarget;
    private final TlogDesign mDesign;
    private final int mForegroundColor;

    public TargetSetHeaderBackground(View target, TlogDesign design, int foregroundColor) {
        mTarget = target;
        mDesign = design;
        mForegroundColor = foregroundColor;
    }

    @Override
    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        BitmapDrawable background = new BitmapDrawable(mTarget.getResources(), bitmap);
        ColorDrawable foreground = new ColorDrawable(mForegroundColor);
        if (TlogDesign.COVER_ALIGN_CENTER.equals(mDesign.getCoverAlign())) {
            background.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        } else {
            background.setGravity(Gravity.FILL);
        }
        mTarget.setBackgroundDrawable(
                new LayerDrawable(new Drawable[] {background, foreground}));
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        // XXX
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}
