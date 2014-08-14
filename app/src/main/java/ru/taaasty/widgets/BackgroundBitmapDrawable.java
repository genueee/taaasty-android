package ru.taaasty.widgets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;

import ru0xdc.NdkStackBlur;


public class BackgroundBitmapDrawable extends BitmapDrawable {
    private Matrix mMatrix = new Matrix();
    private Matrix mBlurScaleMatrix = new Matrix();
    private int moldHeight;

    public static final int COVER_ALIGN_CENTER_CROP = 0;
    public static final int COVER_ALIGN_TILES = 1;
    public static final int COVER_ALIGN_STRETCH = 2;

    private int mCoverAlign = COVER_ALIGN_CENTER_CROP;

    private int mBlurRarius;
    private int mBlurScaleFactor = 3;

    private boolean mRefreshBlurredBitmap = true;

    private Bitmap mBlurredBitmap;

    private final NdkStackBlur mBlurer = NdkStackBlur.create();

    public BackgroundBitmapDrawable(Resources res, Bitmap bitmap) {
        super(res, bitmap);
    }

    public void setCoverAlign(int coverAlign) {
        if (mCoverAlign != coverAlign) {
            mCoverAlign = coverAlign;
            switch (mCoverAlign) {
                case COVER_ALIGN_CENTER_CROP:
                    setTileModeXY(null, null);
                    setGravity(Gravity.CENTER);
                    break;
                case COVER_ALIGN_TILES:
                    setGravity(Gravity.FILL);
                    setTileModeXY(Shader.TileMode.MIRROR, Shader.TileMode.MIRROR);
                    break;
                case COVER_ALIGN_STRETCH:
                    setTileModeXY(null, null);
                    setGravity(Gravity.FILL);
                    break;
            }

            invalidateSelf();
        }
    }

    public int getCoverAlign() {
        return mCoverAlign;
    }

    public int getBlur() {
        return mBlurRarius;
    }

    public void setBlurRadius(int blur) {
        mBlurRarius = blur;
        mRefreshBlurredBitmap = true;
    }

    public int getBlurScaleFactor() {
        return mBlurScaleFactor;
    }

    public void setBlurScaleFactor(int scaleFactor) {
        mBlurScaleFactor = scaleFactor;
        mRefreshBlurredBitmap = true;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        if (bounds.height() > moldHeight) {
            moldHeight = bounds.height();
            Bitmap b = getBitmap();
            RectF src = new RectF(0, 0, b.getWidth(), b.getHeight());
            RectF dst;

            float dwidth = src.width();
            float dheight = src.height();

            float vwidth = bounds.width();
            float vheight = bounds.height();

            float scale;
            float dx = 0, dy = 0;

            if (dwidth * vheight > vwidth * dheight) {
                scale = (float) vheight / (float) dheight;
                dx = (vwidth - dwidth * scale) * 0.5f;
            } else {
                scale = (float) vwidth / (float) dwidth;
                dy = (vheight - dheight * scale) * 0.5f;
            }

            mMatrix.setScale(scale, scale);
            mMatrix.postTranslate(dx, dy);
        }
        mRefreshBlurredBitmap = true;
    }

    private void refreshBlurredBitmap() {
        mRefreshBlurredBitmap = false;

        mBlurScaleMatrix.set(mMatrix);
        if (mBlurRarius == 0) {
            mBlurredBitmap = getBitmap();
        } else {
            mBlurScaleMatrix.preScale(mBlurScaleFactor, mBlurScaleFactor);
            Bitmap b = getBitmap();
            mBlurredBitmap = Bitmap.createScaledBitmap(b, (int)(b.getWidth() / (float)mBlurScaleFactor),
                    (int)(b.getHeight() / (float)mBlurScaleFactor), true);
            // mBlurredBitmap = b.copy(Bitmap.Config.ARGB_8888, true);
            mBlurer.blur(mBlurRarius, mBlurredBitmap);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mCoverAlign == COVER_ALIGN_CENTER_CROP) {
            if (mRefreshBlurredBitmap) refreshBlurredBitmap();
            // canvas.drawColor(0xaa00ff00);
            canvas.drawBitmap(mBlurredBitmap, mBlurScaleMatrix, null);
        } else {
            super.draw(canvas);
        }
    }
}
