package ru.taaasty.widgets;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Rating;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.LikesHelper;

public abstract class EntryBottomActionBar {
    private static final boolean DBG = BuildConfig.DEBUG;

    private TextView mCommentsCountView;
    private TextView mLikesView;
    private ImageView mMoreButton;

    private Entry mOnItemListenerEntry;
    private final LikesHelper mLikesHelper;


    private TlogDesign mTlogDesign;

    /**
     * Я могу голосовать за этот пост
     */
    private boolean mCanVote = false;

    /**
     * Пост с голосовалкой
     */
    private boolean mIsVoteable = false;

    /**
     * Я проголосовал за пост
     */
    private boolean mIsVoted = false;

    private boolean mIsRatingInUpdate = false;

    private int mVotes;

    public EntryBottomActionBar(View root) {
        mTlogDesign = TlogDesign.DUMMY;
        mLikesHelper = LikesHelper.getInstance();
        mCommentsCountView = (TextView)root.findViewById(R.id.comments_count);
        mLikesView = (TextView)root.findViewById(R.id.likes);
        mMoreButton = (ImageView)root.findViewById(R.id.more);
        mOnItemListenerEntry = null;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String action = null;
                if (mOnItemListenerEntry == null) {
                    if (DBG) throw new IllegalStateException();
                    return;
                }
                switch (v.getId()) {
                    case R.id.comments_count:
                        onPostCommentsClicked(v);
                        action = "Открыты комментарии";
                        break;
                    case R.id.more:
                        onPostAdditionalMenuClicked(v);
                        action = "Открыто доп. меню";
                        break;
                    case R.id.likes:
                        onPostLikesClicked(v, mCanVote);
                        if (mCanVote) {
                            action = mIsVoted ? "Снят лайк" : "Поставлен лайк";
                        } else {
                            action = "Хотел лайкнуть, а нельзя";
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
                if (v.getContext().getApplicationContext() instanceof TaaastyApplication) {
                    AnalyticsHelper.getInstance().sendPostsEvent(action, null);
                }
            }
        };

        mCommentsCountView.setOnClickListener(onClickListener);
        mLikesView.setOnClickListener(onClickListener);
        mMoreButton.setOnClickListener(onClickListener);
    }

    // TODO Возможно, можно избавиться
    public void setOnItemListenerEntry(Entry entry) {
        mOnItemListenerEntry = entry;
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
            if (mIsVoteable
                    && mOnItemListenerEntry != null
                    && !mOnItemListenerEntry.isMyEntry()) {
                mLikesView.setClickable(true);
            } else {
                mLikesView.setClickable(false);
            }
            mLikesView.setBackgroundResource(R.drawable.list_selector_holo_light);
        }
        mLikesView.setVisibility(View.VISIBLE);
    }

    public void setCommentsClickable(boolean clickable) {
        mCommentsCountView.setOnClickListener(null);
        mCommentsCountView.setClickable(clickable);
    }

    protected abstract void onPostLikesClicked(View view, boolean canVote);
    protected abstract void onPostCommentsClicked(View view);
    protected abstract void onPostAdditionalMenuClicked(View view);

}
