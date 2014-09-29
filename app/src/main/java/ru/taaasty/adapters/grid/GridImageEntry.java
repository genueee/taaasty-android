package ru.taaasty.adapters.grid;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

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
public class GridImageEntry {
    private final FrameLayout mImageLayout;
    private final ImageView mImageView;
    private final Drawable mImagePlaceholderDrawable;
    private final EllipsizingTextView mTitle;

    private Context mContext;
    private final Picasso mPicasso;
    private ImageLoadingGetter mImageGetter;

    public GridImageEntry(Context context, View v) {
        mImageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
        mImageView = (ImageView) mImageLayout.findViewById(R.id.image);
        mTitle = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);

        mContext = context;
        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        Resources resources = context.getResources();
        mImagePlaceholderDrawable = new ColorDrawable(resources.getColor(R.color.grid_item_image_loading_color));

        mTitle.setMaxLines(10);
    }

    public void setupEntry(Entry entry, int parentWidth) {
        setupImage(entry, parentWidth);
        setupTitle(entry, parentWidth);
    }

    private void setupImage(Entry item, int parentWidth) {
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
        imgSize.shrinkToWidth(parentWidth);
        imgSize.shrinkToMaxTextureSize();

        if (imgSize.width < image.image.geometry.width) {
            // Изображение было уменьшено под размеры imageView
            resizeToWidth = parentWidth;
            imgViewHeight = (int)Math.ceil(imgSize.height);
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(parentWidth);
            imgSize.cropToMaxTextureSize();
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        mImageView.setMinimumHeight(imgViewHeight);
        mImageLayout.setForeground(null);
        mImageLayout.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);

        mPicasso
                .load(b.toUrl())
                .placeholder(mImagePlaceholderDrawable)
                .error(R.drawable.image_loading_drawable)
                .into(mImageView);
    }

    private void setupTitle(Entry item, int parentWidth) {
        if (!item.hasTitle()) {
            mTitle.setVisibility(View.GONE);
            return;
        }

        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(parentWidth, mContext);
        CharSequence title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(item.getTitle(), null, null));

        mTitle.setText(Html.fromHtml(title.toString(), mImageGetter, null), TextView.BufferType.NORMAL);
        TextViewImgLoader.bindAndLoadImages(mTitle);
        mTitle.setVisibility(View.VISIBLE);
    }
}