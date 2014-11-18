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

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

/**
* Created by alexey on 28.09.14.
*/
public class GridEmbeddEntry extends GridEntryBase {
    private final FrameLayout mImageLayout;
    private final ImageView mImageView;
    private final Drawable mImagePlaceholderDrawable;
    private final Drawable mEmbeddForegroundDrawable;
    private final EllipsizingTextView mTitle;

    private final Picasso mPicasso;
    private ImageLoadingGetter mImageGetter;

    public GridEmbeddEntry(Context context, View v, int cardWidth) {
        super(context, v, cardWidth);
        mImageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
        mImageView = (ImageView) mImageLayout.findViewById(R.id.image);
        mTitle = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);

        mPicasso = Picasso.with(context);
        Resources resources = context.getResources();
        mImagePlaceholderDrawable = new ColorDrawable(resources.getColor(R.color.grid_item_image_loading_color));
        mEmbeddForegroundDrawable = resources.getDrawable(R.drawable.embedd_play_foreground);

        mTitle.setMaxLines(2);
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
        Link imageLink;
        int imgViewHeight;

        if (mCardWidth == 0) {
            imageLink = item.getIframely().getImageLink();
        } else {
            imageLink = item.getIframely().getImageLink(mCardWidth);
        }
        if (imageLink == null) {
            mImageLayout.setVisibility(View.VISIBLE);
            mImageLayout.setForeground(mEmbeddForegroundDrawable);
            return;
        }

        imgSize = new ImageSize(imageLink.media.width, imageLink.media.height);
        imgSize.shrinkToWidth(mCardWidth);
        imgSize.shrinkToMaxTextureSize();

        if (imgSize.width < imageLink.media.width) {
            // Изображение было уменьшено под размеры imageView
            imgViewHeight = (int)Math.ceil(imgSize.height);
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(mCardWidth);
            imgSize.cropToMaxTextureSize();
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        mImageView.setAdjustViewBounds(true); // Instagram часто возвращает кривые размеры. Пусть мерцает.
        mImageLayout.setVisibility(View.VISIBLE);
        mImageLayout.setForeground(mEmbeddForegroundDrawable);

        String url = imageLink.getHref();

        mImagePlaceholderDrawable.setBounds(0, 0, mCardWidth, imgViewHeight);
        mImageView.setImageDrawable(mImagePlaceholderDrawable);
        mImageView.requestLayout();

        mPicasso
                .load(url)
                .placeholder(mImagePlaceholderDrawable)
                .error(R.drawable.image_load_error)
                .into(mImageView);
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
}
