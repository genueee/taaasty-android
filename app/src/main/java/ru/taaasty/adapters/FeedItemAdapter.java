package ru.taaasty.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.etsy.android.grid.StaggeredGridView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.list.ListEmbeddEntry;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListImageEntry;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.widgets.EntryBottomActionBar;

public class FeedItemAdapter extends BaseAdapter {

    private final Context mContext;
    private final List<Entry> mFeed;
    private final LayoutInflater mInfater;

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    private static final int VIEW_TYPE_IMAGE = 0;
    private static final int VIEW_TYPE_EMBEDD = 1;
    private static final int VIEW_TYPE_QUOTE = 2;
    private static final int VIEW_TYPE_OTHER = 3;

    private TlogDesign mFeedDesign;
    private final OnItemListener mListener;

    private final boolean mShowUserAvatar;

    private final Set<Long> mUpdateRatingEntrySet;

    private boolean mAuthorAvatarClick = false;


    public FeedItemAdapter(Context context, OnItemListener listener, boolean showUserAvatar, boolean authorAvatarClick) {
        super();
        mFeed = new ArrayList<>();
        mContext = context;
        mInfater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mUpdateRatingEntrySet = new HashSet<>();
        mListener = listener;
        mShowUserAvatar = showUserAvatar;
        mAuthorAvatarClick = authorAvatarClick;
    }

    public void setFeed(List<Entry> feed) {
        mFeed.clear();
        appendFeed(feed);
    }


    public void appendFeed(List<Entry> feed) {
        mFeed.addAll(feed);
        notifyDataSetChanged();
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyDataSetChanged();
    }

    public List<Entry> getFeed() {
        return Collections.unmodifiableList(mFeed);
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

        int parentWidth = getImageViewWith(parent);
        Entry item = mFeed.get(position);
        ListEntryBase holder;

        if (convertView == null) {
            if (item.isImage()) {
                res = mInfater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListImageEntry(mContext, res, mShowUserAvatar);
            } else if (item.isEmbedd()) {
                res = mInfater.inflate(R.layout.list_feed_item_image, parent, false);
                holder = new ListEmbeddEntry(mContext, res, mShowUserAvatar);
            } else if (item.isQuote()) {
                res = mInfater.inflate(R.layout.list_feed_item_quote, parent, false);
                holder = new ListQuoteEntry(mContext, res, mShowUserAvatar);
            } else {
                res = mInfater.inflate(R.layout.list_feed_item_text, parent, false);
                holder = new ListTextEntry(mContext, res, mShowUserAvatar);
            }
            res.setTag(R.id.feed_item_view_holder, holder);
            res.setOnClickListener(mOnFeedItemClickListener);
            if(mAuthorAvatarClick)
                holder.getAvatarAuthorView().setOnClickListener(mOnAuthorItemClickListener);
            holder.getEntryActionBar().setOnItemClickListener(mListener);
        } else {
            res = convertView;
            holder = (ListEntryBase)res.getTag(R.id.feed_item_view_holder);
        }
        holder.setupEntry(item, mFeedDesign, parentWidth);
        holder.getEntryActionBar().setOnItemListenerEntry(item);
        res.setTag(R.id.feed_item_post, item);
        if(mAuthorAvatarClick)
            holder.getAvatarAuthorView().setTag( R.id.feed_item_post, item);
        return res;
    }

    private int getImageViewWith(View parent) {
        if (parent instanceof StaggeredGridView) {
            StaggeredGridView sgv = (StaggeredGridView) parent;
            return sgv.getColumnWidth();
        } else {
            return parent.getWidth();
        }
    }


    private final View.OnClickListener mOnFeedItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Entry entry = (Entry)v.getTag(R.id.feed_item_post);
            if (mListener != null) mListener.onFeedItemClicked(v, entry.getId());
        }
    };

    private final View.OnClickListener mOnAuthorItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Entry entry = (Entry)v.getTag(R.id.feed_item_post);
            if (mListener != null) mListener.onPostUserInfoClicked(v, entry );
        }
    };

    public interface OnItemListener extends EntryBottomActionBar.OnEntryActionBarListener {
        public void onFeedItemClicked(View view, long postId);
    }
}
