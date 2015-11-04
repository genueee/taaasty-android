package ru.taaasty.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by alexey on 24.10.14.
 */
public class DividerItemDecoration extends RecyclerView.ItemDecoration {

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

    private Drawable mDivider;

    private int mOrientation;

    private int mPendingIndicatorViewType = RecyclerView.INVALID_TYPE;

    public DividerItemDecoration(Context context, int dividerId) {
        this(context, dividerId, VERTICAL_LIST);
    }

    public DividerItemDecoration(Context context, int dividerId, int orientation) {
        mDivider = context.getResources().getDrawable(dividerId).mutate();
        setOrientation(orientation);
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    /**
     * Индикатор под этим viewHolderType не будет показываться
     */
    public void setPendingIndicatorViewType(int viewHolderType) {
        mPendingIndicatorViewType = viewHolderType;
    }

    @Override
    public void onDraw (Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
        }
    }

    public void drawVertical(Canvas c, RecyclerView parent) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();

        if (mPendingIndicatorViewType != RecyclerView.INVALID_TYPE) {
            if (childCount == 0) return;
            RecyclerView.ViewHolder vh = parent.getChildViewHolder(parent.getChildAt(childCount - 1));
            if (vh != null && vh.getItemViewType() == mPendingIndicatorViewType) {
                childCount -= 1;
            }
        }

        final int dividerHeight = mDivider.getIntrinsicHeight();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin
                    + Math.round(ViewCompat.getTranslationY(child));
            final int bottom = top + dividerHeight;
            mDivider.setBounds(left, top, right, bottom);
            mDivider.setAlpha((int)(child.getAlpha() * 255f));
            mDivider.draw(c);
        }
    }

    public void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();

        final int childCount = parent.getChildCount();

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int left = child.getRight() + params.rightMargin
                    + Math.round(ViewCompat.getTranslationX(child));
            final int right = left + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.setAlpha((int)(child.getAlpha() * 255f));
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets (Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL_LIST) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }
}