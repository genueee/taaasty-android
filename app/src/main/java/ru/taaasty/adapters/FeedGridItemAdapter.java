package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.etsy.android.grid.StaggeredGridView;
import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

public class FeedGridItemAdapter extends BaseAdapter {

    private static final int MAX_LINES_TITLE = 2;
    private static final int MAX_LINES_TEXT = 10;

    private final List<Entry> mFeed;
    private final LayoutInflater mInfater;
    private final Picasso mPicasso;

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    private static final int FEED_STYLE_DARK = 0;
    private static final int FEED_STYLE_LIGHT = 1;

    private TlogDesign mFeedDesign;

    private final Resources mResources;
    private final FontManager mFontManager;

    private final int mGridItemPaddingTop;
    private final int mGridItemPaddingBottom;

    public FeedGridItemAdapter(Context context) {
        super();
        mFeed = new ArrayList<Entry>();
        mInfater = LayoutInflater.from(context);
        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        mFeedDesign = TlogDesign.DUMMY;
        mResources = context.getResources();
        mFontManager = FontManager.getInstance(context);
        mGridItemPaddingTop = mResources.getDimensionPixelSize(R.dimen.feed_grid_item_padding_top);
        mGridItemPaddingBottom = mResources.getDimensionPixelSize(R.dimen.feed_grid_item_padding_bottom);
    }

    public void setFeed(List<Entry> feed) {
        mFeed.clear();
        appendFeed(feed);
    }

    public void appendFeed(List<Entry> feed) {
        mFeed.addAll(feed);
        notifyDataSetChanged();

        // Отдельным потоком выполняем getTextSpanned() (ибо долгий процесс)
        final ArrayList<Entry> feedCopy = new ArrayList<>(mFeed);
        new Thread() {
            @Override
            public void run() {
                for (Entry i: feedCopy) {
                    i.getTextSpanned();
                    i.getSourceSpanned();
                }
            }
        }.start();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyDataSetChanged();
    }

    public List<Entry> getFeed() {
        return Collections.unmodifiableList(mFeed);
    }

    @Override
    public int getCount() {
        return mFeed.size();
    }

    @Override
    public Entry getItem(int position) {
        return mFeed.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mFeed.get(position).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void applyFeedStyle(ViewHolder vh) {
        int textColor = mFeedDesign.getFeedTextColor(mResources);
        Typeface tf = mFeedDesign.isFontTypefaceSerif() ? mFontManager.getPostSerifTypeface() : mFontManager.getPostSansSerifTypeface();

        vh.text.setTextColor(textColor);
        vh.title.setTextColor(textColor);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.live_feed_item, parent, false);
            vh = new ViewHolder(res);
            vh.title.setMaxLines(MAX_LINES_TITLE);
            vh.text.setMaxLines(MAX_LINES_TEXT);
            res.setTag(R.id.feed_item_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.feed_item_view_holder);
        }
        applyFeedStyle(vh);
        Entry item = mFeed.get(position);
        adjustMargins(vh, item);
        setImage(vh, item, parent);
        setText(vh, item);

