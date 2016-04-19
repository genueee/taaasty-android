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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Random;

import ru.taaasty.rest.model.Userpic;
import ru.taaasty.utils.FontManager;

/**
 * Created by alexey on 31.07.14.
 */
public class DefaultUserpicDrawable extends Drawable {
    public static final int AVATAR_DIAMETER = 128;
    public static final int TEXT_SIZE = 42;
    public static final int TEXT_SIZE_ANONYMOUS = 84;
    private final Paint mBackgroundPaint;
    private final Paint mTextPaint;

    private final Typeface mMainFont;

    private final boolean mIsAnonymous;

    private int mBackgroundColor;
    private int mTextColor;
    private int mAlpha;
    private char mCharacter;
    private Rect mBounds;

    private boolean mPaintsInvalidated;

    private static final Random sRandom = new Random();

    public static DefaultUserpicDrawable createAnonymousDefault(Context context) {
        return new DefaultUserpicDrawable(context, String.valueOf('\ue002'), 0xff7648DE, Color.WHITE, true);
    }

    public static DefaultUserpicDrawable create(Context context, @Nullable Userpic userpic, String name) {
        int textColor;
        int background;
        boolean isAnonymous = false;
        Character character;

        // character
        if (userpic != null && !TextUtils.isEmpty(userpic.symbol)) {
            character = userpic.symbol.charAt(0);
            if (!Userpic.KIND_ANONYMOUS.equals(userpic.kind)) {
                character = Character.toUpperCase(character);
            }
        } else {
            character = getUsernameFirstLetter(name, false);
        }

        // Is anonymous
        if (character >= 0xe000 && character <= 0xe064) {
            isAnonymous = true;
        } else {
            isAnonymous = false;
        }

        // Colors
        if (userpic != null && userpic.defaultColors != null) {
            textColor = userpic.defaultColors.getTextColor();
            background = userpic.defaultColors.getBackgroundColor();
        } else {
            textColor = Color.WHITE;
            background = isAnonymous ? 0xff7648DE : 0xff44d068;
        }

        return new DefaultUserpicDrawable(context, String.valueOf(character), background, textColor, isAnonymous);
    }

    public DefaultUserpicDrawable(Context context, String username, int backgroundColor, int textColor, boolean isAnonymous) {
        mBackgroundPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mTextPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mAlpha = 255;
        mIsAnonymous = isAnonymous;
        if (mIsAnonymous) {
            mMainFont = FontManager.getInstance(context).getFontAnonymous();
        } else {
            mMainFont = FontManager.getInstance(context).getMainFont();
        }
        setUser(username, backgroundColor, textColor);
    }

    private static char getUsernameFirstLetter(CharSequence username, boolean isAnonymousConversation) {
        if (TextUtils.isEmpty(username)) {
            return isAnonymousConversation ? 0xe002 : ' ';
        }
        return Character.toUpperCase(username.charAt(0));
    }


    private void setUser(String username, int backgroundColor, int textColor) {
        mCharacter = getUsernameFirstLetter(username, false);
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
        mPaintsInvalidated = false;
        invalidateSelf();
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
        String text = String.valueOf(mCharacter);
        mTextPaint.getTextBounds(text, 0, 1, textBounds);

        canvas.drawText(String.valueOf(mCharacter),
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

    private void invalidatePaints() {
        mBackgroundPaint.setColor(Color.argb(mAlpha, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor)));
        mBackgroundPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.argb(mAlpha, Color.red(mTextColor), Color.green(mTextColor), Color.blue(mTextColor)));
        mTextPaint.setTypeface(mMainFont);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        float textSize = mIsAnonymous ? TEXT_SIZE_ANONYMOUS : TEXT_SIZE;
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
