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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
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
import ru.taaasty.utils.Size;
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
    public void setupEntry(final Entry entry, TlogDesign design, String feedId) {
        TimingLogger timings = null;
        if (BuildConfig.DEBUG) timings = new TimingLogger(Constants.LOG_TAG, "setup EmbeddEntry");

        super.setupEntry(entry, design, feedId);
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
        Picasso.with(mContext).cancelRequest(mImageView);
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
            // gifDrawable.recycle(); // recycle делать нельзя, так как у нас 1 может быть в нескольких холдерах
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
        Size pictureSize, imgViewSize;
        boolean resizeToWidth;
        int thumborWidth, thumborHeight;
        boolean fitMaxTextureSize = false;

        mLoadGifSubscription.unsubscribe();
        recycleGifDrawable();

        if (item.getImages().isEmpty()) {
            // Если в список изображений пуст, пробуем поле Entry.imageUrl. Вроде никогда не должно срабатывать
            if (!TextUtils.isEmpty(item.getImageUrl())) {
                setupImageByUrl(item, parentWidth);
            } else {
                mImageView.setImageDrawable(null);
                mImageView.setVisibility(View.GONE);
            }
            return;
        }

        ImageInfo image = item.getImages().get(0);
        pictureSize = image.image.geometry.toImageSize(); // Изображение
        imgViewSize = calculateImageViewSize(pictureSize, parentWidth); // ImageView под неё

        resizeToWidth = pictureSize.width > imgViewSize.width;
        thumborWidth = (int)Math.min(imgViewSize.width, pictureSize.width);
        thumborHeight = (int)Math.min(imgViewSize.height, pictureSize.height);

        int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
        if (thumborWidth > maxTextureSize) {
            thumborWidth = maxTextureSize;
            fitMaxTextureSize = true;
        }
        if (thumborHeight > maxTextureSize) {
            thumborHeight = maxTextureSize;
            fitMaxTextureSize = true;
        }

        ThumborUrlBuilder b = NetworkUtils.createThumborUrl(image.image.url);
        if (resizeToWidth || fitMaxTextureSize) {
            b.resize(thumborWidth,
                    fitMaxTextureSize ? thumborHeight : 0)
                    .filter(ThumborUrlBuilder.noUpscale())
                    .fitIn(); // иначе оно кропает длиннокартинки
        }

        doLoadUrl(b.toUrlUnsafe(), (int)imgViewSize.width, (int)imgViewSize.height, image.isAnimatedGif(), false);
    }

    private static Size calculateImageViewSize(Size imgSize, int parentWidth) {
        Size dst = new Size(imgSize.width, imgSize.height);
        dst.shrinkToWidth(parentWidth);
        dst.stretchToWidth(parentWidth);
        return dst;
    }

    private void setupImageByUrl(Entry item, int parentWidth) {
        String imageUrl = item.getImageUrl();
        int height = (int)Math.ceil((float)parentWidth / Constants.DEFAULT_IMAGE_ASPECT_RATIO);
        // Геометрии у нас нет, через thumbor не пропускаем
        doLoadUrl(imageUrl, parentWidth, height, imageUrl.toLowerCase(Locale.US).endsWith(".gif"), true);
    }

    private void doLoadUrl(String url, int width, int height, boolean isAnimatedGif, boolean resizeToWidth) {
        mImageViewUrl = url;
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.VISIBLE);

        if (url.toLowerCase(Locale.US).endsWith(".gif")) {
            ImageUtils.loadGifWithProgress(mImageView, mImageViewUrl, mGitLoadTag, width, height, this);
            Picasso.with(mContext).cancelRequest(mImageView);
        } else {
            Drawable placeholder = createImageLoadingDrawable(width, height);
            mImageView.setImageDrawable(placeholder);
            RequestCreator rq = Picasso.with(mContext)
                    .load(mImageViewUrl)
                    .placeholder(placeholder)
                    .error(R.drawable.image_load_error)
                    .noFade();
            if (resizeToWidth) {
                rq.resize(width, 0).onlyScaleDown();
            }
            rq.into(mImageView, this);
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
