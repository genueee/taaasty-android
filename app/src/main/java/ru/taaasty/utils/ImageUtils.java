package ru.taaasty.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.ImageView;

import com.aviary.android.feather.headless.utils.MegaPixels;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.sdk.FeatherActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.NativeLruCache;
import ru.taaasty.R;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.widgets.DefaultUserpicDrawable;
import ru.taaasty.widgets.PicassoDrawable;

public class ImageUtils {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ImageUtils";

    private static final String IMAGE_CACHE_PREFIX = "_tasty_";

    private final CircleTransformation mCircleTransformation;

    private int mMaxTextureSize ;

    private static ImageUtils sInstance;

    private NativeLruCache mCache;

    private ImageUtils() {
        mCircleTransformation = new CircleTransformation();
        mCache = NetworkUtils.getInstance().getImageCache();
    }

    public static ImageUtils getInstance() {
        if (sInstance == null) sInstance = new ImageUtils();
        return sInstance;
    }

    public void putBitmapToCache(String key, Bitmap bitmap) {
        mCache.set(IMAGE_CACHE_PREFIX + key, bitmap);
    }

    @Nullable
    public Bitmap getBitmapFromCache(String key) {
        return mCache.get(IMAGE_CACHE_PREFIX + key);
    }

    public Bitmap removeBitmapFromCache(String key) {
        return mCache.remove(IMAGE_CACHE_PREFIX + key);
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
     * Загрузка ресурса backgroundResId размером на весь экран, с crop'ом с сохранением пропорций и
     * вычислением  inSampleSize под размеры текущего экрана
     * @param backgroundResId ID ресурса
     * @param dstSize Размеры, в которые будет вставляться изображение
     * @param inSampleSizeAdd Степень двойки, прибавляется к inSampleSize.
     * @return Bitmap
     */
    public static Bitmap decodeBackgroundBitmap(Activity activity, int backgroundResId,
                                                Point dstSize,
                                                int inSampleSizeAdd) {
        float scale, scaleX, scaleY;
        TimingLogger timings = null;

        if (DBG) timings = new TimingLogger("Taaasty", "decodeBackgroundBitmap");

        InputStream is = null;
        try {
            is = activity.getResources().openRawResource(backgroundResId);
            BitmapRegionDecoder bre = BitmapRegionDecoder.newInstance(is, true);

            Rect region = new Rect(0, 0, bre.getWidth(), bre.getHeight());

            scaleY = dstSize.y / (float)bre.getHeight();
            scaleX = dstSize.x / (float)bre.getWidth();
            scale = Math.max(scaleX, scaleY);

            float scaledDisplayWidth = dstSize.x / scale;
            float scaledDisplayHeight = dstSize.y / scale;

            int leftTop = (int)(region.centerX() - (scaledDisplayWidth / 2));
            region.intersect(leftTop, 0, (int)Math.ceil(leftTop + scaledDisplayWidth), (int)Math.ceil(scaledDisplayHeight));
            if (DBG) Log.v(TAG, "region: " + region);

            final BitmapFactory.Options options;
            options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inMutable = false;

            int samplesize = ImageUtils.calculateInSampleSize(
                    bre.getHeight(),
                    bre.getWidth(),
                    (int)(bre.getHeight() * scale),
                    (int)(bre.getWidth() * scale));

            if (inSampleSizeAdd == 0 ) {
                options.inSampleSize = samplesize;
            } else {
                options.inSampleSize = 1 << (int)( inSampleSizeAdd + Math.log(samplesize) / Math.log(2));
            }

            return bre.decodeRegion(region, options);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (DBG) timings.dumpToLog();
        }
        return null;
    }


    public static Drawable changeDrawableIntristicSizeAndBounds(Drawable drawable, int width, int height) {
        Drawable mutable = drawable.mutate();
        mutable.setBounds(0, 0, width, height);
        if (drawable instanceof GradientDrawable) {
            GradientDrawable gd = (GradientDrawable)mutable;
            gd.setSize(width, height);
        } else if (drawable instanceof ShapeDrawable) {
            ShapeDrawable sh = (ShapeDrawable)mutable;
            sh.setIntrinsicWidth(width);
            sh.setIntrinsicHeight(height);
        } else if (drawable instanceof NinePatchDrawable) {
            drawable.setBounds(0, 0, width, height); // XXX: а оно точно работает?
        } else {
            drawable.setBounds(0, 0, width, height);
            if (BuildConfig.DEBUG) Log.e("ImageUtils", "changeDrawableIntristicSizeAndBounds() of " + drawable.getClass().toString(),  new IllegalStateException("unsupported"));
        }
        return mutable;

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

    public static boolean isUriInPicturesDirectory(Context context, Uri uri) {
        File picturesDir = getPicturesDirectory(context);
        File uriPath;
        if (uri == null) return false;
        if (picturesDir == null) return false;

        if (uri.toString().startsWith("/")) {
            uriPath = new File(uri.toString());
        } else {
            if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return false;
            }
            uriPath = new File(uri.getPath());
        }
        return uriPath.getAbsolutePath().startsWith(picturesDir.getAbsolutePath());
    }

    /**
     * Имя файла для фотографии из времени timestamp, без .jpg
     * @param timestamp
     * @return
     */
    public static String getOutputMediaFileName(Date timestamp, String prefix) {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(timestamp);
        return prefix + ts;
    }

    public static Uri createAviaryPictureOutputPath(Context context) throws MakePhotoException {
        return createPictureOutputPath(context, true);
    }

    public static Uri createPictureOutputPath(Context context) throws MakePhotoException {
        return createPictureOutputPath(context, false);
    }

    public static Uri createPictureOutputPath(Context context, boolean isAviary) throws MakePhotoException {
        Date currentDate;
        File storageDir;
        File image;
        String imageFileName;

        storageDir = ImageUtils.getPicturesDirectory(context);
        if (storageDir == null) {
            throw new MakePhotoException(R.string.error_no_place_to_save);
        }
        currentDate = new Date();
        imageFileName = ImageUtils.getOutputMediaFileName(currentDate, isAviary ? "aviary_" : "IMG_");
        image = new File(storageDir, imageFileName + ".jpg");
        return Uri.fromFile(image);
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
        Intent takePictureIntent;
        Uri currentPhotoUri;

        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) == null) {
            throw new MakePhotoException(R.string.error_camera_not_available);
        }

