package ru.taaasty.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

    /**
     * Директория для фотографий
     * @param context
     * @return
     */
    @Nullable
    public static File getPicturesDirectory(Context context) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), context.getString(R.string.images_directory));

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("ImageUtils", "failed to create directory");
                return null;
            }
        }
        return mediaStorageDir;
    }

    /**
     * Имя файла для фотографии из времени timestamp, без .jpg
     * @param timestamp
     * @return
     */
    public static String getOutputMediaFileName(Date timestamp) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(timestamp);
        return "IMG_" + ts;
    }

    /**
     * Добавление фоторгафии в галерею после фотографии
     * @param context
     * @param photoUri путь к файлу (file:/media...)
     */
    public static void galleryAddPic(Context context, Uri photoUri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        // File f = new File(photoPath);
        // Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(photoUri);
        context.sendBroadcast(mediaScanIntent);
    }

    public static Intent createPickImageActivityIntent() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        return photoPickerIntent;
    }

    public static Intent createMakePhotoIntent(Context context, boolean usefrontCamera) throws MakePhotoException {
        File storageDir;
        File image;
        Intent takePictureIntent;
        Date currentDate;
        String imageFileName;
        Uri currentPhotoUri;

        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) == null) {
            throw new MakePhotoException(R.string.error_camera_not_available);
        }
        storageDir = ImageUtils.getPicturesDirectory(context);
        if (storageDir == null) {
            throw new MakePhotoException(R.string.error_no_place_to_save);
        }
        currentDate = new Date();
        imageFileName = ImageUtils.getOutputMediaFileName(currentDate);
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            currentPhotoUri = Uri.fromFile(image);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
            if (usefrontCamera) takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
            return takePictureIntent;
        } catch (IOException e) {
            e.printStackTrace();
            throw new MakePhotoException(R.string.error_can_not_create_file_for_photo, e);
        }
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

    public static class MakePhotoException extends Exception {
        public int errorResourceId;

        public MakePhotoException(int resourceId) {
            super();
            errorResourceId = resourceId;
        }

        public MakePhotoException(int resourceId, Throwable e) {
            super(e);
            errorResourceId = resourceId;
        }

    }

    public static class ImageViewTarget implements DrawableTarget {

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
