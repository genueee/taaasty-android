package ru.taaasty.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.R;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.widgets.DefaultUserpicDrawable;
import ru.taaasty.widgets.PicassoDrawable;

public class ImageUtils {

    private final CircleTransformation mCircleTransformation;

    private static ImageUtils sInstance;

    private ImageUtils() {
        mCircleTransformation = new CircleTransformation();
    }

    public static ImageUtils getInstance() {
        if (sInstance == null) sInstance = new ImageUtils();
        return sInstance;
    }

    public static int calculateInSampleSize(
            int width, int height,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public void loadAvatar(User a, ImageView dst, @DimenRes int diameterResource) {
        loadAvatar(
                a == null ? Userpic.DUMMY : a.getUserpic(),
                a == null ? "" : a.getName(),
                dst,
                diameterResource);
    }

    public void loadAvatar(@Nullable Userpic userpic,
                           String userName,
                           ImageView dst,
                           @DimenRes int diameterResource) {
        loadAvatar(dst.getContext(), userpic, userName, new ImageViewTarget(dst), diameterResource);
    }

    public void loadAvatar(
            Context context,
            @Nullable Userpic userpic,
            String userName,
            DrawableTarget target,
            @DimenRes int diameterResource) {
        ThumborUrlBuilder thumborUrl;
        int avatarDiameter;
        Drawable defaultUserpicDrawable;
        Drawable stubPlaceholder;
        Picasso picasso;

        avatarDiameter = context.getResources().getDimensionPixelSize(diameterResource);
        defaultUserpicDrawable = new DefaultUserpicDrawable(userpic, userName);
        defaultUserpicDrawable.setBounds(0, 0, avatarDiameter, avatarDiameter); // Ставим bounds врчучную, иначе мерцает при скролле
        if (userpic == null || (TextUtils.isEmpty(userpic.thumborPath) && TextUtils.isEmpty(userpic.thumb128Url))) {
            target.onDrawableReady(defaultUserpicDrawable);
            return;
        }

        picasso = NetworkUtils.getInstance().getPicasso(context);
        stubPlaceholder = context.getResources().getDrawable(R.drawable.ic_user_stub);
        stubPlaceholder.setBounds(0, 0, avatarDiameter, avatarDiameter); // Ставим bounds врчучную, иначе мерцает при скролле

        if (!TextUtils.isEmpty(userpic.thumborPath)) {
            thumborUrl = NetworkUtils.createThumborUrlFromPath(userpic.thumborPath);
            String userpicUrl = thumborUrl.resize(avatarDiameter, avatarDiameter)
                    .toUrl();
            // if (DBG) Log.d(TAG, "userpicUrl: " + userpicUrl);
            picasso.load(userpicUrl)
                    .placeholder(stubPlaceholder)
                    .error(defaultUserpicDrawable)
                    .transform(mCircleTransformation)
                    .into(target);
        } else {
            picasso.load(userpic.getOptimalUrlForSize(avatarDiameter, avatarDiameter))
                    .resize(avatarDiameter, avatarDiameter)
                    .centerCrop()
                    .placeholder(stubPlaceholder)
                    .error(defaultUserpicDrawable)
                    .transform(mCircleTransformation)
                    .into(target);

        }
    }

    public static interface DrawableTarget extends Target {
        public void onDrawableReady(Drawable drawable);
    }

    private static class ImageViewTarget implements DrawableTarget {

        private final ImageView mView;

        public ImageViewTarget(ImageView view) {
            mView = view;
            mView.setTag(R.id.picasso_target, this); // anti-picasso weak ref
        }

        @Override
        public void onDrawableReady(Drawable drawable) {
            mView.setImageDrawable(drawable);
            removeTag();
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (mView.getContext() == null) return;
            PicassoDrawable.setBitmap(mView, mView.getContext(), bitmap, from, false, false);
            removeTag();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            if (errorDrawable != null) mView.setImageDrawable(errorDrawable);
            removeTag();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            if (placeHolderDrawable != null) mView.setImageDrawable(placeHolderDrawable);
        }

        private void removeTag() {
            if (mView.getTag(R.id.picasso_target) == this) mView.setTag(R.id.picasso_target, null);
        }
    }

}
