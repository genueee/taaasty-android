package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
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
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

/**
 * Created by alexey on 28.09.14.
 */
public class ListEmbeddEntry extends ListEntryBase {
    private final FrameLayout mImageLayout;
    private final ImageView mImageView;
    private final Drawable mImagePlaceholderDrawable;
    private final Drawable mEmbeddForegroundDrawable;
    private final EllipsizingTextView mTitle;

    private final Context mContext;
    private final Picasso mPicasso;
    private ImageLoadingGetter mImageGetter;

    public ListEmbeddEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mImageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
        mImageView = (ImageView) mImageLayout.findViewById(R.id.image);
        mTitle = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);

        mContext = context;
        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        Resources resources = context.getResources();
        mImagePlaceholderDrawable = new ColorDrawable(resources.getColor(R.color.grid_item_image_loading_color));
        mEmbeddForegroundDrawable = resources.getDrawable(R.drawable.embedd_play_foreground);

        mTitle.setMaxLines(2);
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design, int parentWidth) {
        super.setupEntry(entry, design, parentWidth);
        setupImage(entry, parentWidth);
        setupTitle(entry, parentWidth);
        applyFeedStyle(design);
    }

    @Override
    public void applyFeedStyle(TlogDesign design) {
        super.applyFeedStyle(design);
        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? getFontManager().getPostSerifTypeface() : getFontManager().getPostSansSerifTypeface();
        mTitle.setTextColor(textColor);
        mTitle.setTypeface(tf);
    }

    private void setupImage(Entry item, int parentWidth) {
        ImageSize imgSize;
        Link imageLink;
        int imgViewHeight;

        if (parentWidth == 0) {
            imageLink = item.getIframely().getImageLink();
        } else {
            imageLink = item.getIframely().getImageLink(parentWidth);
        }
        if (imageLink == null) {
            mImageLayout.setVisibility(View.VISIBLE);
            mImageLayout.setForeground(mEmbeddForegroundDrawable);
            return;
        }

        imgSize = new ImageSize(imageLink.media.width, imageLink.media.height);
        imgSize.shrinkToWidth(parentWidth);
        imgSize.shrinkToMaxTextureSize();

        if (imgSize.width < imageLink.media.width) {
            // Изображение было уменьшено под размеры imageView
            imgViewHeight = (int)Math.ceil(imgSize.height);
        } else {
            // Изображение должно быть увеличено под размеры ImageView
            imgSize.stretchToWidth(parentWidth);
            imgSize.cropToMaxTextureSize();
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        mImageView.setMinimumHeight(imgViewHeight);
        mImageView.setAdjustViewBounds(true); // Instagram часто возвращает кривые размеры. Пусть мерцает.
        mImageLayout.setVisibility(View.VISIBLE);
        mImageLayout.setForeground(mEmbeddForegroundDrawable);

        String url = imageLink.getHref();

        mPicasso
                .load(url)
                .placeholder(mImagePlaceholderDrawable)
                .error(R.drawable.image_load_error)
                .into(mImageView);
    }

    private void setupTitle(Entry item, int parentWidth) {
        if (!item.hasTitle()) {
            mTitle.setVisibility(View.GONE);
            return;
        }

        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(
                (parentWidth == 0 ? 0 : parentWidth
                        - getResources().getDimensionPixelSize(R.dimen.feed_item_padding_left)
                        - getResources().getDimensionPixelSize(R.dimen.feed_item_padding_left)),
                mContext);

        CharSequence title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(item.getTitle(), null, null));

        mTitle.setText(Html.fromHtml(title.toString(), mImageGetter, null), TextView.BufferType.NORMAL);
        TextViewImgLoader.bindAndLoadImages(mTitle);
        mTitle.setVisibility(View.VISIBLE);
    }
}
