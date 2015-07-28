package ru.taaasty.widgets;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Rating;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.utils.LikesHelper;

/**
 * Created by alexey on 20.08.14.
 */
public class EntryBottomActionBar {
    private static final boolean DBG = BuildConfig.DEBUG;

    private TextView mCommentsCountView;
    private TextView mLikesView;
    private ImageView mMoreButton;

    private OnEntryActionBarListener mListener;
    private Entry mOnItemListenerEntry;
    private final LikesHelper mLikesHelper;


    private TlogDesign mTlogDesign;

    private boolean mCanVote = false;
    private boolean mIsVoteable = false;
    private boolean mIsVoted = false;
    private boolean mIsRatingInUpdate = false;
    private int mVotes;


    public EntryBottomActionBar(View root) {
        mTlogDesign = TlogDesign.DUMMY;
        mLikesHelper = LikesHelper.getInstance();
        setRoot(root);
    }

    public interface OnEntryActionBarListener {
        void onPostLikesClicked(View view, Entry entry);
        void onPostCommentsClicked(View view, Entry entry);
        void onPostAdditionalMenuClicked(View view, Entry entry);
    }

    public void setRoot(View root) {
        mOnItemListenerEntry = null;
        mCommentsCountView = (TextView)root.findViewById(R.id.comments_count);
        mLikesView = (TextView)root.findViewById(R.id.likes);
        mMoreButton = (ImageView)root.findViewById(R.id.more);
    }

    public void setOnItemListenerEntry(Entry entry) {
        mOnItemListenerEntry = entry;
    }

    public void setOnItemClickListener(OnEntryActionBarListener listener) {
        mListener = listener;
        mCommentsCountView.setOnClickListener(mOnClickListener);
        mLikesView.setOnClickListener(mOnClickListener);
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

        Resources r = mCommentsCountView.getResources();
        int textColor = mTlogDesign.getFeedActionsTextColor(r);

        mCommentsCountView.setTextColor(textColor);
        mLikesView.setTextColor(textColor);

        refreshRating();
    }

    public void setupEntry(Entry item) {
        setComments(item);
        setRating(item);
    }

    public void setComments(Entry item) {
        int comments = item.getCommentsCount();
        mCommentsCountView.setText(String.valueOf(comments));
    }

    private int getNotVotedDrawable() {
        return mTlogDesign.isDarkTheme() ? R.drawable.ic_like_not_voted_dark : R.drawable.ic_like_not_voted_light;
    }

    public void setRating(Entry item) {
        Rating r = item.getRating();
        mCanVote = item.canVote();
        mIsVoteable = item.isVoteable();
        mIsVoted = r.isVoted;
        mVotes = r.votes;
        mIsRatingInUpdate = mLikesHelper.isRatingInUpdate(item.getId());
        refreshRating();
    }

    private void refreshRating() {
        if (!mIsVoteable && !mCanVote) {
            mLikesView.setVisibility(View.GONE);
            return;
        }

        if (mIsRatingInUpdate) {
            mLikesView.setText("—");
            mLikesView.setEnabled(false);
        } else {
            mLikesView.setText(String.valueOf(mVotes));
            mLikesView.setEnabled(true);
        }

        Resources resources = mLikesView.getResources();
        if (mIsVoted) {
            mLikesView.setTextColor(resources.getColor(R.color.text_color_feed_item_likes_gt1));
            mLikesView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_voted, 0, 0, 0);
        } else {
            TlogDesign design = mTlogDesign == null ? TlogDesign.DUMMY : mTlogDesign;
            mLikesView.setTextColor(design.getFeedActionsTextColor(resources));
            mLikesView.setCompoundDrawablesWithIntrinsicBounds(getNotVotedDrawable(), 0, 0, 0);
            mLikesView.setClickable(mCanVote);
            mLikesView.setBackgroundResource(R.drawable.list_selector_holo_light);
        }
        mLikesView.setVisibility(View.VISIBLE);
    }

    public void setCommentsClickable(boolean clickable) {
        mCommentsCountView.setOnClickListener(null);
        mCommentsCountView.setClickable(clickable);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String action = null;
            if (mListener == null) return;
            if (mOnItemListenerEntry == null) {
                if (DBG) throw new IllegalStateException();
                return;
            }
            switch (v.getId()) {
                case R.id.comments_count:
                    mListener.onPostCommentsClicked(v, mOnItemListenerEntry);
                    action = "Открыты комментарии";
                    break;
                case R.id.more:
                    mListener.onPostAdditionalMenuClicked(v, mOnItemListenerEntry);
                    action = "Открыто доп. меню";
                    break;
                case R.id.likes:
                    if (mCanVote) {
                        mListener.onPostLikesClicked(v, mOnItemListenerEntry);
                        action = mIsVoted ? "Снят лайк" : "Поставлен лайк";
                    }
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            if (action != null && v.getContext().getApplicationContext() instanceof  TaaastyApplication) {
                ((TaaastyApplication) v.getContext().getApplicationContext())
                        .sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_POSTS, action, null);
            }
        }
    };
}
