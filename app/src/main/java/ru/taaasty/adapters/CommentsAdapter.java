package ru.taaasty.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.taaasty.R;
import ru.taaasty.model.Comment;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;

public class CommentsAdapter extends BaseAdapter {
    private final LayoutInflater mInfater;
    private final Resources mResources;
    private final FontManager mFontManager;
    private final ImageUtils mImageUtils;

    private final ArrayList<Comment> mComments;
    private TlogDesign mFeedDesign;

    private DateFormat mTimeFormatInstance;
    private DateFormat mDdMmFormatInstance;
    private DateFormat mMmYyFormatInstance;

    public CommentsAdapter(Context context) {
        super();
        mInfater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mResources = context.getResources();
        mFontManager = FontManager.getInstance(context);
        mImageUtils = ImageUtils.getInstance();
        mComments = new ArrayList<Comment>();
        mTimeFormatInstance = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT, Locale.getDefault());
        mDdMmFormatInstance = new SimpleDateFormat("dd MMM");
        mMmYyFormatInstance = new SimpleDateFormat("LL/yyy");
    }

    public void setComments(List<Comment> comments) {
        mComments.clear();
        appendComments(comments);
    }

    public ArrayList<Comment> getComments() {
        return mComments;
    }

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
        notifyDataSetChanged();
    }

    public void appendComments(List<Comment> comments) {
        // XXX: sort, remove duplicates
        mComments.addAll(comments);
        notifyDataSetChanged();
    }

    public Long getTopCommentId() {
        if (mComments.isEmpty()) return null;
        return mComments.get(0).getId();
    }

    @Override
    public int getCount() {
        return mComments.size();
    }

    @Override
    public Comment getItem(int position) {
        return mComments.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mComments.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        View res;

        if (convertView == null) {
            res = mInfater.inflate(R.layout.comments_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.comment_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.comment_view_holder);
        }

        applyFeedStyle(vh);

        Comment comment = mComments.get(position);
        setAuthor(vh, comment);
        setComment(vh, comment);
        setDate(vh, comment);

        return res;
    }

    private void applyFeedStyle(ViewHolder vh) {
        int textColor = mFeedDesign.getFeedTextColor(mResources);
        Typeface tf = mFeedDesign.isFontTypefaceSerif() ? mFontManager.getDefaultSerifTypeface() : mFontManager.getDefaultSansSerifTypeface();

        vh.comment.setTextColor(textColor);
        vh.comment.setTypeface(tf);
    }

    private void setAuthor(ViewHolder vh, Comment item) {
        User author = item.getAuthor();
        mImageUtils.loadAvatar(author.getUserpic(), author.getName(), vh.avatar, R.dimen.avatar_small_diameter);
    }

    private void setComment(ViewHolder vh, Comment item) {
        vh.comment.setText(item.getTextSpanned());
    }

    private void setDate(ViewHolder vh, Comment item) {
        Date updatedAt = item.getUpdatedAt();
        long timediff = Math.abs(System.currentTimeMillis() - updatedAt.getTime());
        String date;
        if (timediff < 24 * 60 * 60 * 1000l) {
            date = mTimeFormatInstance.format(updatedAt);
        } else {
            Calendar lastYear = Calendar.getInstance();
            lastYear.add(Calendar.YEAR, -1);
            if (updatedAt.after(lastYear.getTime())) {
                date = mDdMmFormatInstance.format(updatedAt);
            } else {
                date = mMmYyFormatInstance.format(updatedAt);
            }
        }
        vh.date.setText(date);
    }

    public class ViewHolder {
        public final ImageView avatar;
        public final TextView comment;
        public final TextView date;
        public ViewHolder(View v) {
            avatar = (ImageView) v.findViewById(R.id.avatar);
            comment = (TextView) v.findViewById(R.id.comment);
            date = (TextView) v.findViewById(R.id.date_relative);
        }
    }
}
