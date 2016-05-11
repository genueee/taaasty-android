package ru.taaasty.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.util.TypedValue;

import java.util.Locale;

import ru.taaasty.R;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.FontManager;

/**
 * Created by alexey on 22.04.16.
 */
public class DefaultConversationIconDrawable extends GradientDrawable {

    public static final int TEXT_SIZE = 26;
    public static final int TEXT_SIZE_ANONYMOUS = 34;
    public static final int ICON_SIZE = 26;
    public static final int DEFAULT_TEXT_COLOR = 0x509e9e9e;
    public static final int DEFAULT_STROKE_COLOR = 0x509e9e9e;
    public static final int DEFAULT_BACKGROUND_COLOR = 0xffeaebed;
    private final Paint mTextPaint;

    private int mTextColor;
    private int mAlpha;

    private String mText;

    private final Typeface mMainFont;

    private float mTextSize;

    private Drawable mIcon;

    private int mIconSize;

    public static Drawable create(Context context, Conversation conversation) {
        if (conversation == null) {
            return new DefaultConversationIconDrawable(context, null, false, 0, DEFAULT_TEXT_COLOR, DEFAULT_BACKGROUND_COLOR,DEFAULT_STROKE_COLOR);
        } else if (conversation.getType() == Conversation.Type.PUBLIC) {
            return createPublicPost(context, (PublicConversation) conversation);
        }

        String title = ConversationHelper.getInstance().getTitleWithoutUserPrefix(conversation, context);
        String iconText;
        if (TextUtils.isEmpty(title)) {
            iconText = null;
        } else {
            iconText = String.valueOf(title.charAt(0)).toUpperCase(Locale.getDefault());
        }
        int avatarBackgroundColor = calculateColorFromId(conversation.getId());
        return new DefaultConversationIconDrawable(context, iconText, false, 0,avatarBackgroundColor , 0xffffffff,avatarBackgroundColor);
    }

    public static Drawable createPublicPost(Context context, PublicConversation conversation) {
        if (conversation.isAnonymous()) {
            return createAnonymous(context, conversation);
        } else {
            return new DefaultConversationIconDrawable(context,
                    null, false, R.drawable.ic_tabbar_live_normal, DEFAULT_BACKGROUND_COLOR, DEFAULT_TEXT_COLOR,DEFAULT_STROKE_COLOR);
        }
    }

    public static Drawable createAnonymous(Context context, Conversation conversation) {
        return new DefaultConversationIconDrawable(context,
                String.valueOf('\ue002'),
                true,
                0,
                DEFAULT_BACKGROUND_COLOR,
                DEFAULT_TEXT_COLOR,
                DEFAULT_STROKE_COLOR
        );
    }

    private static int calculateColorFromId(long id) {
        int colorNumber = (int) (id % 10);
        switch (colorNumber) {
            case 0:
                return Color.rgb((int) (255 * 0.925), (int) (255 * 0.416), (int) (255 * 0.369));
            case 1:
                return Color.rgb((int) (255 * 1), (int) (255 * 0.686), (int) (255 * 0.318));
            case 2:
                return Color.rgb((int) (255 * 0.957), (int) (255 * 0.827), (int) (255 * 0.278));
            case 3:
                return Color.rgb((int) (255 * 0.494), (int) (255 * 0.827), (int) (255 * 0.129));
            case 4:
                return Color.rgb((int) (255 * 0.525), (int) (255 * 0.914), (int) (255 * 0.647));
            case 5:
                return Color.rgb((int) (255 * 0.400), (int) (255 * 0.682), (int) (255 * 0.894));
            case 6:
                return Color.rgb((int) (255 * 0.290), (int) (255 * 0.565), (int) (255 * 0.886));
            case 7:
                return Color.rgb((int) (255 * 0.565), (int) (255 * 0.075), (int) (255 * 0.996));
            case 8:
                return Color.rgb((int) (255 * 0.694), (int) (255 * 0.365), (int) (255 * 0.922));
            case 9:
                return Color.rgb((int) (255 * 0.780), (int) (255 * 0.541), (int) (255 * 0.882));
        }
        return 0;
    }

    DefaultConversationIconDrawable(Context context, String text,
                                    boolean isAnonymous,
                                    @DrawableRes int iconResId,
                                    int backGroundColor,
                                    int textColor,
                                    int strokeColor) {
        super();
        Resources resources = context.getResources();


        setShape(GradientDrawable.RECTANGLE);
        setCornerRadius(context.getResources().getDimension(R.dimen.group_avatar_corner_radius));
        setStroke((int) (1 * resources.getDisplayMetrics().density + 0.5f), strokeColor);
        setColor(backGroundColor);

        mAlpha = 0xff;
        mTextColor = textColor;
        mTextPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mText = text;
        mIconSize = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                ICON_SIZE, resources.getDisplayMetrics()));

        if (isAnonymous) {
            mMainFont = FontManager.getInstance(context).getFontAnonymous();
            mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    TEXT_SIZE_ANONYMOUS, resources.getDisplayMetrics());
        } else {
            mMainFont = FontManager.getInstance(context).getMainFont();
            mTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    TEXT_SIZE, resources.getDisplayMetrics());
        }

        if (iconResId != 0) {
            mIcon = ResourcesCompat.getDrawable(resources, iconResId, null).mutate();
            mIcon.setColorFilter(mTextColor, PorterDuff.Mode.SRC_ATOP);
        }

        invalidatePaints();
    }

        private final Rect mTextBounds = new Rect();

        @Override
        public void draw (Canvas canvas){
            super.draw(canvas);

            if (getOpacity() == 0) return;

            Rect bounds = getBounds();

            // Text
            if (!TextUtils.isEmpty(mText)) {
                mTextPaint.getTextBounds(mText, 0, 1, mTextBounds);
                canvas.drawText(mText,
                        bounds.centerX() + mTextBounds.left * 0.4f,
                        bounds.centerY() - mTextBounds.bottom + mTextBounds.height() / 2f,
                        mTextPaint);
            }

            // Icon
            if (mIcon != null) {
                mIcon.draw(canvas);
            }
        }

        @Override
        public void setAlpha ( int alpha){
            super.setAlpha(alpha);
            if (mAlpha != alpha) {
                mAlpha = alpha;
                mTextPaint.setAlpha(mAlpha);
                mIcon.setAlpha(alpha);
                invalidateSelf();
            }
        }

        @Override
        public void setColorFilter (ColorFilter colorFilter){
            super.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity () {
            return super.getOpacity();
        }

        @Override
        protected void onBoundsChange (Rect r){
            super.onBoundsChange(r);
            if (mIcon != null) {
                int inset = (Math.min(r.width(), r.height()) - mIconSize) / 2;
                if (inset < 0) inset = 0;

                mIcon.setBounds(
                        r.left + inset,
                        r.top + inset,
                        r.right - inset,
                        r.bottom - inset
                );
            }
        }

    private void invalidatePaints() {
        mTextPaint.setARGB(mAlpha, Color.red(mTextColor), Color.green(mTextColor), Color.blue(mTextColor));
        mTextPaint.setTypeface(mMainFont);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(mTextSize);
    }

    @Override
    public int getIntrinsicWidth() {
        return super.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return super.getIntrinsicHeight();
    }

}
