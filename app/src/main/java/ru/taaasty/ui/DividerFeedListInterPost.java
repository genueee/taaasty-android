package ru.taaasty.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import ru.taaasty.R;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.list.ListEntryBase;
import ru.taaasty.adapters.list.ListQuoteEntry;
import ru.taaasty.adapters.list.ListTextEntry;

/**
 * Разделитель между поставми в списке
 */
public class DividerFeedListInterPost extends RecyclerView.ItemDecoration {

    private final int mDividerSize;

    private final int mCommentListPaddingBottom;

    private final int mPaddingAboveTopText;

    private final boolean mAddPaddingAboveTopText;

    public DividerFeedListInterPost(Context context, boolean showUserAvatar) {
        super();

        Resources resources = context.getResources();

        mAddPaddingAboveTopText = !showUserAvatar;
        if (showUserAvatar) {
            mDividerSize = resources.getDimensionPixelSize(R.dimen.feed_list_inter_post_distance_with_avatar);
        } else {
            mDividerSize = resources.getDimensionPixelSize(R.dimen.feed_list_inter_post_distance);
        }

        mCommentListPaddingBottom = context.getResources().getDimensionPixelSize(R.dimen.feed_comments_padding_bottom);

        if (mAddPaddingAboveTopText) {
            mPaddingAboveTopText = resources.getDimensionPixelSize(R.dimen.feed_top_text_padding_bottom);
        } else {
            mPaddingAboveTopText = 0;
        }

    }

    @Override
    public void getItemOffsets (Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        boolean initialized = false;

        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);

        // Добавляем отступ над первым текстовым постом, если там текст
        if (mAddPaddingAboveTopText
                && holder != null
                && holder.getPosition() == 1
                && (holder instanceof ListTextEntry || holder instanceof ListQuoteEntry)) {
            outRect.set(0, mPaddingAboveTopText, 0, 0);
            initialized = true;
        }

        if (holder != null && holder.getPosition() > 1) {
            if (holder instanceof  ListEntryBase) {
                // У всех постов, кроме первого, делаем отступ сверху
                outRect.set(0, mDividerSize, 0, 0);
                initialized = true;
            } else if (parent.getAdapter() instanceof  FeedItemAdapter) {
                RecyclerView.Adapter adaper = parent.getAdapter();
                int position = holder.getPosition();
                if (!(position + 1 >= adaper.getItemCount())
                        && adaper.getItemViewType(position) == FeedItemAdapter.VIEW_TYPE_COMMENT
                        && adaper.getItemViewType(position + 1) != FeedItemAdapter.VIEW_TYPE_COMMENT) {
                    // Под последним комментарием поста добавляем отступ
                    outRect.set(0, 0, 0, mCommentListPaddingBottom);
                    initialized = true;
                }
            }
        }

        if (!initialized) outRect.set(0, 0, 0, 0);
    }
}