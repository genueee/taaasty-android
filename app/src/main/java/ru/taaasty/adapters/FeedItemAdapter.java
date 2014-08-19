package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

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
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.EllipsizingTextView;

public class FeedItemAdapter extends BaseAdapter implements IFeedItemAdapter {

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
        Typeface tf = mFeedDesign.isFontTypefaceSerif() ? mFontManager.getDefaultSerifTypeface() : mFontManager.getDefaultSansSerifTypeface();

        vh.text.setTextColor(textColor);
        vh.text.setTypeface(tf);
        vh.author.setTextColor(textColor);
        vh.author.setTypeface(tf);
        vh.title.setTextColor(textColor);
        vh.title.setTypeface(tf);
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
        setAuthor(vh, item);
        setImage(vh, item, parent);
        setTitle(vh, item);
        setText(vh, item);
        vh.entryActionBar.setOnItemListenerEntry(item);
        vh.entryActionBar.setupEntry(item);

        res.setTag(R.id.feed_item_post, item);
        // XXX: more button

        return res;
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
        if (item.getImages().isEmpty()) {
            vh.image.setVisibility(View.GONE);
            return;
        }

        ImageInfo image = item.getImages().get(0);
        ThumborUrlBuilder b = NetworkUtils.createThumborUrlFromPath(image.image.path);

        float dstWidth, dstHeight;
        float imgWidth, imgHeight;

        // XXX: check for 0
        float parentWidth = parent.getMeasuredWidth();
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
        if (DBG) Log.v(TAG, "setimagesize " + dstWidth + " " + dstHeight);
        ViewGroup.LayoutParams lp = vh.image.getLayoutParams();
        lp.height = (int)Math.ceil(dstHeight);
        vh.image.setLayoutParams(lp);
        vh.image.setVisibility(View.VISIBLE);

        mPicasso
                .load(vh.mImageUrl)
                .placeholder(R.drawable.image_loading_drawable)
                .error(R.drawable.image_loading_drawable)
                .noFade()
                .into(vh.image);

    }

    private void setTitle(ViewHolder vh, Entry item) {
        String title = item.getTitle();
        if (TextUtils.isEmpty(title)) {
            vh.title.setVisibility(View.GONE);
        } else {
            vh.title.setText(Html.fromHtml(title));
            vh.title.setVisibility(View.VISIBLE);
        }
    }

    private void setText(ViewHolder vh, Entry item) {
        CharSequence text;
        CharSequence source;

        if (Entry.ENTRY_TYPE_QUOTE.equals(item.getType())) {
            text = UiUtils.formatQuoteText(item.getText());
            source = UiUtils.formatQuoteSource(item.getSource());
        } else {
            text = item.getTextSpanned();
            source = null;
        }

        if (text == null) {
            vh.text.setVisibility(View.GONE);
        } else {
            vh.text.setText(text);
            vh.text.setVisibility(View.VISIBLE);
        }

        if (source == null) {
            vh.source.setVisibility(View.GONE);
        } else {
            vh.source.setText(source);
            vh.source.setVisibility(View.VISIBLE);
        }
    }

    private final View.OnClickListener mOnFeedItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Entry entry = (Entry)v.getTag(R.id.feed_item_post);
            if (mListener != null) mListener.onFeedItemClicked(v, entry.getId());
        }
    };

    public class ViewHolder {
        private final ViewGroup avarar_author;
        public final ImageView avatar;
        public final TextView author;
        public final ImageView image;
        public final EllipsizingTextView title;
        public final EllipsizingTextView text;
        public final TextView source;
        public final EntryBottomActionBar entryActionBar;

        private String mImageUrl = null;

        public ViewHolder(View v) {
            avarar_author = (ViewGroup) v.findViewById(R.id.avatar_author);
            avatar = (ImageView) avarar_author.findViewById(R.id.avatar);
            author = (TextView) avarar_author.findViewById(R.id.author);
            image = (ImageView) v.findViewById(R.id.image);
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
