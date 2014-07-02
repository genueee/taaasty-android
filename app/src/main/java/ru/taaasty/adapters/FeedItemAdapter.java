package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.FeedItem;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.NetworkUtils;


public class FeedItemAdapter extends BaseAdapter {

    private final List<FeedItem> mFeed;
    private final LayoutInflater mInfater;
    private final Picasso mPicasso;

    private final Map<String, ImageWh> mImageSizes;

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FeedItemAdapter";

    private final int mAvatarDiameter;
    private final CircleTransformation mCircleTransformation;

    public FeedItemAdapter(Context context) {
        super();
        mFeed = new ArrayList<FeedItem>();
        mInfater = LayoutInflater.from(context);
        mPicasso = Picasso.with(context);
        mAvatarDiameter = context.getResources().getDimensionPixelSize(R.dimen.avatar_small_diameter);
        mCircleTransformation = new CircleTransformation();
        mImageSizes = new HashMap<String, ImageWh>();
    }

    public void setFeed(List<FeedItem> feed) {
        mFeed.clear();
        mFeed.addAll(feed);
        for (FeedItem i: mFeed) {
            i.getTextSpanned();
            i.getSourceSpanned();
        }
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
            res = mInfater.inflate(R.layout.feed_item, parent, false);
            vh = new ViewHolder(res);
            res.setTag(R.id.feed_item_view_holder, vh);
        } else {
            res = convertView;
            vh = (ViewHolder) res.getTag(R.id.feed_item_view_holder);
        }
        FeedItem item = mFeed.get(position);
        setAuthor(vh, item);
        setImage(vh, item, parent);
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

        String userpicUrl = author.getUserpic().largeUrl;
        if (TextUtils.isEmpty(userpicUrl)) {
            vh.avatar.setImageResource(R.drawable.avatar_dummy);
        } else {
            ThumborUrlBuilder b = NetworkUtils.createThumborUrl(userpicUrl);
            if (b != null) {
                userpicUrl = b.resize(mAvatarDiameter, mAvatarDiameter)
                        //.filter(ThumborUrlBuilder.format(ThumborUrlBuilder.ImageFormat.WEBP))
                        .smart()
                        .toUrl();
                // if (DBG) Log.d(TAG, "userpicUrl: " + userpicUrl);
                mPicasso.load(userpicUrl)
                        .placeholder(R.drawable.avatar_dummy)
                        .error(R.drawable.avatar_dummy)
                        .transform(mCircleTransformation)
                        .noFade()
                        .into(vh.avatar);
            } else {
                mPicasso.load(userpicUrl)
                        .resize(mAvatarDiameter, mAvatarDiameter)
                        .centerCrop()
                        .placeholder(R.drawable.avatar_dummy)
                        .error(R.drawable.avatar_dummy)
                        .transform(mCircleTransformation)
                        .noFade()
                        .into(vh.avatar);
            }
        }
    }

    private void setImage(ViewHolder vh, FeedItem item, ViewGroup parent) {
        mPicasso.cancelRequest(vh.imageTarget);
        if (item.getImages().size() > 0) {
            String imageUrl = item.getImages().get(0).mediumUrl;

            /*
            if (DBG) Log.v(TAG, "width: " + vh.image.getMeasuredWidth()
                            + "parent width: " + parent.getMeasuredWidth()
            );
            */

            if (parent.getMeasuredWidth() > 0) {
                ThumborUrlBuilder b = NetworkUtils.createThumborUrl(imageUrl);
                if (b != null) {
                    b.resize(parent.getMeasuredWidth(), 0);
                    /*
                    if (imageUrl.endsWith(".jpg") || imageUrl.endsWith(".Jpg") || imageUrl.endsWith(".JPG")) {
                        b.filter(ThumborUrlBuilder.format(ThumborUrlBuilder.ImageFormat.WEBP));
                    }
                    */
                    imageUrl =  b.toUrl();
                }
            }
            // if (DBG) Log.v(TAG, "image url: " + imageUrl);
            vh.mImageUrl = imageUrl;
            ImageWh wh = mImageSizes.get(imageUrl);
            if (wh != null) {
                if (DBG) Log.v(TAG, "setimagesize " + wh.width + " " + wh.height);
                Bitmap b = Bitmap.createBitmap(wh.width, wh.height, Bitmap.Config.ARGB_8888);
                b.eraseColor(Color.TRANSPARENT);
                vh.image.setImageBitmap(b);
                vh.image.setVisibility(View.VISIBLE);
            } else {
                vh.image.setVisibility(View.GONE);
            }
            mPicasso
                    .load(imageUrl)
                    .noFade()
                    .into(vh.imageTarget);
        } else {
            vh.image.setVisibility(View.GONE);
        }
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

    private void setRating(ViewHolder vh, FeedItem item) {
        FeedItem.Rating r = item.getRating();
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

    public static class ImageWh {
        final int width;
        final int height;
        public ImageWh(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

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

        private final Target imageTarget;
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
            imageTarget = new Target() {

                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    image.setImageBitmap(bitmap);
                    image.setVisibility(View.VISIBLE);
                    mImageSizes.put(mImageUrl, new ImageWh(bitmap.getWidth(), bitmap.getHeight()));
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    // XXX
                    image.setVisibility(View.GONE);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                    // image.setImageDrawable(placeHolderDrawable);
                }
            } ;
        }
    }
}
