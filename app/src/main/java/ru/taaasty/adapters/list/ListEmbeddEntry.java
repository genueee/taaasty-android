package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;

import java.util.ArrayList;

import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.iframely.Link;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.ExtendedImageView;

/**
 * Created by alexey on 28.09.14.
 */
public class ListEmbeddEntry extends ListEntryBase implements Callback {
    private final ExtendedImageView mImageView;
    private final Drawable mImagePlaceholderDrawable;
    private final Drawable mEmbeddForegroundDrawable;
    private final TextView mTitle;

    private final Context mContext;
    private ImageLoadingGetter mImageGetter;

    private TextViewImgLoader mTitleImgLoader;

    private User mUser;

    public ListEmbeddEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mImageView = (ExtendedImageView) v.findViewById(R.id.image);
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);

        mTitle.setMovementMethod(LinkMovementMethodNoSelection.getInstance());
        if (Build.VERSION.SDK_INT <= 16) {
            // Оно там глючное, текст в списке съезжает вправо иногда
            mTitle.setTextIsSelectable(false);
        }

        mContext = context;
        Resources resources = context.getResources();
        mImagePlaceholderDrawable = new ColorDrawable(resources.getColor(R.color.grid_item_image_loading_color));
        mEmbeddForegroundDrawable = resources.getDrawable(R.drawable.embedd_play_foreground);
        mImageView.setForeground(null);
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design) {
        super.setupEntry(entry, design);
        mUser = entry.getAuthor();
        setupImage(entry, mParentWidth);
        setupTitle(entry, mParentWidth);
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

    @Override
    public void recycle() {
        picasso.cancelRequest(mImageView);
        if (mTitleImgLoader != null) mTitleImgLoader.reset();
    }

    public View getImageView() {
        return mImageView;
    }

    private void setupImage(Entry item, final int parentWidth) {
        ImageSize imgSize;
        Link imageLink;
        final int imgViewHeight;

        picasso.cancelRequest(mImageView);

        if (parentWidth == 0) {
            imageLink = item.getIframely().getImageLink();
        } else {
            imageLink = item.getIframely().getImageLink(parentWidth);
        }
        if (imageLink == null) {
            mImageView.setForeground(mEmbeddForegroundDrawable);
            mImageView.setVisibility(View.VISIBLE);
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

        mImageView.setAdjustViewBounds(true); // Instagram часто возвращает кривые размеры. Пусть мерцает.
        mImageView.setVisibility(View.VISIBLE);
        if (item.getIframely().isContentLooksLikeImage()) {
            mImageView.setForeground(null);
        } else {
            mImageView.setForeground(mEmbeddForegroundDrawable);
        }

        final String url = imageLink.getHref();

        mImagePlaceholderDrawable.setBounds(0, 0, parentWidth, imgViewHeight);
        mImageView.setImageDrawable(mImagePlaceholderDrawable);
        mImageView.requestLayout();
        picasso
                .load(url)
                .placeholder(mImagePlaceholderDrawable)
                .error(R.drawable.image_load_error)
                .noFade()
                .into(mImageView, this);
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

        CharSequence title = UiUtils.formatEntryText(item.getTitle(), mImageGetter);
        mTitle.setText(title, TextView.BufferType.NORMAL);
        mTitleImgLoader = TextViewImgLoader.bindAndLoadImages(mTitle, onImgClickListener);
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
