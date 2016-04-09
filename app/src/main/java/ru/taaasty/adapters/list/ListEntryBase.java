package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.TextView;


import junit.framework.Assert;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.EntryBottomActionBar;


public abstract class ListEntryBase extends RecyclerView.ViewHolder {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ListEntryBase";

    private final FontManager mFontManager;
    private final TextView mAvatarAuthor;

    private final EntryBottomActionBar mEntryActionBar;

    @Nullable
    private FlowEntryHeader mFlowEntryHeader;
    private final boolean mShowUserAvatar;

    protected int mParentWidth;

    protected OnEntryClickListener mEntryClickListener;

    public ListEntryBase(Context context, final View v, boolean showUserAvatar) {
        super(v);
        mFontManager = FontManager.getInstance(context);
        mShowUserAvatar = showUserAvatar;
        mAvatarAuthor = (TextView) v.findViewById(R.id.avatar_author);
        mEntryActionBar = new EntryBottomActionBar(v.findViewById(R.id.entry_bottom_action_bar)) {

            @Override
            protected void onPostLikesClicked(View view, boolean canVote) {
                if (mEntryClickListener != null)
                    mEntryClickListener.onPostLikesClicked(ListEntryBase.this, view, canVote);
            }

            @Override
            protected void onPostCommentsClicked(View view) {
                if (mEntryClickListener != null)
                    mEntryClickListener.onPostCommentsClicked(ListEntryBase.this, view);
            }

            @Override
            protected void onPostAdditionalMenuClicked(View view) {
                if (mEntryClickListener != null)
                    mEntryClickListener.onPostAdditionalMenuClicked(ListEntryBase.this, view);
            }
        };


        if (!showUserAvatar) {
            mAvatarAuthor.setVisibility(View.GONE);
        }
    }

    public void setupEntry(Entry entry, TlogDesign design, String feedId) {
        mEntryActionBar.setupEntry(entry);
        setAuthor(entry, feedId);
        setFlowHeader(entry, feedId);
    }

    public void applyFeedStyle(TlogDesign design) {
        int textColor = design.getFeedTextColor(getResources());
        if (mShowUserAvatar) {
            if (design.isLightTheme()) {
                mAvatarAuthor.setTextColor(ContextCompat.getColorStateList(itemView.getContext(), R.color.text_color_feed_white_clickable));
            } else {
                mAvatarAuthor.setTextColor(textColor);
            }
        }
        mEntryActionBar.setTlogDesign(design);
    }

    public void setEntryClickListener(OnEntryClickListener listener) {
        mEntryClickListener = listener;
    }

    /**
     * Верхная часть: автор (если не отключен)
     *
     * @param item
     */

    private void setAuthor(Entry item, String feedId) {
        if (!mShowUserAvatar) return;
        User author = item.getAuthor();
        mAvatarAuthor.setText(getAvatarDescription(item, feedId));
        ImageUtils.getInstance()
                .loadAvatarToLeftDrawableOfTextView(author, R.dimen.avatar_extra_small_diameter_34dp, mAvatarAuthor);


    }

    private CharSequence getAvatarDescription(Entry item, @Nullable String feedId) {
        String template = "";
        String author = String.valueOf(item.getAuthor().getId());
        String tlog = String.valueOf(item.getTlog().id);

        boolean authorIsFeMale = item.getAuthor().isFemale();
        int authorWroteToTlogResId = authorIsFeMale ? R.string.author_female_wrote_to_tlog : R.string.author_male_wrote_to_tlog;

        if (TextUtils.isEmpty(feedId)) {
            // Просматриваем общий фид. Здесь репостов нет
            if (author.equals(tlog)) {
                // Юзер написал в свой тлог
                template = "$from";
            } else {
                // не в свой
                template = getResources().getString(authorWroteToTlogResId);
            }
        } else {
            // Просматриваем чей-то фид или поток
            if (author.equals(tlog)) {
                // Автор написал в свой тлог
                if (author.equals(feedId)) {
                    template = "$from";
                } else {
                    template = getResources().getString(R.string.repost_from);
                }
            } else {
                // Авто написал не в свой тлог
                if (tlog.equals(feedId)) {
                    template = "$from";
                } else {
                    // author, tlog, feedId не совпадают между собой.
                    template = getResources().getString(authorWroteToTlogResId);
                }
            }
        }

        return TextUtils.replace(template,
                new String[]{"$from", "$to"}, new CharSequence[]{
                        item.getAuthor().getNameWithPrefix(), item.getTlog().author.getNameWithPrefix()
                });
    }

