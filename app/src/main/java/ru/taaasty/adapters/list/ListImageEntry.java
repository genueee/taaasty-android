package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

public class ListImageEntry extends ListEntryBase {
    private final FrameLayout mImageLayout;
    private final ImageView mImageView;
    private final EllipsizingTextView mTitle;

    private Context mContext;
    private final Picasso mPicasso;
    private ImageLoadingGetter mImageGetter;

    public ListImageEntry(Context context, View v, boolean showAuthorAvatar) {
        super(context, v, showAuthorAvatar);

        mImageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
        mImageView = (ImageView) mImageLayout.findViewById(R.id.image);
        mTitle = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);

        mContext = context;
        mPicasso = NetworkUtils.getInstance().getPicasso(context);

        mTitle.setMaxLines(10);
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
        int resizeToWidth = 0;
        int imgViewHeight;

        if (item.getImages().isEmpty()) {
            mImageLayout.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = item.getImages().get(0);
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
            imgViewHeight = (int)Math.ceil(imgSize.height);
        }

        ViewGroup.LayoutParams lp = mImageView.getLayoutParams();
        lp.height = imgViewHeight;
        mImageView.setLayoutParams(lp);
        mImageView.setAdjustViewBounds(false); // Иначе мерцает
        mImageLayout.setForeground(null);
        mImageLayout.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);

        mPicasso
                .load(b.toUrl())
                .fit()
                .centerInside()
                .placeholder(R.drawable.image_loading_drawable)
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
