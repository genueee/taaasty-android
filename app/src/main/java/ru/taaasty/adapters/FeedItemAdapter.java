package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.model.iframely.Link;
import ru.taaasty.ui.post.ShowPostFragment;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageSize;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.widgets.EllipsizingTextView;
import ru.taaasty.widgets.EntryBottomActionBar;

public class FeedItemAdapter extends BaseAdapter {

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
    private final ImageUtils mImageUtils;
    private final OnItemListener mListener;

    private boolean mShowUserAvatar = true;

    private final Set<Long> mUpdateRatingEntrySet;

    /**
     * Расстояние между картинкой и текстом
     */
    private final int mImageAndTextSpacing;


    public FeedItemAdapter(Context context, OnItemListener mListener) {
        super();
        mFeed = new ArrayList<Entry>();
        mInfater = LayoutInflater.from(context);
        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        mFeedDesign = TlogDesign.DUMMY;
        mResources = context.getResources();
        mFontManager = FontManager.getInstance(context);
        mImageUtils = ImageUtils.getInstance();
        mUpdateRatingEntrySet = new HashSet<>();
        this.mListener = mListener;
        mImageAndTextSpacing = mResources.getDimensionPixelSize(R.dimen.feed_item_padding_image_text);
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

    public void setShowUserAvatar(boolean show) {
        if (show != mShowUserAvatar) {
            mShowUserAvatar = show;
            notifyDataSetChanged();
        }
    }

    public void onUpdateRatingStart(long entryId) {
        if (mUpdateRatingEntrySet.contains(entryId)) {
            if (DBG) {
                throw new IllegalStateException();
            } else {
                return;
            }
        }
        mUpdateRatingEntrySet.add(entryId);
        notifyDataSetChanged();
    }

    public void onUpdateRatingEnd(long entryId) {
        mUpdateRatingEntrySet.remove(entryId);
        notifyDataSetChanged();
    }

    public boolean isRatingInUpdate(long entryId) {
        return mUpdateRatingEntrySet.contains(entryId);
    }

    public void updateEntry(Entry entry) {
        int size = mFeed.size();
        for (int i=0; i < size; ++i) {
            if (mFeed.get(i).getId() == entry.getId()) {
                mFeed.set(i, entry);
                notifyDataSetChanged();
                break;
            }
        }
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
        vh.text.setTypeface(tf);
        vh.author.setTextColor(textColor);
        vh.title.setTextColor(textColor);
        vh.title.setTypeface(tf);
        vh.source.setTypeface(tf);
        vh.source.setTextColor(textColor);
        vh.entryActionBar.setTlogDesign(mFeedDesign);

        // XXX: ставим только чтобы нормально перекрывать параллаксвый хидер
        // vh.root.setBackgroundColor(mFeedDesign.getFeedBackgroundColor(mResources));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.feed_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.feed_item_view_holder, vh);
            res.setOnClickListener(mOnFeedItemClickListener);
            vh.entryActionBar.setOnItemClickListener(mListener);
            vh.title.setMaxLines(2);
            vh.text.setMaxLines(10);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.feed_item_view_holder);
        }
        applyFeedStyle(vh);
        Entry item = mFeed.get(position);
        adjustMargins(vh, item);
        setAuthor(vh, item);
        setImage(vh, item, parent);
        setText(vh, item);
        vh.entryActionBar.setOnItemListenerEntry(item);
        vh.entryActionBar.setupEntry(item);

        res.setTag(R.id.feed_item_post, item);
        // XXX: more button

        return res;
    }

    private void adjustMargins(ViewHolder vh, Entry item) {
        boolean hasImage = item.isVideo() || !item.getImages().isEmpty();

        // Отступ между картинкой и текстом. Добавляем под картинкой.
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) vh.imageLayout.getLayoutParams();
        if (hasImage && !item.hasNoAnyText()) {
            lp.bottomMargin = mImageAndTextSpacing;
        } else {
            lp.bottomMargin = 0;
        }
        vh.imageLayout.setLayoutParams(lp);
    }

    private void setAuthor(ViewHolder vh, Entry item) {
        if (mShowUserAvatar) {
            User author = item.getAuthor();
            vh.author.setText(author.getSlug());
            mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);
        } else {
            vh.avarar_author.setVisibility(View.GONE);
        }
    }

    private void setImage(ViewHolder vh, Entry item, ViewGroup parent) {
        if (item.isVideo()) {
            setVideoPostImage(vh, item, parent);
        } else {
            setImagePostImage(vh, item, parent);
        }
    }

    private int getImageViewWith(View parent) {
        if (parent instanceof StaggeredGridView) {
            StaggeredGridView sgv = (StaggeredGridView) parent;
            return sgv.getColumnWidth();
        } else {
            return parent.getWidth();
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

        vh.mImageUrl = imageLink.getHref();

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(R.drawable.image_loading_drawable)
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

        ViewGroup.LayoutParams lp = vh.image.getLayoutParams();
        lp.height = imgViewHeight;
        vh.image.setLayoutParams(lp);
        vh.image.setAdjustViewBounds(false); // Иначе мерцает
        vh.imageLayout.setForeground(null);
        vh.imageLayout.setVisibility(View.VISIBLE);

        // XXX: У некоторых картинок может не быть image.image.path
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);
        b.filter(ThumborUrlBuilder.quality(60));
        if (resizeToWidth != 0) b.resize(resizeToWidth, 0);
        vh.mImageUrl = b.toUrl();

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(R.drawable.image_loading_drawable)
                .error(R.drawable.image_loading_drawable)
                .into(vh.image);

    }

    private void setText(ViewHolder vh, Entry item) {
        ShowPostFragment.setupPostText(item, vh.title, vh.text, vh.source, mResources);
    }

    private final View.OnClickListener mOnFeedItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Entry entry = (Entry)v.getTag(R.id.feed_item_post);
            if (mListener != null) mListener.onFeedItemClicked(v, entry.getId());
        }
    };

    public class ViewHolder {
        private final View root;
        private final ViewGroup avarar_author;
        public final ImageView avatar;
        public final TextView author;
        public final FrameLayout imageLayout;
        public Drawable embeddForegroundDrawable;
        public final ImageView image;
        public final EllipsizingTextView title;
        public final EllipsizingTextView text;
        public final TextView source;
        public final EntryBottomActionBar entryActionBar;

        private String mImageUrl = null;

        public ViewHolder(View v) {
            root = v;
            avarar_author = (ViewGroup) v.findViewById(R.id.avatar_author);
            avatar = (ImageView) avarar_author.findViewById(R.id.avatar);
            author = (TextView) avarar_author.findViewById(R.id.author);
            imageLayout = (FrameLayout)v.findViewById(R.id.image_layout);
            image = (ImageView) imageLayout.findViewById(R.id.image);
            title = (EllipsizingTextView) v.findViewById(R.id.feed_item_title);
            text = (EllipsizingTextView) v.findViewById(R.id.feed_item_text);
            source = (TextView) v.findViewById(R.id.source);
            entryActionBar = new EntryBottomActionBar(v.findViewById(R.id.entry_bottom_action_bar), true);
        }
    }

    public interface OnItemListener extends EntryBottomActionBar.OnEntryActionBarListener {
        public void onFeedItemClicked(View view, long postId);
    }
}
