package ru.taaasty.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Rating;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.utils.ImageUtils;

/**
 * Created by alexey on 20.08.14.
 */
public class EntryBottomActionBar {
    private static final boolean DBG = BuildConfig.DEBUG;
    public static final int AVATAR_EXTRA_SMALL_DIAMETER = R.dimen.avatar_extra_small_diameter;

    private TextView mCommentsCountView;
    private TextView mLikesView;
    @Nullable
    private TextView mUserInfo;
    private ImageView mMoreButton;

    private boolean mHideUserInfo;
    private OnEntryActionBarListener mListener;
    private Entry mOnItemListenerEntry;

    private TlogDesign mTlogDesign;

    public EntryBottomActionBar(View root, boolean hideUserInfo) {
        mHideUserInfo = hideUserInfo;
        mTlogDesign = TlogDesign.DUMMY;
        setRoot(root);
    }

    public interface OnEntryActionBarListener {
        public void onPostLikesClicked(View view, Entry entry);
        public void onPostCommentsClicked(View view, long postId);
        public void onPostUserInfoClicked(View view, Entry entry);
        public void onPostAdditionalMenuClicked(View view, long postId);
    }

    public void setRoot(View root) {
        mOnItemListenerEntry = null;
        mCommentsCountView = (TextView)root.findViewById(R.id.comments_count);
        mLikesView = (TextView)root.findViewById(R.id.likes);
        mMoreButton = (ImageView)root.findViewById(R.id.more);

        if (mHideUserInfo) {
            root.findViewById(R.id.user_info).setVisibility(View.INVISIBLE);
            mUserInfo = null;
        } else {
            mUserInfo = (TextView)root.findViewById(R.id.user_info);
            Drawable userIconDrawable = new ColorDrawable(Color.TRANSPARENT);
            int sizePx = root.getContext().getResources().getDimensionPixelSize(AVATAR_EXTRA_SMALL_DIAMETER);
            userIconDrawable.setBounds(0, 0, sizePx, sizePx);
            mUserInfo.setCompoundDrawables(userIconDrawable, null, null, null);
            mUserInfo.setVisibility(View.VISIBLE);
        }
    }

    public void setOnItemListenerEntry(Entry entry) {
        mOnItemListenerEntry = entry;
    }

    public void setOnItemClickListener(OnEntryActionBarListener listener) {
        mListener = listener;
        mCommentsCountView.setOnClickListener(mOnClickListener);
        mLikesView.setOnClickListener(mOnClickListener);
        if (mUserInfo != null) mUserInfo.setOnClickListener(mOnClickListener);
        mMoreButton.setOnClickListener(mOnClickListener);
    }

    public void setTlogDesign(TlogDesign design) {
        mTlogDesign = design;
        setupTlogDesign();
    }

    void setupTlogDesign() {
        TlogDesign design = mTlogDesign == null ? TlogDesign.DUMMY : mTlogDesign;
        if (design.isLightTheme()) {
            mCommentsCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comments_count_light, 0, 0, 0);
            mMoreButton.setImageResource(R.drawable.ic_more_light);
        } else {
            mCommentsCountView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comments_count_dark, 0, 0, 0);
            mMoreButton.setImageResource(R.drawable.ic_more_dark);
        }
    }

    public void setupEntry(Entry item) {
        setComments(item);
        setRating(item, false);
        setUserAvatar(item);
    }

    public void setComments(Entry item) {
        int comments = item.getCommentsCount();
        mCommentsCountView.setText(String.valueOf(comments));
    }

    private int getNotVotedDrawable() {
        return mTlogDesign.isDarkTheme() ? R.drawable.ic_like_not_voted_dark : R.drawable.ic_like_not_voted_light;
    }

    public void setRating(Entry item, boolean isRatingInUpdate) {
        Rating r = item.getRating();

        if (!r.isVoteable) {
            mLikesView.setVisibility(View.INVISIBLE);
        } else {
            mLikesView.setVisibility(View.VISIBLE);

            if (isRatingInUpdate) {
                mLikesView.setText("—");
                mLikesView.setEnabled(false);
            } else {
                mLikesView.setText(String.valueOf(r.votes));
                mLikesView.setEnabled(true);
            }

            Resources resources = mLikesView.getResources();
            if (r.isVoted) {
                mLikesView.setTextColor(resources.getColor(R.color.text_color_feed_item_likes_gt1));
                mLikesView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_voted, 0, 0, 0);
                mLikesView.setBackgroundDrawable(null); // XXX: убрать нафиг, когда починят отмену голоса
            } else {
                mLikesView.setTextColor(Color.BLACK);
                mLikesView.setCompoundDrawablesWithIntrinsicBounds(getNotVotedDrawable(), 0, 0, 0);
                mLikesView.setClickable(true);
                mLikesView.setBackgroundResource(R.drawable.clickable_item_background);
            }
        }
    }

    public void setCommentsClickable(boolean clickable) {
        mCommentsCountView.setClickable(clickable);
    }

    public void setUserAvatar(Entry item) {
        if (mUserInfo == null) return;

        mUserInfo.setText(item.getAuthor().getSlug());
        mUserInfo.setVisibility(View.VISIBLE);
        ImageUtils.getInstance().loadAvatar(mCommentsCountView.getContext(),
                item.getAuthor().getUserpic(), item.getAuthor().getSlug(), mAvatarTarget,
                AVATAR_EXTRA_SMALL_DIAMETER
                );
    }

    private final ImageUtils.DrawableTarget mAvatarTarget = new ImageUtils.DrawableTarget() {
        @Override
        public void onDrawableReady(Drawable drawable) {
            Context context = mUserInfo.getContext();
            if (context == null) return;
            int avatarSize = context.getResources().getDimensionPixelSize(AVATAR_EXTRA_SMALL_DIAMETER);
            drawable.setBounds(0, 0, avatarSize, avatarSize);
            mUserInfo.setCompoundDrawables(drawable, null, null, null);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Context context = mUserInfo.getContext();
            if (context == null) return;
            Drawable drawables[] = mUserInfo.getCompoundDrawables();
            Drawable placeholder = drawables != null ? drawables[0] : null;
            if (placeholder instanceof AnimationDrawable) {
                ((AnimationDrawable) placeholder).stop();
            }
            PicassoDrawable drawable =
                    new PicassoDrawable(context, bitmap, placeholder, from, false, false);
            mUserInfo.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            Context context = mUserInfo.getContext();
            if (context == null) return;
            int avatarSize = context.getResources().getDimensionPixelSize(AVATAR_EXTRA_SMALL_DIAMETER);
            errorDrawable.setBounds(0, 0, avatarSize, avatarSize);
            mUserInfo.setCompoundDrawables(errorDrawable, null, null, null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener == null) return;
            if (mOnItemListenerEntry == null) {
                if (DBG) throw new IllegalStateException();
                return;
            }
            switch (v.getId()) {
                case R.id.comments_count:
                    mListener.onPostCommentsClicked(v, mOnItemListenerEntry.getId());
                    break;
                case R.id.more:
                    mListener.onPostAdditionalMenuClicked(v, mOnItemListenerEntry.getId());
                    break;
                case R.id.user_info:
                    mListener.onPostUserInfoClicked(v, mOnItemListenerEntry);
                    break;
                case R.id.likes:
                    mListener.onPostLikesClicked(v, mOnItemListenerEntry);
                    break;
                default:
                    throw new IllegalArgumentException();
            }
        }
    };
}