    private void setFlowHeader(Entry item, @Nullable String feedId) {
        if (!item.getTlog().isFlow()
                || String.valueOf(item.getTlog().id).equals(feedId) /* При просмотре потока не показываем заголовки у всех постов */) {
            if (mFlowEntryHeader != null) {
                if (DBG) Log.v(TAG, "setFlowHeader() set gone to inflated header");
                mFlowEntryHeader.root.setVisibility(View.GONE);
            }
        } else {
            if (mFlowEntryHeader == null) {
                inflateFlowHeader();
            } else {
                if (DBG) Log.v(TAG, "setFlowHeader() set visible to inflated header");
            }
            mFlowEntryHeader.root.setVisibility(View.VISIBLE);
            mFlowEntryHeader.setupEntry(item);
        }
    }

    private void inflateFlowHeader() {
        if (DBG) Log.v(TAG, "inflateFlowHeader()");
        if (mFlowEntryHeader != null)
            return;

        ViewStub stub = (ViewStub) itemView.findViewById(R.id.flow_title);
        View root = stub.inflate();
        mFlowEntryHeader = new FlowEntryHeader(root) {
            @Override
            public int guessVisibleWidth(View view) {
                return guessViewVisibleWidth(view);
            }
        };

        mFlowEntryHeader.root.setOnClickListener(v -> {
            if (mEntryClickListener != null)
                mEntryClickListener.onPostFlowHeaderClicked(ListEntryBase.this, v);
        });

    }

    public void setParentWidth(int width) {
        mParentWidth = width;
    }

    protected Resources getResources() {
        return itemView.getResources();
    }

    protected FontManager getFontManager() {
        return mFontManager;
    }

    public EntryBottomActionBar getEntryActionBar() {
        return mEntryActionBar;
    }

    public TextView getAvatarAuthorView() {
        return mAvatarAuthor;
    }

    public void recycle() {
        if (mFlowEntryHeader != null) mFlowEntryHeader.stopImageLoading();
    }

    int guessViewVisibleWidth(View view) {
        if (view.getWidth() > 0) {
            int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
            if (BuildConfig.DEBUG && (mParentWidth != 0)) {
                int guessedWidth = guessViewVisibleWidthFromDimensions(view);
                Assert.assertEquals("Ширина может определяться неверно. width: " + width + " guessed: " + guessedWidth,
                        width, guessedWidth);
            }
            return width;
        } else {
            return guessViewVisibleWidthFromDimensions(view);
        }
    }

    private int guessViewVisibleWidthFromDimensions(View view) {
        if (mParentWidth == 0) return 0;
        ViewGroup.MarginLayoutParams itemViewLayoutParams = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
        ViewGroup.MarginLayoutParams viewLayoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        return mParentWidth -
                itemViewLayoutParams.leftMargin - itemViewLayoutParams.rightMargin -
                itemView.getPaddingLeft() - itemView.getPaddingRight() -
                viewLayoutParams.leftMargin - viewLayoutParams.rightMargin -
                view.getPaddingLeft() - view.getPaddingRight();
    }

    public interface OnEntryClickListener {
        void onPostLikesClicked(ListEntryBase holder, View view, boolean canVote);

        void onPostCommentsClicked(ListEntryBase holder, View view);

        void onPostAdditionalMenuClicked(ListEntryBase holder, View view);

        void onPostFlowHeaderClicked(ListEntryBase holder, View view);
    }
}
