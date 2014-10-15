package ru.taaasty.utils;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import it.sephiroth.android.library.picasso.Picasso;
import ru.taaasty.R;
import ru.taaasty.model.Userpic;
import ru.taaasty.widgets.PicassoDrawable;

public abstract class ActionbarUserIconLoader {
    private final ActionBar mActionBar;
    private  Context mContext;

    public ActionbarUserIconLoader(Context context, ActionBar ab) {
        mActionBar = ab;
        mContext = context;
    }

    public abstract void onBitmapFailed(Drawable errorDrawable);

    public void loadIcon(Userpic userpic, String username) {
        ImageUtils.getInstance().loadAvatar(mContext, userpic, username,
                mPicassoTarget, android.R.dimen.app_icon_size);
    }

    private final ImageUtils.DrawableTarget mPicassoTarget = new ImageUtils.DrawableTarget() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            if (mActionBar == null) return;
            if (Picasso.LoadedFrom.MEMORY.equals(from)) {
                mActionBar.setIcon(new BitmapDrawable(mContext.getResources(), bitmap));
            } else {
                Drawable placeholder = mContext.getResources().getDrawable(R.drawable.ic_user_stub);
                if (placeholder instanceof AnimationDrawable) {
                    ((AnimationDrawable) placeholder).stop();
                }
                PicassoDrawable drawable =
                        new PicassoDrawable(mContext, bitmap, placeholder, from, false, false);
                mActionBar.setIcon(drawable);
            }
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            ActionbarUserIconLoader.this.onBitmapFailed(errorDrawable);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        @Override
        public void onDrawableReady(Drawable drawable) {
            if (mActionBar == null) return;
            mActionBar.setIcon(drawable);
        }
    };
}
