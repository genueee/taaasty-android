package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import ru.taaasty.R;

/**
 * Created by alexey on 06.10.14.
 */
public class PhotoScrollPositionIndicator extends View {

    private final Paint mPaint;

    private float mPhotoViewWidth = 1;
    private float mImageLeft = 0;
    private float mImageRight = 1;

    public PhotoScrollPositionIndicator(Context context) {
        this(context, null);
    }

    public PhotoScrollPositionIndicator(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PhotoScrollPositionIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(context.getResources().getColor(R.color.photo_scroll_indicator_slider));

        Drawable background = context.getResources().getDrawable(R.drawable.photo_scroll_indicator_background);
        setBackgroundDrawable(background);
    }

    public void setScrollSizes(int photoViewWidth, RectF imageRect) {
        mPhotoViewWidth = photoViewWidth;
        mImageLeft = imageRect.left;
        mImageRight = imageRect.right;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int paddingLeft = getPaddingLeft();

        float width = getWidth() - paddingLeft - getPaddingRight();
        float imageWidth = mImageRight - mImageLeft;
        if (imageWidth <= 0) imageWidth = 1;

        float scale = width / imageWidth;
        float left = (0f - mImageLeft) *  scale;
        float length = width * mPhotoViewWidth / (mImageRight - mImageLeft);
        float right = left + length;

        final float top = getPaddingTop();
        final float bottom = getHeight() - getPaddingBottom();

        canvas.drawRect(left, top, right, bottom, mPaint);
    }



}
