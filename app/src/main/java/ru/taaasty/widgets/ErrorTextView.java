package ru.taaasty.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class ErrorTextView extends TextView {
    public ErrorTextView(Context context) {
        this(context, null);
    }

    public ErrorTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ErrorTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public void setError(String error) {
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
    }

    public void hideErrorView() {
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
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        Animation fadeInAnim = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);
        fadeInAnim.setDuration(shortAnimTime);
        fadeInAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        startAnimation(fadeInAnim);
    }

    private void updateErrorView(final String newText) {
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

        startAnimation(fadeOutAnim);
    }

}
