package ru.taaasty.widgets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
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
    private int mOldHeight;

    public static final int COVER_ALIGN_CENTER_CROP = 0;
    public static final int COVER_ALIGN_TILES = 1;
    public static final int COVER_ALIGN_STRETCH = 2;

    private int mCoverAlign = COVER_ALIGN_CENTER_CROP;

    private int mBlurRarius;
    private int mBlurScaleFactor = 3;

    private boolean mRefreshBlurredBitmap = true;
    private boolean mRebuildShader = false;
    private boolean mApplyGravity = false;

    private Bitmap mBlurredBitmap;

    private final Rect mDstRect = new Rect();   // Gravity.apply() sets this

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
    public void setTileModeXY(Shader.TileMode xmode, Shader.TileMode ymode) {
        if (getTileModeX() != xmode && getTileModeY() != ymode) {
            mRebuildShader = true;
        }
        super.setTileModeXY(xmode, ymode);
    }

    @Override
    public void setGravity(int gravity) {
        if (getGravity() != gravity) {
            mApplyGravity = true;
        }
        super.setGravity(gravity);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        mApplyGravity = true;
        mRefreshBlurredBitmap = true;

        if (bounds.height() > mOldHeight) {
            mOldHeight = bounds.height();
            Bitmap b = getBitmap();
            RectF src = new RectF(0, 0, b.getWidth(), b.getHeight());

            float dwidth = src.width();
            float dheight = src.height();

            float vwidth = bounds.width();
            float vheight = bounds.height();

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
    }

    private void refreshBlurredBitmap() {
        mRefreshBlurredBitmap = false;

        mBlurScaleMatrix.set(mMatrix);
        if (mBlurRarius == 0) {
            mBlurredBitmap = getBitmap();
        } else {
            mBlurScaleMatrix.preScale(mBlurScaleFactor, mBlurScaleFactor);
            Bitmap b = getBitmap();
            int widthScaled = Math.max((int) Math.ceil(b.getWidth() / (float) mBlurScaleFactor), 1);
            int heightScaled = Math.max((int) Math.ceil(b.getHeight() / (float) mBlurScaleFactor), 1);
            mBlurredBitmap = Bitmap.createScaledBitmap(b, widthScaled, heightScaled, true);
            // mBlurredBitmap = b.copy(Bitmap.Config.ARGB_8888, true);
            mBlurer.blur(mBlurRarius, mBlurredBitmap);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (mRefreshBlurredBitmap) refreshBlurredBitmap();
        if (mCoverAlign == COVER_ALIGN_CENTER_CROP) {
            canvas.drawBitmap(mBlurredBitmap, mBlurScaleMatrix, getPaint());
        } else {
            Bitmap bitmap = mBlurredBitmap;
            if (bitmap != null) {
                if (mRebuildShader) {
                    Shader.TileMode tmx = getTileModeX();
                    Shader.TileMode tmy = getTileModeY();

                    if (tmx == null && tmy == null) {
                        getPaint().setShader(null);
                    } else {
                        getPaint().setShader(new BitmapShader(bitmap,
                                tmx == null ? Shader.TileMode.CLAMP : tmx,
                                tmy == null ? Shader.TileMode.CLAMP : tmy));
                    }
                    mRebuildShader = false;
                    copyBounds(mDstRect);
                }

                Shader shader = getPaint().getShader();
                if (shader == null) {
                    if (mApplyGravity) {
                        Gravity.apply(getGravity(), getIntrinsicWidth(), getIntrinsicHeight(),
                                getBounds(), mDstRect);
                        mApplyGravity = false;
                    }
                    canvas.drawBitmap(bitmap, null, mDstRect, getPaint());
                } else {
                    if (mApplyGravity) {
                        copyBounds(mDstRect);
                        mApplyGravity = false;
                    }
                    canvas.drawRect(mDstRect, getPaint());
                }
            }
        }
    }
}
