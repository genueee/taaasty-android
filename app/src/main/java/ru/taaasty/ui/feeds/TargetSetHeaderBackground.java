package ru.taaasty.ui.feeds;

import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.view.View;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.model.TlogDesign;
import ru.taaasty.ui.BackgroundBitmapDrawable;

/**
* Created by alexey on 18.07.14.
*/
public class TargetSetHeaderBackground implements Target {

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
        BackgroundBitmapDrawable background = new BackgroundBitmapDrawable(mTarget.getResources(), bitmap);
        ColorDrawable foreground = new ColorDrawable(mForegroundColor);
        if (TlogDesign.COVER_ALIGN_CENTER.equals(mDesign.getCoverAlign())) {
            background.setCoverAlign(BackgroundBitmapDrawable.COVER_ALIGN_CENTER_CROP);
        } else {
            background.setCoverAlign(BackgroundBitmapDrawable.COVER_ALIGN_STRETCH);
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
