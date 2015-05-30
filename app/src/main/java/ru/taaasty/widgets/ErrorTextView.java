package ru.taaasty.widgets;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import ru.taaasty.TaaastyApplication;

public class ErrorTextView extends TextView {

    private static final int HIDE_DELAY_MIN_MS = 5000;

    private static final int HIDE_DELAY_MS_PER_CHARACTER = 200;

    private Handler mHideErrorViewDelayerHandler;

    public ErrorTextView(Context context) {
        this(context, null);
    }

    public ErrorTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ErrorTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mHideErrorViewDelayerHandler = new Handler();
    }

    /**
     * Установка ошибки
     * @param error Текст ошибки
     * @param ex Исключение, если есть, для аналитики
     */
    public void setError(CharSequence error, @Nullable Throwable exception) {
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);
        if (TextUtils.isEmpty(error)) {
            hideErrorView();
        } else {
            if (getVisibility() == View.VISIBLE) {
                updateErrorView(error);
            } else {
                setText(error);
                showErrorView();
            }
        }

        if (exception != null) {
            if (getContext().getApplicationContext() instanceof TaaastyApplication) {
                Tracker t = ((TaaastyApplication) getContext().getApplicationContext()).getTracker();
                t.send(new HitBuilders.ExceptionBuilder()
                                .setDescription(
                                        new StandardExceptionParser(getContext(), null)
                                                .getDescription(Thread.currentThread().getName(), exception))
                                .setFatal(false)
                                .build()
                );
            }
        }
    }

    public void hideErrorView() {
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);
        if (getVisibility() != View.VISIBLE) return;
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        Animation fadeOutAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        fadeOutAnim.setDuration(shortAnimTime);
        fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        startAnimation(fadeOutAnim);
    }

    public void showErrorView() {
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        Animation fadeInAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        fadeInAnim.setDuration(shortAnimTime);
        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mHideErrorViewDelayerHandler.postDelayed(mHideErrorViewDelayed, getHideDelay());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        startAnimation(fadeInAnim);
    }

    private int getHideDelay() {
        return HIDE_DELAY_MIN_MS + getText().length() * HIDE_DELAY_MS_PER_CHARACTER;
    }

    private void updateErrorView(final CharSequence newText) {
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);

        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        final Animation fadeOutAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
        final Animation fadeInAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);

        fadeOutAnim.setDuration(shortAnimTime / 2);
        fadeInAnim.setDuration(shortAnimTime / 2);

        fadeOutAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setText(newText);
                startAnimation(fadeInAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mHideErrorViewDelayerHandler.postDelayed(mHideErrorViewDelayed, getHideDelay());
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        startAnimation(fadeOutAnim);
    }

    private Runnable mHideErrorViewDelayed = new Runnable() {
        @Override
        public void run() {
            hideErrorView();
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHideErrorViewDelayerHandler.removeCallbacks(mHideErrorViewDelayed);
    }
}
