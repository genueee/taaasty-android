package ru.taaasty.widgets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;

import ru.taaasty.R;

public class RelativeDateTextSwitcher extends TextSwitcher {
    private static final String TAG = "RelativeDateTextSwtcher";
    private static final boolean DBG = false;

    private static final long UPDATE_PERIOD_MS = 10 * DateUtils.SECOND_IN_MILLIS;

    private long mDateMs;

    private boolean initialized;

    private Handler mUpdateHandler = null;

    private final boolean mIsRelativeDateTimeFormat;

    private final int mRelativeFormatFlags;

    private CharSequence mTextPrefix;

    public RelativeDateTextSwitcher(Context context) {
        this(context, null);
    }

    public RelativeDateTextSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.RelativeDateTextSwitcher, 0, 0);
            try {
                mIsRelativeDateTimeFormat = typedArray.getBoolean(R.styleable.RelativeDateTextSwitcher_isRelativeDateTimeFormat, false);
                mRelativeFormatFlags = typedArray.getInt(R.styleable.RelativeDateTextSwitcher_dateFormatFlags, 0);
            } finally {
                typedArray.recycle();
            }
        } else {
            mIsRelativeDateTimeFormat = false;
            mRelativeFormatFlags = 0;
        }
    }

    private void init() {
        if (initialized) return;
        initialized = true;
        Animation in = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        Animation out = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        in.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        out.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
        setInAnimation(in);
        setOutAnimation(out);
    }

    @Nullable
    public CharSequence getText() {
        View currentView = getCurrentView();
        if (currentView != null) {
            return ((TextView)currentView).getText();
        } else {
            return null;
        }
    }

    public void setText(int resId) {
        setText(getResources().getText(resId));
    }

    public void setText(CharSequence text) {
        if (!TextUtils.equals(getText(), text)) {
            super.setText(text);
        }
    }

    public void setTextColor(ColorStateList color) {
        ((TextView)getChildAt(0)).setTextColor(color);
        ((TextView)getChildAt(1)).setTextColor(color);
    }

    public void setTypeface(Typeface typefate) {
        ((TextView)getChildAt(0)).setTypeface(typefate);
        ((TextView)getChildAt(1)).setTypeface(typefate);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(@DrawableRes int left, @DrawableRes int top,
                                                        @DrawableRes int right, @DrawableRes int bottom) {
        ((TextView)getChildAt(0)).setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        ((TextView)getChildAt(1)).setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left, Drawable top, Drawable right, Drawable bottom) {
        ((TextView)getChildAt(0)).setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
        ((TextView)getChildAt(1)).setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom);
    }

    public void setRelativeDate(long dateMs) {
        mDateMs = dateMs;
        CharSequence text = getRelativeDate(dateMs);
        setCurrentText(text);
        startDateUpdate();
    }

    public void invalidateRelativeDate() {
        mDateMs = 0;
        stopDateUpdate();
    }

    private CharSequence getRelativeDate(long dateMs) {
        long now = System.currentTimeMillis();
        // Коррекция на разницу часов серверных и устройства.
        // Округляем время до минуты, не пишем "0 минут назад"
        if (now - DateUtils.MINUTE_IN_MILLIS < dateMs) dateMs = now - DateUtils.MINUTE_IN_MILLIS;
        if (mIsRelativeDateTimeFormat) {
            return DateUtils.getRelativeDateTimeString(getContext(), dateMs,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS,
                    mRelativeFormatFlags);
        } else {
            long duration = Math.abs(now - dateMs);
            if (duration >= DateUtils.WEEK_IN_MILLIS) {
                return DateUtils.formatDateTime(getContext(), dateMs, mRelativeFormatFlags);
            } else {
                return DateUtils.getRelativeTimeSpanString(dateMs, now, DateUtils.MINUTE_IN_MILLIS, mRelativeFormatFlags);
            }
        }

    }

    private void updateDate() {
        if (DBG) Log.v(TAG, "updateDate()");
        setText(getRelativeDate(mDateMs));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mDateMs != 0) {
            updateDate();
            startDateUpdate();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopDateUpdate();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        if (changedView != this) return;

        if (visibility == GONE || visibility == INVISIBLE) {
            stopDateUpdate();
        } else if (mUpdateHandler == null) {
            startDateUpdate();
        }
    }

    @Override
    public void onWindowFocusChanged (boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (DBG) Log.v(TAG, "onWindowFocusChanged " + hasWindowFocus);

        if (mDateMs == 0) return;
        if (hasWindowFocus && mUpdateHandler == null) {
            startDateUpdate();
        } else {
            stopDateUpdate();
        }
    }

    private void stopDateUpdate() {
        if (mUpdateHandler == null) return;
        if (DBG) Log.v(TAG, "stopDateUpdate()");
        mUpdateHandler.removeCallbacksAndMessages(null);
        mUpdateHandler = null;
    }

    private void startDateUpdate() {
        if (DBG) Log.v(TAG, "startDateUpdate()");
        if (mUpdateHandler == null) {
            mUpdateHandler = new Handler();
        } else {
            mUpdateHandler.removeCallbacksAndMessages(null);
        }
        mUpdateHandler.postDelayed(mUpdateDateRunnable, UPDATE_PERIOD_MS);
    }

    private Runnable mUpdateDateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getContext() == null || mUpdateHandler == null || mDateMs == 0) {
                mUpdateHandler = null;
                return;
            }
            updateDate();
            mUpdateHandler.postDelayed(this, UPDATE_PERIOD_MS);
        }
    };
}
