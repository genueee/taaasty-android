package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
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
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;

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

    public FeedItemAdapter(Context context, OnItemListener mListener) {
        super();
        mFeed = new ArrayList<Entry>();
        mInfater = LayoutInflater.from(context);
        mPicasso = NetworkUtils.getInstance().getPicasso(context);
        mFeedDesign = TlogDesign.DUMMY;
        mResources = context.getResources();
        mFontManager = FontManager.getInstance(context);
        mImageUtils = ImageUtils.getInstance();
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
            vh.likes.setOnClickListener(mOnFeedLikesClickListener);
            vh.comments.setOnClickListener(mOnFeedCommentsClickListener);
            vh.moreButton.setOnClickListener(mOnFeedAdditionalMenuClickListener);
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
        setRating(vh, item);
        setComments(vh, item);


        res.setTag(R.id.feed_item_post_id, item.getId());
        vh.likes.setTag(R.id.feed_item_post_id, item.getId());
        vh.comments.setTag(R.id.feed_item_post_id, item.getId());
        vh.moreButton.setTag(R.id.feed_item_post_id, item.getId());

        // XXX: more button
        // XX: тыкабельные кнопки

        return res;
    }

    private void setAuthor(ViewHolder vh, Entry item) {
        Entry.Author author = item.getAuthor();
        vh.author.setText(author.getSlug());
        mImageUtils.loadAvatar(author.getUserpic(), vh.avatar, R.dimen.avatar_small_diameter);
    }

    private void setImage(ViewHolder vh, Entry item, ViewGroup parent) {
        if (item.getImages().isEmpty()) {
            vh.image.setVisibility(View.GONE);
            return;
        }

        Entry.Image image = item.getImages().get(0);
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
            vh.title.setText(title);
            vh.title.setVisibility(View.VISIBLE);
        }
    }

    private void setText(ViewHolder vh, Entry item) {
        CharSequence text = item.getTextSpanned();
        CharSequence source = item.getSourceSpanned();

        // XXX: другой шрифт если есть source
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

    private void setRating(ViewHolder vh, Entry item) {
        Entry.Rating r = item.getRating();
        if (r.votes > 0) {
            vh.likes.setText(String.valueOf(r.votes));
            vh.likes.setTextColor(mResources.getColor(R.color.text_color_feed_item_likes_gt1));
            vh.likes.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gt0_likes, 0, 0, 0);
            vh.likes.setBackgroundResource(R.drawable.feed_item_likes_border_gt0);
        } else {
            vh.likes.setText("0");
            vh.likes.setTextColor(mResources.getColor(R.color.text_color_feed_item_gray));
            vh.likes.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_no_likes_light, 0, 0, 0);
            vh.likes.setBackgroundResource(R.drawable.feed_item_likes_border);
        }
    }

    private void setComments(ViewHolder vh, Entry item) {
        int comments = item.getCommentsCount();
        vh.comments.setText(String.valueOf(comments));
    }

    private final View.OnClickListener mOnFeedItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long postId = (Long)v.getTag(R.id.feed_item_post_id);
            if (mListener != null) mListener.onFeedItemClicked(v, postId);
        }
    };

    private final View.OnClickListener mOnFeedLikesClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long postId = (Long)v.getTag(R.id.feed_item_post_id);
            if (mListener != null) mListener.onFeedLikesClicked(v, postId);
        }
    };

    private final View.OnClickListener mOnFeedAdditionalMenuClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long postId = (Long)v.getTag(R.id.feed_item_post_id);
            if (mListener != null) mListener.onFeedAdditionalMenuClicked(v, postId);
        }
    };

    private final View.OnClickListener mOnFeedCommentsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            long postId = (Long)v.getTag(R.id.feed_item_post_id);
            if (mListener != null) mListener.onFeedCommentsClicked(v, postId);
        }
    };

    public class ViewHolder {
        public final ImageView avatar;
        public final TextView author;
        public final ImageView image;
        public final TextView title;
        public final TextView text;
        public final TextView likes;
        public final TextView comments;
        public final TextView source;
        public final ImageView moreButton;

        private String mImageUrl = null;

        public ViewHolder(View v) {
            avatar = (ImageView) v.findViewById(R.id.avatar);
            author = (TextView) v.findViewById(R.id.author);
            image = (ImageView) v.findViewById(R.id.image);
            title = (TextView) v.findViewById(R.id.title);
            text = (TextView) v.findViewById(R.id.text);
            comments = (TextView) v.findViewById(R.id.comments_count);
            likes = (TextView) v.findViewById(R.id.likes);
            source = (TextView) v.findViewById(R.id.source);
            moreButton = (ImageView) v.findViewById(R.id.more);
        }
    }

    public interface OnItemListener {
        public void onFeedItemClicked(View view, long postId);
        public void onFeedLikesClicked(View view, long postId);
        public void onFeedCommentsClicked(View view, long postId);
        public void onFeedAdditionalMenuClicked(View view, long postId);
    }
}
