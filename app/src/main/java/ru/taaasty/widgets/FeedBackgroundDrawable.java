package ru.taaasty.widgets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;

import ru.taaasty.R;
import ru.taaasty.rest.model.TlogDesign;

/**
 * Created by alexey on 15.08.15.
 */
public class FeedBackgroundDrawable extends BitmapDrawable {

    private final int mDrawBitmapOnTopLimit;

    private final int mHeaderHeight;

    private Matrix mMatrix = new Matrix();
    private int mBitmapRectHeight;

    private boolean mDrawBitmapOnTop;

    private final Rect mFeedRect = new Rect();
    private final Rect mScreenRect = new Rect();
    private final Rect mFeedMargin = new Rect();

    private final Paint mFeedPaint = new Paint();

    private final Paint mBorderPaint = new Paint();

    private final Paint mBorderTopPaint = new Paint();

    public static @ColorRes int getFeedColorResId(TlogDesign design) {
        if (design.isLightTheme()) {
            return R.color.feed_light_background_color;
        } else {
            return R.color.feed_dark_background_color;
        }
    }

    public static @ColorRes int getFeedBorderColorResId(TlogDesign design) {
        if (design.isLightTheme()) {
            return R.color.feed_light_border_background_color;
        } else {
            return R.color.feed_dark_border_background_color;
        }
    }


    public FeedBackgroundDrawable(Resources res, @Nullable Bitmap bitmap, int headerHeight) {
        super(res, bitmap);
        setFeedDesign(res, TlogDesign.DUMMY);
        mBorderTopPaint.setColor(res.getColor(R.color.feed_dark_border_background_color));
        mDrawBitmapOnTopLimit = (int)(48f * res.getDisplayMetrics().densityDpi / 160f + 0.5f);
        mHeaderHeight = headerHeight;
    }

    public void setFeedMargin(int left, int top, int right, int bottom) {
        if ((mFeedMargin.left != left)
                || (mFeedMargin.right != right)
                || (mFeedMargin.top != top)
                || (mFeedMargin.bottom != bottom)) {
            mFeedMargin.left = left;
            mFeedMargin.top = top;
            mFeedMargin.right = right;
            mFeedMargin.bottom = bottom;
            refreshFeedRect();
            invalidateSelf();
        }
    }

    public void setFeedDesign(Resources resources, TlogDesign design) {
        setFeedColor(resources.getColor(getFeedColorResId(design)));
        setBorderColor(resources.getColor(getFeedBorderColorResId(design)));
    }

    public void setFeedColor(@ColorInt int color) {
        if (color != mFeedPaint.getColor()) {
            mFeedPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setBorderColor(@ColorInt int color) {
        if (color != mBorderPaint.getColor()) {
            mBorderPaint.setColor(color);
            invalidateSelf();
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        refreshBitmapMatrix(bounds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (getBitmap() == null) {
            drawNullBitmap(canvas);
        } else if (mDrawBitmapOnTop) {
            drawBitmapOnTop(canvas);
        } else {
            drawBitmapFullScreen(canvas);
        }
    }

    private void refreshFeedRect() {
        copyBounds(mFeedRect);
        mFeedRect.left += mFeedMargin.left;
        mFeedRect.top += mFeedMargin.top;
        mFeedRect.right -= mFeedMargin.right;
        mFeedRect.bottom -= mFeedMargin.bottom;
    }

    private void refreshBitmapMatrix(Rect bounds) {
        if (getBitmap() == null) return;

        mDrawBitmapOnTop = mFeedMargin.left <= mDrawBitmapOnTopLimit;

        int newHeight;
        if (mDrawBitmapOnTop) {
            newHeight = mHeaderHeight;
        } else {
            newHeight = bounds.height();
        }

        if (newHeight == 0 || mBitmapRectHeight == newHeight) return;

        mBitmapRectHeight = newHeight;

        float dwidth = getBitmap().getWidth();
        float dheight = getBitmap().getHeight();

        float vwidth = bounds.width();
        float vheight = newHeight;

        float scale;
        float dx = 0, dy = 0;

        if (dwidth * vheight > vwidth * dheight) {
            scale = vheight / (float) dheight;
            dx = (vwidth - dwidth * scale) * 0.5f;
        } else {
            scale = vwidth / (float) dwidth;
            dy = (vheight - dheight * scale) * 0.5f;
        }

        mMatrix.setScale(scale, scale);
        mMatrix.postTranslate(dx, dy);
    }

    private void drawNullBitmap(Canvas canvas) {
        refreshFeedRect();
        copyBounds(mScreenRect);
        drawTopMargin(canvas);
        drawLeftMargin(canvas);
        drawRightMargin(canvas);
        drawBottomMargin(canvas);
        canvas.drawRect(mFeedRect, mFeedPaint);
    }

    private void drawBitmapOnTop(Canvas canvas) {
        refreshFeedRect();
        copyBounds(mScreenRect);
        if (mFeedMargin.top > 0) {
            canvas.save();
            canvas.clipRect(
                    mScreenRect.left, mScreenRect.top,
                    mScreenRect.right, mFeedRect.top
            );
            canvas.drawBitmap(getBitmap(), mMatrix, getPaint());
            canvas.restore();
        }

        drawLeftMargin(canvas);
        drawRightMargin(canvas);
        drawBottomMargin(canvas);
        canvas.drawRect(mFeedRect, mFeedPaint);
    }

    private void drawBitmapFullScreen(Canvas canvas) {
        refreshFeedRect();
        if (!mFeedRect.contains(getBounds())) {
            canvas.save();
            // DIFFERENCE работает только с API 18 при апаратном ускорении
            // Если здесь его заменить на несколько clip с union, то появляются артефакты на 4.1,
            // поэтому не надо, пусть лучше игнориреутся
            canvas.clipRect(mFeedRect, Region.Op.DIFFERENCE);
            canvas.drawBitmap(getBitmap(), mMatrix, getPaint());
            canvas.restore();
        }
        canvas.drawRect(mFeedRect, mFeedPaint);
    }

    private void drawTopMargin(Canvas canvas) {
        if (mFeedMargin.top <= 0) return;
        canvas.drawRect(
                mScreenRect.left, mScreenRect.top,
                mScreenRect.right, mFeedRect.top,
                mBorderTopPaint
        );
    }

    private void drawLeftMargin(Canvas canvas) {
        if (mFeedMargin.left <= 0) return;
        canvas.drawRect(
                mScreenRect.left, mFeedRect.top,
                mFeedRect.left, mScreenRect.bottom,
                mBorderPaint
        );
    }

    private void drawRightMargin(Canvas canvas) {
        if (mFeedMargin.right <= 0) return;
        canvas.drawRect(
                mFeedRect.right, mFeedRect.top,
                mScreenRect.right, mScreenRect.bottom,
                mBorderPaint
        );
    }

    private void drawBottomMargin(Canvas canvas) {
        if (mFeedMargin.bottom <= 0) return;
        canvas.drawRect(
                mFeedRect.left, mFeedRect.bottom,
                mFeedRect.right, mScreenRect.bottom,
                mBorderPaint
        );
    }

}

