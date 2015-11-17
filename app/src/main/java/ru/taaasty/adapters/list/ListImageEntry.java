package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.TimingLogger;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.picasso.Callback;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.Locale;

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
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.ExtendedImageView;
import ru.taaasty.widgets.MyRecyclerView;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class ListImageEntry extends ListEntryBase implements Callback, MyRecyclerView.ScrollEventConsumerVh {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListImageEntry";

    private final ExtendedImageView mImageView;
    private final TextView mMoreImagesWidget;
    private final TextView mTitle;

    private Context mContext;

    private ImageLoadingGetter mImageGetter;

    private String mImageViewUrl;

    private User mUser;

    private TextViewImgLoader mTitleImgLoader;

    @Nullable
    private OkHttpClient mOkHttpClient = null;

    private final Object mGitLoadTag = this;

    private Subscription mLoadGifSubscription = Subscriptions.unsubscribed();

    public ListImageEntry(final Context context, View v, boolean showAuthorAvatar) {
        super(context, v, showAuthorAvatar);

        mImageView = (ExtendedImageView) v.findViewById(R.id.image);
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mMoreImagesWidget = (TextView)v.findViewById(R.id.more_photos_indicator);

        mTitle.setMovementMethod(LinkMovementMethodNoSelection.getInstance());
        if (Build.VERSION.SDK_INT <= 16) {
            // Оно там глючное, текст в списке съезжает вправо иногда
            mTitle.setTextIsSelectable(false);
        }

        mContext = context;
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
        mLoadGifSubscription.unsubscribe();
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

    public void onStartScroll() {
        stopGifDrawable();
    }

    public void onStopScroll() {
        startGifDrawable();
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

    /**
     * Создаем каждый раз новый drawable, так как размеры у нас меняются, а ImageView никак не обрабатывает
     * смену размеров у уже установленного drawable
     */
    private LayerDrawable createImageLoadingDrawable(int width, int height) {
        LayerDrawable drawable = (LayerDrawable)mContext.getResources()
                .getDrawable(R.drawable.image_loading_with_progress)
                .mutate();
        Drawable imageLoadingDrawable = drawable.findDrawableByLayerId(R.id.progress_background);
        ImageUtils.changeDrawableIntristicSizeAndBounds(imageLoadingDrawable, width, height);
        return drawable;
    }

    private void setupImage(Entry item, int parentWidth) {
        ImageSize imgSize;
        int resizeToWidth = 0;
        int imgViewHeight;
        boolean fitMaxTextureSize = false;

        mLoadGifSubscription.unsubscribe();
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

        mImageViewUrl = b.toUrl();
        if (image.isAnimatedGif()) {
            ImageUtils.loadGifWithProgress(mImageView, mImageViewUrl, mGitLoadTag, parentWidth, imgViewHeight, this);
        } else {
            Drawable placeholder = createImageLoadingDrawable(parentWidth, imgViewHeight);
            mImageView.setImageDrawable(placeholder);
            picasso
                    .load(mImageViewUrl)
                    .placeholder(placeholder)
                    .error(R.drawable.image_load_error)
                    .noFade()
                    .into(mImageView, this);
        }
    }

    private void setupImageByUrl(Entry item, int parentWidth) {
        String imageUrl = item.getImageUrl();
        assert imageUrl != null;
        int height = (int)Math.ceil((float)parentWidth / Constants.DEFAULT_IMAGE_ASPECT_RATIO);

        mImageViewUrl = imageUrl;
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.VISIBLE);

        if (imageUrl.toLowerCase(Locale.US).endsWith(".gif")) {
            ImageUtils.loadGifWithProgress(mImageView, mImageViewUrl, mGitLoadTag, parentWidth, height, this);
            picasso.cancelRequest(mImageView);
        } else {
            Drawable placeholder = createImageLoadingDrawable(parentWidth, height);
            mImageView.setImageDrawable(placeholder);
            picasso
                    .load(mImageViewUrl)
                    .placeholder(placeholder)
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

    private final TextViewImgLoader.OnClickListener onImgClickListener = new TextViewImgLoader.OnClickListener() {
        @Override
        public void onImageClicked(TextView widget, String source) {
            CharSequence seq = widget.getText();
            ArrayList<String> sources = UiUtils.getImageSpanUrls(seq);
            if (!sources.isEmpty()) {
                ShowPhotoActivity.startShowPhotoActivity(widget.getContext(), mTitle.getText().toString(), sources, null, widget);
            }
        }
    };
}
