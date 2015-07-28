package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;
import com.squareup.picasso.Callback;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import pl.droidsonroids.gif.AnimationListener;
import pl.droidsonroids.gif.GifDrawable;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.ImageInfo;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.ExtendedImageView;

public class ListImageEntry extends ListEntryBase implements Callback {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListImageEntry";

    private final ExtendedImageView mImageView;
    private final TextView mMoreImagesWidget;
    private final TextView mTitle;

    private Context mContext;
    private final LayerDrawable mImageLoadingDrawable;
    private final Drawable mImageLoadingBackgroundDrawable;
    private final Drawable mImageLoadingProgressIndicatorDrawable;
    private ImageLoadingGetter mImageGetter;

    private String mImageViewUrl;

    private User mUser;

    private TextViewImgLoader mTitleImgLoader;

    @Nullable
    private OkHttpClient mOkHttpClient = null;

    private Object mGitLoadTag = this;

    private boolean mAttachedToWindow;

    public ListImageEntry(final Context context, View v, boolean showAuthorAvatar) {
        super(context, v, showAuthorAvatar);

        mImageView = (ExtendedImageView) v.findViewById(R.id.image);
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mMoreImagesWidget = (TextView)v.findViewById(R.id.more_photos_indicator);

        mTitle.setMovementMethod(LinkMovementMethod.getInstance());
        if (Build.VERSION.SDK_INT <= 16) {
            // Оно там глючное, текст в списке съезжает вправо иногда
            mTitle.setTextIsSelectable(false);
        }

        mContext = context;
        mImageLoadingDrawable = (LayerDrawable)context.getResources().getDrawable(R.drawable.image_loading_with_progress);
        mImageLoadingBackgroundDrawable = mImageLoadingDrawable.findDrawableByLayerId(R.id.progress_background);
        mImageLoadingProgressIndicatorDrawable = mImageLoadingDrawable.findDrawableByLayerId(R.id.progress_indicator);
    }

    @Override
    public void setupEntry(final Entry entry, TlogDesign design) {
        TimingLogger timings = null;
        if (BuildConfig.DEBUG) timings = new TimingLogger(Constants.LOG_TAG, "setup EmbeddEntry");

        super.setupEntry(entry, design);
        mUser = entry.getAuthor();
        setupImage(entry, mParentWidth);
        setupTitle(entry, mParentWidth);
        setupMoreImages(entry);
        applyFeedStyle(design);

        if (BuildConfig.DEBUG && timings != null) {
            timings.addSplit("setup EmbeddEntry end");
            timings.dumpToLog();
        }
    }

    @Override
    public void applyFeedStyle(TlogDesign design) {
        super.applyFeedStyle(design);
        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? getFontManager().getPostSerifTypeface() : getFontManager().getPostSansSerifTypeface();
        mTitle.setTextColor(textColor);
        mTitle.setTypeface(tf);
        mMoreImagesWidget.setTextColor(getResources().getColor(design.isDarkTheme() ?
                R.color.text_color_feed_actions_dark_theme : R.color.text_color_feed_actions_light_theme));
    }

    @Override
    public void recycle() {
        picasso.cancelRequest(mImageView);
        if (mTitleImgLoader != null) mTitleImgLoader.reset();
        if (mOkHttpClient != null) mOkHttpClient.cancel(mGitLoadTag);
        recycleGifDrawable();
    }

    private void recycleGifDrawable() {
        Drawable drawable = mImageView.getDrawable();
        if (drawable != null && drawable instanceof GifDrawable){
            mImageView.setImageDrawable(null);
            GifDrawable gifDrawable = ((GifDrawable) drawable);
            gifDrawable.recycle();
        }
    }

