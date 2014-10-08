package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import ru.taaasty.R;

/**
 * Created by alexey on 06.10.14.
 */
public class PhotoScrollPositionIndicator extends View {

    private boolean mHidden = true;

    private final int mAnimationDuration;

    private float mPhotoViewWidth = 1;
    private float mImageLeft = 0;
    private float mImageRight = 1;

    private Path mTempPath = new Path();
    private RectF mTempRectF = new RectF();

    private Paint mBackgroundPaint;
    private Paint mIndicatorPaint;

    private int mIndicatorColor;
    private int mBackgroundColor;

    private int mWidth;
    private int mHeight;

    public PhotoScrollPositionIndicator(Context context) {
        this(context, null);
    }

    public PhotoScrollPositionIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoScrollPositionIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mAnimationDuration = getResources().getInteger(R.integer.shortAnimTime);
        mBackgroundColor = getResources().getColor(R.color.photo_scroll_indicator_background);
        mIndicatorColor = getResources().getColor(R.color.photo_scroll_indicator_slider);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(mBackgroundColor);
        mBackgroundPaint.setAntiAlias(true);

        mIndicatorPaint = new Paint();
        mIndicatorPaint.setColor(mIndicatorColor);
        mIndicatorPaint.setAntiAlias(true);

        setAlpha(0);
    }

    public void setScrollSizes(int photoViewWidth, RectF imageRect) {
        mPhotoViewWidth = photoViewWidth;
        mImageLeft = imageRect.left;
        mImageRight = imageRect.right;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = h;
        mWidth = w;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mTempRectF.top = 0;
        mTempRectF.bottom = mHeight;
        mTempRectF.left = 0;
        mTempRectF.right = mWidth;

        drawPill(canvas, mTempRectF, mBackgroundPaint);

        float imageWidth = mImageRight - mImageLeft;
        if (imageWidth <= 0) imageWidth = 1;

        float scale = mWidth / imageWidth;
        float left = (0f - mImageLeft) *  scale;
        float length = mWidth * mPhotoViewWidth / (mImageRight - mImageLeft);
        float right = left + length;

        mTempRectF.top = 0;
        mTempRectF.bottom = mHeight;
        mTempRectF.left = left;
        mTempRectF.right = right;

        drawPill(canvas, mTempRectF, mIndicatorPaint);
    }

    private void drawPill(Canvas canvas, RectF rectF, Paint paint) {
        float radius = rectF.height() / 2;
        float temp;

        mTempPath.reset();
        mTempPath.moveTo(rectF.left + radius, rectF.top);
        mTempPath.lineTo(rectF.right - radius, rectF.top);

        temp = rectF.left;
        rectF.left = rectF.right - 2 * radius;
        mTempPath.arcTo(rectF, 270, 180);
        rectF.left = temp;

        mTempPath.lineTo(rectF.left + radius, rectF.bottom);

        temp = rectF.right;
        rectF.right = rectF.left + rectF.height();
        mTempPath.arcTo(rectF, 90, 180);
        rectF.right = temp;

        mTempPath.close();
        canvas.drawPath(mTempPath, paint);
    }

    public void show() {
        if (!mHidden) {
            return;
        }

        mHidden = false;
        animate().cancel();
        animate().alpha(1f).setDuration(mAnimationDuration);
    }

    public void hide() {
        if (mHidden) {
            return;
        }

        mHidden = true;
        animate().cancel();
        animate().alpha(0f).setDuration(mAnimationDuration);
    }



}
