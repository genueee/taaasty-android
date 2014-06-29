package ru.taaasty.adapters;

import android.content.Context;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.model.FeedItem;


public class FeedItemAdapter extends BaseAdapter {

    private final List<FeedItem> mFeed;
    private final LayoutInflater mInfater;

    public FeedItemAdapter(Context context) {
        super();
        mFeed = new ArrayList<FeedItem>();
        mInfater = LayoutInflater.from(context);
    }

    public void setFeed(List<FeedItem> feed) {
        mFeed.clear();
        mFeed.addAll(feed);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mFeed.size();
    }

    @Override
    public FeedItem getItem(int position) {
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res  = mInfater.inflate(R.layout.feed_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.feed_item_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder)res.getTag(R.id.feed_item_view_holder);
        }
        FeedItem item = mFeed.get(position);
        setAuthor(vh, item);
        setImage(vh, item);
        setTitle(vh, item);
        setText(vh, item);
        setRating(vh, item);
        setComments(vh, item);

        // XXX: more button
        // XX: тыкабельные кнопки

        return res;
    }

    private void setAuthor(ViewHolder vh, FeedItem item) {
        FeedItem.Author author = item.getAuthor();
        vh.author.setText(author.getSlug());
        // XXX: avatar
    }

    private void setImage(ViewHolder vh, FeedItem item) {
        // XXX: image
        vh.image.setVisibility(View.GONE);
    }

    private void setTitle(ViewHolder vh, FeedItem item) {
        String title = item.getTitle();
        if (TextUtils.isEmpty(title)) {
            vh.title.setVisibility(View.GONE);
        } else {
            vh.title.setText(title);
            vh.title.setVisibility(View.VISIBLE);
        }
    }

    private void setText(ViewHolder vh, FeedItem item) {
        Spanned text = item.getTextSpanned();
        Spanned source = item.getSourceSpanned();

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

    private void setRating(ViewHolder vh, FeedItem item) {
        FeedItem.Rating r =  item.getRating();
        if (r.votes > 0) {
            vh.likes.setText(String.valueOf(r.votes));
            vh.likes.setTextColor(vh.likes.getResources().getColor(R.color.text_color_feed_item_likes_gt1));
            vh.likes.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_gt0_likes, 0, 0, 0);
            vh.likes.setBackgroundResource(R.drawable.feed_item_likes_border_gt0);
        } else {
            vh.likes.setText("0");
            vh.likes.setTextColor(vh.likes.getResources().getColor(R.color.text_color_feed_item_gray));
            vh.likes.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_no_likes_light, 0, 0, 0);
            vh.likes.setBackgroundResource(R.drawable.feed_item_likes_border);
        }
    }

    private void setComments(ViewHolder vh, FeedItem item) {
        int comments = item.getCommentsCount();
        vh.comments.setText(String.valueOf(comments));
    }

    public static class ViewHolder {
        public final TextView author;
        public final ImageView image;
        public final TextView title;
        public final TextView text;
        public final TextView likes;
        public final TextView comments;
        public final TextView source;
        public final ImageView moreButton;

        public ViewHolder(View v) {
            author = (TextView)v.findViewById(R.id.author);
            image = (ImageView)v.findViewById(R.id.image);
            title = (TextView)v.findViewById(R.id.title);
            text  = (TextView)v.findViewById(R.id.text);
            comments  = (TextView)v.findViewById(R.id.comments_count);
            likes = (TextView)v.findViewById(R.id.likes);
            source = (TextView)v.findViewById(R.id.source);
            moreButton  = (ImageView)v.findViewById(R.id.more);
        }
    }
}
