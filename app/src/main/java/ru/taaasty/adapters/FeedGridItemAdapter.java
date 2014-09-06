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
import ru.taaasty.utils.FontManager;
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
        for (Entry i: mFeed) {
            i.getTextSpanned();
            i.getSourceSpanned();
        }
        notifyDataSetChanged();
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
        Typeface tf = mFeedDesign.isFontTypefaceSerif() ? mFontManager.getDefaultSerifTypeface() : mFontManager.getDefaultSansSerifTypeface();

        vh.text.setTextColor(textColor);
        vh.text.setTypeface(tf);
        vh.title.setTextColor(textColor);
        vh.title.setTypeface(tf);
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
        adjustPaddings(vh, item);
        setImage(vh, item, parent);
        setText(vh, item);

        return res;
    }

    private void adjustPaddings(ViewHolder vh, Entry item) {
        if (item.hasNoAnyText()) {
            vh.root.setPadding(vh.root.getPaddingLeft(), 0, vh.root.getPaddingRight(), 0);
        } else {
            int paddingTop  = item.hasImages() ? 0 : mGridItemPaddingTop;
            vh.root.setPadding(vh.root.getPaddingLeft(), paddingTop,
                    vh.root.getPaddingRight(), mGridItemPaddingBottom);
        }
    }

    private void setImage(ViewHolder vh, Entry item, ViewGroup parent) {
        if (item.getImages().isEmpty()) {
            vh.image.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = item.getImages().get(0);
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));

        float dstWidth, dstHeight;
        float imgWidth, imgHeight;

        // XXX: check for 0
        float parentWidth;
        if (parent instanceof StaggeredGridView) {
            StaggeredGridView sgv = (StaggeredGridView) parent;
            parentWidth = sgv.getColumnWidth();
        } else {
            parentWidth = parent.getMeasuredWidth();
        }

        if (parentWidth < image.image.geometry.width) {
            imgWidth = parentWidth;
            imgHeight = (float)image.image.geometry.height * parentWidth / (float)image.image.geometry.width;
            b.resize((int)Math.ceil(imgWidth), 0);
        } else {
            imgWidth = image.image.geometry.width;
            imgHeight = image.image.geometry.height;
        }
        dstWidth = parentWidth;
        dstHeight = imgHeight * (dstWidth / imgWidth);

        vh.mImageUrl = b.toUrl();
        // if (DBG) Log.v(TAG, "setimagesize " + dstWidth + " " + dstHeight);
        vh.image.setMinimumHeight((int)Math.floor(dstHeight));
        vh.image.setVisibility(View.VISIBLE);

        if (vh.imagePlaceholderDrawable == null) {
            vh.imagePlaceholderDrawable = new ColorDrawable(mResources.getColor(R.color.grid_item_image_loading_color));
        }

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(vh.imagePlaceholderDrawable)
                .error(R.drawable.image_loading_drawable)
                .into(vh.image);

    }

    private void setText(ViewHolder vh, Entry item) {

        if (Entry.ENTRY_TYPE_QUOTE.equals(item.getType())) {
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
                CharSequence title = item.getTitle();
                vh.title.setMaxLines(MAX_LINES_TITLE);
                vh.title.setText(Html.fromHtml(title.toString()));
                vh.title.setVisibility(View.VISIBLE);
            } else {
                vh.title.setVisibility(View.GONE);
            }
            if (item.hasText()) {
                CharSequence text = item.getTextSpanned();
                vh.text.setText(text);
                vh.text.setVisibility(View.VISIBLE);
            } else {
                vh.text.setVisibility(View.GONE);
            }
        }
    }

    public class ViewHolder {
        public final View root;
        public final ImageView image;
        public Drawable imagePlaceholderDrawable;
        public final EllipsizingTextView title;
        public final EllipsizingTextView text;
        public final TextView source;

        private String mImageUrl = null;

        public ViewHolder(View v) {
            root = v;
            image = (ImageView) v.findViewById(R.id.image);
            title = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);
            text = (EllipsizingTextView) v.findViewById(R.id.feed_item_text);
            source = (TextView) v.findViewById(R.id.source);
        }
    }
}
