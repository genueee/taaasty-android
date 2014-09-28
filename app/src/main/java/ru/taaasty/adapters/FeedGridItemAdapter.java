package ru.taaasty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.etsy.android.grid.StaggeredGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.grid.GridEmbeddEntry;
import ru.taaasty.adapters.grid.GridImageEntry;
import ru.taaasty.adapters.grid.GridQuoteEntry;
import ru.taaasty.adapters.grid.GridTextEntry;
import ru.taaasty.model.Entry;

public class FeedGridItemAdapter extends BaseAdapter {

    private static final int MAX_LINES_TITLE = 2;
    private static final int MAX_LINES_TEXT = 10;

    private final List<Entry> mFeed;
    private final LayoutInflater mInfater;
    private final Context mContext;

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    private static final int FEED_STYLE_DARK = 0;
    private static final int FEED_STYLE_LIGHT = 1;

    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_EMBEDD = 1;
    private static final int VIEW_TYPE_QUOTE = 2;
    private static final int VIEW_TYPE_OTHER = 3;

    public FeedGridItemAdapter(Context context) {
        super();
        mContext = context;
        mFeed = new ArrayList<Entry>();
        mInfater = LayoutInflater.from(context);
    }

    public void setFeed(List<Entry> feed) {
        mFeed.clear();
        appendFeed(feed);
    }

    public void appendFeed(List<Entry> feed) {
        mFeed.addAll(feed);
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

    @Override
    public int getItemViewType(int position) {
        Entry item = getItem(position);
        if (item == null) return VIEW_TYPE_OTHER;
        if (item.isImage()) {
            return VIEW_TYPE_IMAGE;
        } else if (item.isEmbedd()) {
            return VIEW_TYPE_EMBEDD;
        } else if (item.isQuote()) {
            return VIEW_TYPE_QUOTE;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_OTHER + 1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View res;
        Entry item = mFeed.get(position);
        if (item.isImage()) {
            GridImageEntry holder;
            if (convertView == null) {
                res = mInfater.inflate(R.layout.grid_feed_item_image, parent, false);
                holder = new GridImageEntry(mContext, res);
                res.setTag(R.id.feed_item_view_holder, holder);
            } else {
                res = convertView;
                holder = (GridImageEntry) res.getTag(R.id.feed_item_view_holder);
            }
            int parentWidth = getImageViewWith(parent);
            holder.setupEntry(item, parentWidth);
            return res;
        } else if (item.isEmbedd()) {
            GridEmbeddEntry holder;
            if (convertView == null) {
                res = mInfater.inflate(R.layout.grid_feed_item_image, parent, false);
                holder = new GridEmbeddEntry(mContext, res);
                res.setTag(R.id.feed_item_view_holder, holder);
            } else {
                res = convertView;
                holder = (GridEmbeddEntry) res.getTag(R.id.feed_item_view_holder);
            }
            int parentWidth = getImageViewWith(parent);
            holder.setupEntry(item, parentWidth);
            return res;
        } else if (item.isQuote()) {
            GridQuoteEntry holder;
            if (convertView == null) {
                res = mInfater.inflate(R.layout.grid_feed_item_quote, parent, false);
                holder = new GridQuoteEntry(res);
                res.setTag(R.id.feed_item_view_holder, holder);
            } else {
                res = convertView;
                holder = (GridQuoteEntry) res.getTag(R.id.feed_item_view_holder);
            }
            holder.setupEntry(item);
            return res;
        } else {
            GridTextEntry holder;
            if (convertView == null) {
                res = mInfater.inflate(R.layout.grid_feed_item_text, parent, false);
                holder = new GridTextEntry(mContext, res);
                res.setTag(R.id.feed_item_view_holder, holder);
            } else {
                res = convertView;
                holder = (GridTextEntry) res.getTag(R.id.feed_item_view_holder);
            }
            int parentWidth = getImageViewWith(parent);
            holder.setupEntry(item, parentWidth);
            return res;
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
}
