package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import java.util.Random;

import ru.taaasty.model.Userpic;
import ru.taaasty.utils.FontManager;

/**
 * Created by alexey on 31.07.14.
 */
public class DefaultUserpicDrawable extends Drawable {
    public static final int AVATAR_DIAMETER = 128;
    public static final int TEXT_SIZE = 42;
    private final Paint mBackgroundPaint;
    private final Paint mTextPaint;

    private final Typeface mMainFont;

    private int mBackgroundColor;
    private int mTextColor;
    private int mAlpha;
    private String mUsername;
    private Rect mBounds;

    private boolean mPaintsInvalidated;

    public DefaultUserpicDrawable(Context context, String username, int backgroundColor, int textColor) {
        mBackgroundPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mTextPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mAlpha = 255;
        mMainFont = FontManager.getInstance(context).getMainFont();
        setUser(username, backgroundColor, textColor);
    }

    public DefaultUserpicDrawable(Context context) {
        this(context, "", 0, 0);
        Random rnd = new Random();
        setUser("", Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256)), Color.BLACK);
    }

    public DefaultUserpicDrawable(Context context, Userpic userpic, String name) {
        this(context);
        setUser(userpic, name);
    }

    public void setUser(String username, int backgroundColor, int textColor) {
        mUsername = username == null ? "" : username;
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
        mPaintsInvalidated = false;
        invalidateSelf();
    }

    public void setUser(Userpic userpic, String name) {
        mUsername = name;
        mTextColor = userpic.defaultColors.getTextColor();
        mBackgroundColor = userpic.defaultColors.getBackgroundColor();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mPaintsInvalidated) {
            invalidatePaints();
        }

        if (getOpacity() == 0) return;

        Rect bounds = getBounds();

        float diameter = Math.min(bounds.width(), bounds.height());

        canvas.drawCircle(bounds.centerX(), bounds.centerY(), diameter / 2.0f, mBackgroundPaint);

        Rect textBounds = new Rect();
        String text = String.valueOf(getUsernameFirstLettter());
        mTextPaint.getTextBounds(text, 0, 1, textBounds);

        canvas.drawText(String.valueOf(getUsernameFirstLettter()),
                bounds.centerX() + textBounds.left *0.4f,
                bounds.centerY() - textBounds.bottom + textBounds.height() / 2f,
                mTextPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            mBackgroundPaint.setAlpha(alpha);
            mTextPaint.setAlpha(mAlpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mBackgroundPaint != null) {
            mBackgroundPaint.setColorFilter(cf);
            invalidateSelf();
        }
    }

    @Override
    public int getOpacity() {
        return mBackgroundPaint.getAlpha() < 255 ? PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    private char getUsernameFirstLettter() {
        if (TextUtils.isEmpty(mUsername)) return ' ';
        return Character.toUpperCase(mUsername.charAt(0));
    }

    private void invalidatePaints() {
        mBackgroundPaint.setColor(Color.argb(mAlpha, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor)));
        mBackgroundPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.argb(mAlpha, Color.red(mTextColor), Color.green(mTextColor), Color.blue(mTextColor)));
        mTextPaint.setTypeface(mMainFont);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        float textSize = TEXT_SIZE;
        mTextPaint.setTextSize(textSize);

        mPaintsInvalidated = true;
    }

    @Override
    public int getIntrinsicWidth() {
        return AVATAR_DIAMETER;
    }

    @Override
    public int getIntrinsicHeight() {
        return AVATAR_DIAMETER;
    }
}
