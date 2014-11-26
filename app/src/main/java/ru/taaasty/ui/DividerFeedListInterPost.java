package ru.taaasty.ui;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import junit.framework.Assert;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FeedItemAdapter;
import ru.taaasty.adapters.list.ListEntryBase;

/**
 * Разделитель между поставми в списке
 */
public class DividerFeedListInterPost extends RecyclerView.ItemDecoration {

    private final int mDividerSize;

    private final int mCommentListPaddingBottom;

    public DividerFeedListInterPost(Context context, boolean showUserAvatar) {
        super();
        if (showUserAvatar) {
            mDividerSize = context.getResources().getDimensionPixelSize(R.dimen.feed_list_inter_post_distance_with_avatar);
        } else {
            mDividerSize = context.getResources().getDimensionPixelSize(R.dimen.feed_list_inter_post_distance);
        }

        mCommentListPaddingBottom = context.getResources().getDimensionPixelSize(R.dimen.feed_comments_padding_bottom);
    }

    @Override
    public void getItemOffsets (Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        boolean initialized = false;

        RecyclerView.ViewHolder holder = parent.getChildViewHolder(view);
        if (holder != null && holder.getPosition() > 1) {
            if (holder instanceof  ListEntryBase) {
                // У всех постов, кроме первого, делаем отступ сверху
                outRect.set(0, mDividerSize, 0, 0);
                initialized = true;
            } else {
                RecyclerView.Adapter adaper = parent.getAdapter();
                if (BuildConfig.DEBUG) Assert.assertTrue(adaper instanceof  FeedItemAdapter);
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