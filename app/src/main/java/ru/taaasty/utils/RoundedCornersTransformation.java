package ru.taaasty.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;

import com.squareup.picasso.Transformation;

public class RoundedCornersTransformation implements Transformation {

    private static final int CORNER_RADIUS_CIRCLE = -1;

    private static RoundedCornersTransformation sInstance = new RoundedCornersTransformation(CORNER_RADIUS_CIRCLE);

    private final int mCornerRadius;

    public static RoundedCornersTransformation createCircle() {
        return sInstance;
    }

    public static RoundedCornersTransformation create(int cornerRadius) {
        return new RoundedCornersTransformation(cornerRadius);
    }

    private RoundedCornersTransformation(int cornerRadius) {
        mCornerRadius = cornerRadius;
    }

    @Override
    public Bitmap transform(Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());

        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;

        Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
        if (squaredBitmap != source) {
            source.recycle();
        }

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
        paint.setShader(shader);
        paint.setAntiAlias(true);

        if (mCornerRadius == CORNER_RADIUS_CIRCLE) {
            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);
        } else {
            if (Build.VERSION.SDK_INT >= 21 ) {
                canvas.drawRoundRect(0, 0, size, size, this.mCornerRadius, this.mCornerRadius, paint);
            } else {
                canvas.drawRoundRect(new RectF(0, 0, size, size), this.mCornerRadius, this.mCornerRadius, paint);
            }
        }

        squaredBitmap.recycle();
        return bitmap;
    }

    @Override
    public String key() {
        return "circleTransformation(" +
                "cornerRadius=" + this.mCornerRadius +
                ")";
    }


}