        currentPhotoUri = createPictureOutputPath(context);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
        if (usefrontCamera) takePictureIntent.putExtra("android.intent.extras.CAMERA_FACING", 1);
        return takePictureIntent;
    }

    public static Intent createFeatherPhotoIntent(Context context, Uri originalPhotoUri) throws MakePhotoException {
        Intent featherPhotoIntent;
        Uri newPhotoUri;

        newPhotoUri = createAviaryPictureOutputPath(context);
        featherPhotoIntent = new Intent(context , FeatherActivity.class );
        featherPhotoIntent.setData(originalPhotoUri);
        featherPhotoIntent.putExtra( Constants.EXTRA_IN_API_KEY_SECRET, context.getString(R.string.aviary_secret) );
        featherPhotoIntent.putExtra( Constants.EXTRA_IN_SAVE_ON_NO_CHANGES, false );
        featherPhotoIntent.putExtra( Constants.EXTRA_OUTPUT, newPhotoUri );
        featherPhotoIntent.putExtra( Constants.EXTRA_IN_HIRES_MEGAPIXELS, MegaPixels.Mp3.ordinal());
        return featherPhotoIntent;
    }

    public void loadAvatar(@Nullable User a, ImageView dst, @DimenRes int diameterResource) {
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

        private final boolean mAttachTag;
        private final ImageView mView;

        public ImageViewTarget(ImageView view) {
            this(view, true);
        }

        public ImageViewTarget(ImageView view, boolean attachTag) {
            mView = view;
            mAttachTag = attachTag;
            if (attachTag) mView.setTag(R.id.picasso_target, this); // anti-picasso weak ref
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
            if (mAttachTag && mView.getTag(R.id.picasso_target) == this) mView.setTag(R.id.picasso_target, null);
        }
    }

    public void onAppInit() {
        initMaxTextureSize();
    }

    private void initMaxTextureSize() {
        // XXX: не работает нихера
        /*
        int[] maxTextureSize = new int[]{0};
        try {
            GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxTextureSize, 0);
            mMaxTextureSize = maxTextureSize[0];
            if (maxTextureSize[0] == 0) {
                Log.v("ImageUtils", "GL_MAX_TEXTURE_SIZE is 0");
                EGL10 egl = (EGL10) EGLContext.getEGL();
                EGLContext ctx = egl.eglGetCurrentContext();
                GL10 gl = (GL10) ctx.getGL();
                IntBuffer val = IntBuffer.allocate(1);
                gl.glGetIntegerv(GL10.GL_MAX_TEXTURE_SIZE, val);
                mMaxTextureSize = val.get();
                if (mMaxTextureSize == 0) {
                    Log.v("ImageUtils", "GL_MAX_TEXTURE_SIZE - 2 is 0");
                    mMaxTextureSize = 2048;
                }
            }
        } catch (Throwable ex) {
            Log.i("ImageUtils", "initMaxTextureSize() error", ex);
            mMaxTextureSize = 2048;
        }
        */
        mMaxTextureSize = 2048;
    }

    public int getMaxTextureSize() {
        return mMaxTextureSize;
    }

}
