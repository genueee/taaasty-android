package ru.taaasty.adapters.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.pollexor.ThumborUrlBuilder;

import it.sephiroth.android.library.picasso.Callback;
import it.sephiroth.android.library.picasso.Picasso;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

/**
* Created by alexey on 28.09.14.
*/
public class GridImageEntry extends GridEntryBase implements Callback  {
    private final FrameLayout mImageLayout;
    private final ImageView mImageView;
    private final Drawable mImagePlaceholderDrawable;
    private final Drawable mGifForegroundDrawable;
    private final EllipsizingTextView mTitle;

    private final Picasso mPicasso;
    private ImageLoadingGetter mImageGetter;

    public GridImageEntry(Context context, View v, int cardWidth) {
        super(context, v, cardWidth);
        mImageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
        mImageView = (ImageView) mImageLayout.findViewById(R.id.image);
        mTitle = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);

        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        Resources resources = context.getResources();
        mImagePlaceholderDrawable = new ColorDrawable(resources.getColor(R.color.grid_item_image_loading_color));
        mGifForegroundDrawable = resources.getDrawable(R.drawable.embedd_play_foreground);

        mTitle.setMaxLines(10);
    }

    @Override
    public void bindEntry(Entry entry) {
        setupImage(entry);
        setupTitle(entry);
    }

    @Override
    public void recycle() {
        mImageView.setImageDrawable(mImagePlaceholderDrawable);
        mTitle.setText(null);
    }

    private void setupImage(Entry item) {
        ImageSize imgSize;
        int resizeToWidth = 0;
        int imgViewHeight;

        if (item.getImages().isEmpty()) {
            mImageLayout.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = item.getImages().get(0);
        // XXX: check for 0
        imgSize = image.image.geometry.toImageSize();
        imgSize.shrinkToWidth(mCardWidth);
        imgSize.shrinkToMaxTextureSize();

        if (imgSize.width < image.image.geometry.width) {
            // Изображение было уменьшено под размеры imageView
            resizeToWidth = mCardWidth;
            imgViewHeight = (int)Math.ceil(imgSize.height);
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(mCardWidth);
            imgSize.cropToMaxTextureSize();
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)mImageView.getLayoutParams();
        lp.height = imgViewHeight;
        mImageView.setLayoutParams(lp);


        if (image.isAnimatedGif()) {
            mImageLayout.setForeground(mGifForegroundDrawable);
        } else {
            mImageLayout.setForeground(null);
        }
        mImageLayout.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);

        mPicasso
                .load(b.toUrl())
                .placeholder(mImagePlaceholderDrawable)
                .error(R.drawable.image_loading_drawable)
                .into(mImageView, this);
    }

    private void setupTitle(Entry item) {
        if (!item.hasTitle()) {
            mTitle.setVisibility(View.GONE);
            return;
        }

        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(mCardWidth, mContext);
        CharSequence title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(item.getTitle(), null, null));

        mTitle.setText(Html.fromHtml(title.toString(), mImageGetter, null), TextView.BufferType.NORMAL);
        TextViewImgLoader.bindAndLoadImages(mTitle);
        mTitle.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSuccess() {
        mImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

    @Override
    public void onError() {
        // 9patch нормально скалится только при использовании FIT_XY
        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
    }
}
