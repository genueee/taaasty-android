package ru.taaasty.widgets;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.os.Build;
import android.util.TypedValue;

import ru.taaasty.R;

/**
 * Created by alexey on 03.11.14.
 */
public class DateBackgroundDrawable extends ShapeDrawable {

    private int mPaddingLeftRight;

    private int mPaddingTopBottom;

    public DateBackgroundDrawable(Resources resources) {
        super(new PillShape());
        getPaint().setColor(resources.getColor(R.color.date_indicator_background));
        mPaddingLeftRight = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, resources.getDisplayMetrics()));
        mPaddingTopBottom = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, resources.getDisplayMetrics()));
    }

    public static class PillShape extends Shape {
        private RectF mRect = new RectF();
        private Path mTempPath = new Path();

        @Override
        protected void onResize(float width, float height) {
            mRect.set(0, 0, width, height);
            updatePillPath();
        }

        @Override
        public void draw(Canvas canvas, Paint paint) {
            canvas.drawPath(mTempPath, paint);
        }

        @Override
        public PillShape clone() throws CloneNotSupportedException {
            final PillShape shape = (PillShape) super.clone();
            shape.mRect = new RectF(mRect);
            shape.mTempPath = new Path(mTempPath);
            return shape;
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void getOutline(Outline outline) {
            outline.setConvexPath(mTempPath);
        }

        private void updatePillPath() {
            float radius = mRect.height() / 2;
            float temp;

            mTempPath.reset();
            mTempPath.moveTo(mRect.left + radius, mRect.top);
            mTempPath.lineTo(mRect.right - radius, mRect.top);

            temp = mRect.left;
            mRect.left = mRect.right - 2 * radius;
            mTempPath.arcTo(mRect, 270, 180);
            mRect.left = temp;

            mTempPath.lineTo(mRect.left + radius, mRect.bottom);

            temp = mRect.right;
            mRect.right = mRect.left + mRect.height();
            mTempPath.arcTo(mRect, 90, 180);
            mRect.right = temp;

            mTempPath.close();
        }
    }
}