    public void onAttachedToWindow() {
        mAttachedToWindow = true;
        startGifDrawable();
    }

    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        stopGifDrawable();
    }

    public void onStartScroll() {
        if (mAttachedToWindow) stopGifDrawable();
    }

    public void onStopScroll() {
        if (mAttachedToWindow) startGifDrawable();
    }

    private void startGifDrawable() {
        Drawable drawable = mImageView.getDrawable();
        if (drawable != null && drawable instanceof GifDrawable){
            ((GifDrawable) drawable).start();
        }
    }

    private void stopGifDrawable() {
        Drawable drawable = mImageView.getDrawable();
        if (drawable != null && drawable instanceof GifDrawable){
            ((GifDrawable) drawable).stop();
        }
    }

    private void setupImage(Entry item, int parentWidth) {
        ImageSize imgSize;
        int resizeToWidth = 0;
        int imgViewHeight;
        boolean fitMaxTextureSize = false;

        recycleGifDrawable();

        if (item.getImages().isEmpty()) {
            if (!TextUtils.isEmpty(item.getImageUrl())) {
                setupImageByUrl(item, parentWidth);
            } else {
                mImageView.setImageDrawable(null);
                mImageView.setVisibility(View.GONE);
            }
            return;
        }

        ImageInfo image = item.getImages().get(0);
        imgSize = image.image.geometry.toImageSize();
        imgSize.shrinkToWidth(parentWidth);

        if (imgSize.width < image.image.geometry.width) {
            // Изображение было уменьшено под размеры imageView
            resizeToWidth = parentWidth;
            imgViewHeight = (int)imgSize.height;
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(parentWidth);
            imgViewHeight = (int)imgSize.height;
        }

        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);

        // Здесь можем сломать aspect ratio, поэтому потом восстанавливаем его в picasso
        int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
        int thumborWidth = resizeToWidth  > 0 ? resizeToWidth : image.image.geometry.width;
        int thumborHeight = resizeToWidth > 0 ? imgViewHeight : image.image.geometry.height;
        if (thumborWidth > maxTextureSize) {
            thumborWidth = maxTextureSize;
            fitMaxTextureSize = true;
        }
        if (thumborHeight > maxTextureSize) {
            thumborHeight = maxTextureSize;
            fitMaxTextureSize = true;
        }
        if ((resizeToWidth != 0) || fitMaxTextureSize) {
            b.resize(thumborWidth,
                    fitMaxTextureSize ? thumborHeight : 0).fitIn();
        }

        ImageUtils.changeDrawableIntristicSizeAndBounds(mImageLoadingBackgroundDrawable, parentWidth, imgViewHeight);
        mImageView.setImageDrawable(mImageLoadingDrawable);
        mImageView.requestLayout();  //Иначе иногда ImageView не принимает новые размеры
        mImageViewUrl = b.toUrl();

        if (image.isAnimatedGif()) {
            loadGif(mImageViewUrl, mImageView);
        } else {
            mImageLoadingProgressIndicatorDrawable.setLevel(0);
            picasso
                    .load(mImageViewUrl)
                    .placeholder(mImageLoadingDrawable)
                    .error(R.drawable.image_load_error)
                    .noFade()
                    .into(mImageView, this);
        }
    }

    private void setupImageByUrl(Entry item, int parentWidth) {
        String imageUrl = item.getImageUrl();
        assert imageUrl != null;
        int height = (int)Math.ceil((float)parentWidth / Constants.DEFAULT_IMAGE_ASPECT_RATIO);

        ImageUtils.changeDrawableIntristicSizeAndBounds(mImageLoadingBackgroundDrawable, parentWidth, height);
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.VISIBLE);
        mImageView.setImageDrawable(mImageLoadingDrawable);
        mImageView.requestLayout();
        mImageViewUrl = imageUrl;

        if (imageUrl.toLowerCase(Locale.US).endsWith(".gif")) {
            loadGif(mImageViewUrl, mImageView);
            picasso.cancelRequest(mImageView);
        } else {
            mImageLoadingProgressIndicatorDrawable.setLevel(0);
            picasso
                    .load(mImageViewUrl)
                    .placeholder(mImageLoadingDrawable)
                    .error(R.drawable.image_load_error)
                    .resize(parentWidth, 0)
                    .noFade()
                    .into(mImageView, this);
        }
    }

    public View getImageView() {
        return mImageView;
    }

    public View getMoreImagesWidget() {
        return mMoreImagesWidget;
    }

    public String getImageViewUrl() {
        return mImageViewUrl;
    }

    private void setupTitle(Entry item, int parentWidth) {
        if (!item.hasTitle()) {
            mTitle.setVisibility(View.GONE);
            return;
        }

        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(
                guessViewVisibleWidth(mTitle),
                mContext);

        CharSequence title = UiUtils.formatEntryTextSpanned(item.getTitleSpanned(), mImageGetter);
        mTitle.setText(title, TextView.BufferType.NORMAL);
        mTitleImgLoader = TextViewImgLoader.bindAndLoadImages(mTitle, onImgClickListener);
        mTitle.setVisibility(View.VISIBLE);
    }

    private void setupMoreImages(Entry item) {
        int count_more = item.getImages().size() - 1;
        if (count_more > 0) {
            String text = mMoreImagesWidget.getResources().getQuantityString(R.plurals.more_images, count_more, count_more);
            mMoreImagesWidget.setText(text);
            mMoreImagesWidget.setVisibility(View.VISIBLE);
        } else {
            mMoreImagesWidget.setVisibility(View.GONE);
        }
    }

    @Override
    public void onSuccess() {
        mImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    public void onError() {
        // 9patch нормально скалится только при использовании FIT_XY
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    private void loadGif(String url, final ImageView imageView) {
        if (mOkHttpClient == null) {
            mOkHttpClient = NetworkUtils.getInstance().getOkHttpClient();
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        mImageLoadingProgressIndicatorDrawable.setLevel(0);
        mImageView.setImageDrawable(mImageLoadingDrawable);
        mOkHttpClient
                .newCall(request)
                .enqueue(new com.squareup.okhttp.Callback() {
                    @Override
                    public void onFailure(Request request, IOException e) {
                        reportError(e);
                    }

                    @Override
                    public void onResponse(Response response) throws IOException {
                        try {
                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            }
                            byte content[];
                            if (response.networkResponse() == null) {
                                content = response.body().bytes();
                            } else {
                                content = readResponseWithProgress(response);
                            }

                            final GifDrawable drawable = new GifDrawable(content);
                            if (drawable.getLoopCount() != 0) initLoopForever(drawable);
                            imageView.post(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                    imageView.setImageDrawable(drawable);
                                    mImageLoadingProgressIndicatorDrawable.setLevel(0);
                                }
                            });
                        } catch (Throwable e) {
                            reportError(e);
                        }
                    }

                    class SetProgressRunnable  implements Runnable {
                        private final Drawable mDrawable;
                        private final int mLevel;

                        public SetProgressRunnable(Drawable drawable, long progress, long max) {
                            mDrawable = drawable;
                            if (max == 0) {
                                mLevel = 0;
                            } else {
                                if (progress > max) progress = max;
                                mLevel = (int) ((float) progress * 10000f / (float) max);
                            }
                        }

                        @Override
                        public void run() {
                            mDrawable.setLevel(mLevel);
                        }
                    }

                    private byte[] readResponseWithProgress(Response response) throws  IOException {
                        byte bytes[];
                        int pos;
                        int nRead;
                        long lastTs, lastPos;

                        final long contentLength = response.body().contentLength();
                        if (contentLength < 0 || contentLength > Integer.MAX_VALUE) {
                            throw new IOException("Cannot buffer entire body for content length: " + contentLength);
                        }

                        mImageView.post(new SetProgressRunnable(mImageLoadingProgressIndicatorDrawable, 0, contentLength));
                        InputStream source = response.body().byteStream();

                        bytes = new byte[(int)contentLength];
                        pos = 0;
                        lastTs = System.nanoTime();
                        lastPos = 0;
                        try {
                            while ((nRead = source.read(bytes, pos, bytes.length - pos)) != -1) {
                                pos += nRead;

                                long newTs = System.nanoTime();
                                if ((lastPos != pos) && ((newTs - lastTs >= 200 * 1e6) || (pos == bytes.length))) {
                                    lastTs = newTs;
                                    lastPos = pos;
                                    mImageView.post(new SetProgressRunnable(mImageLoadingProgressIndicatorDrawable, pos, contentLength));
                                }
                                if (pos == bytes.length) break;
                            }
                        } finally {
                            Util.closeQuietly(source);
                            mImageView.post(new SetProgressRunnable(mImageLoadingProgressIndicatorDrawable, 0, 0));
                        }

                        if (contentLength != -1 && contentLength != bytes.length) {
                            throw new IOException("Content-Length and stream length disagree");
                        }

                        return bytes;
                    }

                    private void reportError(final Throwable exception) {
                        Log.i("ListImageEntry", "load gif error", exception);
                        imageView.post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageResource(R.drawable.image_load_error);
                                imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                            }
                        });
                    }

                    private void initLoopForever(final GifDrawable drawable) {
                        drawable.addAnimationListener(new AnimationListener() {
                            @Override
                            public void onAnimationCompleted() {
                                imageView.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        drawable.start();
                                    }
                                }, 3000);
                            }
                        });
                    }
                });

    }

    private final TextViewImgLoader.OnClickListener onImgClickListener = new TextViewImgLoader.OnClickListener() {
        @Override
        public void onImageClicked(TextView widget, String source) {
            CharSequence seq = widget.getText();
            ArrayList<String> sources = UiUtils.getImageSpanUrls(seq);
            if (!sources.isEmpty()) {
                ShowPhotoActivity.startShowPhotoActivity(widget.getContext(), mUser, mTitle.getText().toString(), sources, null, widget);
            }
        }
    };
}
