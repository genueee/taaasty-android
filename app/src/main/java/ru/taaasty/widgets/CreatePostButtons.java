package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IdRes;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;

public class CreatePostButtons extends LinearLayout {

    private int mActivatedElement;

    private View mPrivatePostIndicator;

    private View mWithVotingIndicator;

    @Entry.EntryPrivacy
    private String mPrivacy = Entry.PRIVACY_PUBLIC;

    private onCreatePostButtonsListener mListener;

    public CreatePostButtons(Context context) {
        this(context, null);
    }

    public CreatePostButtons(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public CreatePostButtons(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        inflate(context, R.layout.create_post_buttons, this);
        mActivatedElement = View.NO_ID;
        init();
    }

    private void init() {
        if (mPrivatePostIndicator != null) return;
        mPrivatePostIndicator = findViewById(R.id.private_post_indicator);
        mWithVotingIndicator = findViewById(R.id.is_with_voting_indicator);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int childCount = getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = getChildAt(i);
            v.setOnClickListener(mOnClickListener);
        }

        refreshActivated();
    }

    private final Rect visibleRect = new Rect();

    public void setActivated(@IdRes int activated) {
        mActivatedElement = activated;
        refreshActivated();
    }

    public int getActivatedViewId() {
        return mActivatedElement;
    }

    public void setOnItemClickListener(onCreatePostButtonsListener listener) {
        mListener = listener;
    }

    public void refreshActivated() {
        int childCount = getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = getChildAt(i);
            if (v == mPrivatePostIndicator || v == mWithVotingIndicator) continue;
            v.setActivated(mActivatedElement == v.getId());
        }
    }

    public void setPrivacy(@Entry.EntryPrivacy String privacy) {
        mPrivacy = privacy;
        switch (privacy) {
            case Entry.PRIVACY_PRIVATE:
                mPrivatePostIndicator.setActivated(true);
                hideVotingIndicator();
                break;
            case Entry.PRIVACY_PUBLIC_WITH_VOTING:
                mPrivatePostIndicator.setActivated(false);
                mWithVotingIndicator.setActivated(true);
                showVotingIndicator();
                break;
            case Entry.PRIVACY_PUBLIC:
                mPrivatePostIndicator.setActivated(false);
                mWithVotingIndicator.setActivated(false);
                showVotingIndicator();
                break;
            default:
                throw new IllegalArgumentException("Unknown privacy " + privacy);
        }
    }

    private void showVotingIndicator() {
        if (mWithVotingIndicator.getVisibility() != View.VISIBLE) {
            mWithVotingIndicator.setVisibility(View.VISIBLE);
            mWithVotingIndicator.setAlpha(0f);
        }
        ViewCompat.animate(mWithVotingIndicator)
                .alpha(1f)
                .setDuration(getResources().getInteger(R.integer.shortAnimTime))
                .withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {
                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mWithVotingIndicator.setAlpha(1f);
                    }

                    @Override
                    public void onAnimationCancel(View view) {

                    }
                })
                .start();
    }

    private void hideVotingIndicator() {
        if (mWithVotingIndicator.getVisibility() != View.VISIBLE) return;
        ViewCompat.animate(mWithVotingIndicator)
                .alpha(0f)
                .setDuration(getResources().getInteger(R.integer.shortAnimTime))
                .withLayer()
                .setListener(new ViewPropertyAnimatorListener() {
                    @Override
                    public void onAnimationStart(View view) {

                    }

                    @Override
                    public void onAnimationEnd(View view) {
                        mWithVotingIndicator.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(View view) {

                    }
                })
                .start();
    }

    public @Entry.EntryPrivacy String getPrivacy() {
        if (mPrivatePostIndicator.isActivated()) {
            return Entry.PRIVACY_PRIVATE;
        }

        if (mWithVotingIndicator.isActivated()) {
            return Entry.PRIVACY_PUBLIC_WITH_VOTING;
        }

        return Entry.PRIVACY_PUBLIC;
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.private_post_indicator:
                    if (v.isActivated()) {
                        setPrivacy(Entry.PRIVACY_PUBLIC);
                    } else {
                        setPrivacy(Entry.PRIVACY_PRIVATE);
                    }
                    break;
                case R.id.is_with_voting_indicator:
                    v.setActivated(!v.isActivated());
                    break;
                default:
                    if (mListener != null) mListener.onCreatePostButtonClicked(v);
            }

        }
    };

    public interface onCreatePostButtonsListener {
        void onCreatePostButtonClicked(View v);
    }
}