        return res;
    }

    private void adjustMargins(ViewHolder vh, Entry item) {
        if (item.hasNoAnyText()) {
            vh.root.setPadding(vh.root.getPaddingLeft(), 0, vh.root.getPaddingRight(), 0);
        } else {
            // Для видео поста стараемся показывать хоть какую-нибудь картинку.
            boolean hasImage = item.isVideo() || !item.getImages().isEmpty();
            int paddingTop  = hasImage ? 0 : mGridItemPaddingTop;
            vh.root.setPadding(vh.root.getPaddingLeft(), paddingTop,
                    vh.root.getPaddingRight(), mGridItemPaddingBottom);
        }
    }

    // TODO: практически точная копия из FeedItemAdapter. Избавиться от дублирования
    private void setImage(ViewHolder vh, Entry item, ViewGroup parent) {
        if (item.isVideo()) {
            setVideoPostImage(vh, item, parent);
        } else {
            setImagePostImage(vh, item, parent);
        }
    }

    private void setVideoPostImage(ViewHolder vh, Entry item, ViewGroup parent) {
        ImageSize imgSize;
        Link imageLink;
        int imgViewHeight;

        if (vh.embeddForegroundDrawable == null) {
            vh.embeddForegroundDrawable = mResources.getDrawable(R.drawable.embedd_play_foreground);
        }

        int parentWidth = getImageViewWith(parent);
        if (parentWidth == 0) {
            imageLink = item.getIframely().getImageLink();
        } else {
            imageLink = item.getIframely().getImageLink(parentWidth);
        }
        if (imageLink == null) {
            vh.imageLayout.setVisibility(View.VISIBLE);
            vh.imageLayout.setForeground(vh.embeddForegroundDrawable);
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

        vh.image.setMinimumHeight(imgViewHeight);
        vh.image.setAdjustViewBounds(true); // Instagram часто возвращает кривые размеры. Пусть мерцает.
        vh.imageLayout.setVisibility(View.VISIBLE);
        vh.imageLayout.setForeground(vh.embeddForegroundDrawable);

        if (vh.imagePlaceholderDrawable == null) {
            vh.imagePlaceholderDrawable = new ColorDrawable(mResources.getColor(R.color.grid_item_image_loading_color));
        }

        vh.mImageUrl = imageLink.getHref();

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(vh.imagePlaceholderDrawable)
                .error(R.drawable.image_loading_drawable)
                .into(vh.image);
    }

    private void setImagePostImage(ViewHolder vh, Entry item, ViewGroup parent) {
        ImageSize imgSize;
        int resizeToWidth = 0;
        int imgViewHeight;

        if (item.getImages().isEmpty()) {
            vh.imageLayout.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = item.getImages().get(0);
        // XXX: check for 0
        int parentWidth = getImageViewWith(parent);
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

        vh.image.setMinimumHeight(imgViewHeight);
        vh.image.setAdjustViewBounds(false); // Иначе мерцает
        vh.imageLayout.setForeground(null);
        vh.imageLayout.setVisibility(View.VISIBLE);

        if (vh.imagePlaceholderDrawable == null) {
            vh.imagePlaceholderDrawable = new ColorDrawable(mResources.getColor(R.color.grid_item_image_loading_color));
        }

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);
        vh.mImageUrl = b.toUrl();

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(vh.imagePlaceholderDrawable)
                .error(R.drawable.image_loading_drawable)
                .into(vh.image);
    }

    private int getImageViewWith(View parent) {
        if (parent instanceof StaggeredGridView) {
            StaggeredGridView sgv = (StaggeredGridView) parent;
            return sgv.getColumnWidth();
        } else {
            return parent.getWidth();
        }
    }

    private void setText(ViewHolder vh, Entry item) {

        if (item.isQuote()) {
            // Цитата
            Spanned text = UiUtils.formatQuoteText(item.getText());
            Spanned source = UiUtils.formatQuoteSource(item.getSource());

            vh.text.setVisibility(View.GONE);
            if (text != null) {
                vh.title.setMaxLines(MAX_LINES_TEXT);
                vh.title.setText(text);
                vh.title.setVisibility(View.VISIBLE);
            } else {
                vh.title.setVisibility(View.GONE);
            }

            if (source != null) {
                vh.source.setText(source);
                vh.source.setVisibility(View.VISIBLE);
            } else {
                vh.source.setVisibility(View.GONE);
            }
        } else {
            // Все остальное
            vh.source.setVisibility(View.GONE);
            if (item.hasTitle()) {
                CharSequence title = UiUtils.removeTrailingWhitespaces(item.getTitleSpanned());
                vh.title.setMaxLines(MAX_LINES_TITLE);
                vh.title.setText(Html.fromHtml(title.toString()));
                vh.title.setVisibility(View.VISIBLE);
            } else {
                vh.title.setVisibility(View.GONE);
            }
            if (item.hasText()) {
                CharSequence text = UiUtils.removeTrailingWhitespaces(item.getTextSpanned());
                vh.text.setText(text);
                vh.text.setVisibility(View.VISIBLE);
            } else {
                vh.text.setVisibility(View.GONE);
            }
        }
    }

    public class ViewHolder {
        public final View root;
        public final FrameLayout imageLayout;
        public final ImageView image;
        public Drawable imagePlaceholderDrawable;
        public Drawable embeddForegroundDrawable;
        public final EllipsizingTextView title;
        public final EllipsizingTextView text;
        public final TextView source;

        private String mImageUrl = null;

        public ViewHolder(View v) {
            root = v;
            imageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
            image = (ImageView) imageLayout.findViewById(R.id.image);
            title = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);
            text = (EllipsizingTextView) v.findViewById(R.id.feed_item_text);
            source = (TextView) v.findViewById(R.id.source);
        }
    }
}
