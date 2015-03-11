package ru.taaasty.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import ru.taaasty.R;
import ru.taaasty.model.Entry;

public class CreatePostButtons extends LinearLayout {

    private int mActivatedElement;

    private View mPrivatePostIndicator;

    private View mWithVotingIndicator;

    private LinearLayout mButtonsList;

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
        mButtonsList = (LinearLayout)findViewById(R.id.buttons_list);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        int childCount = mButtonsList.getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = mButtonsList.getChildAt(i);
            v.setOnClickListener(mOnClickListener);
        }

        mWithVotingIndicator.setOnClickListener(mOnClickListener);
        mPrivatePostIndicator.setOnClickListener(mOnClickListener);

        refreshActivated();
    }

    private final Rect visibleRect = new Rect();

    public void setActivated(@IdRes int activated) {
        mActivatedElement = activated;
        refreshActivated();

        View activatedView = getActivatedView();
        if (activatedView != null) {
            MarginLayoutParams lp = (MarginLayoutParams)activatedView.getLayoutParams();
            visibleRect.left = activatedView.getLeft() - lp.leftMargin * 2;
            visibleRect.right = activatedView.getRight() + lp.rightMargin * 2;
            visibleRect.top = activatedView.getTop();
            visibleRect.bottom = activatedView.getBottom();
            if (visibleRect.width() > 0) activatedView.requestRectangleOnScreen(visibleRect);
        }
    }

    public int getActivatedViewId() {
        return mActivatedElement;
    }

    public void setOnItemClickListener(onCreatePostButtonsListener listener) {
        mListener = listener;
    }

    public void refreshActivated() {
        int childCount = mButtonsList.getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = mButtonsList.getChildAt(i);
            v.setActivated(mActivatedElement == v.getId());
        }
    }

    @Nullable
    private View getActivatedView() {
        int childCount = mButtonsList.getChildCount();
        for (int i=0; i < childCount; ++i) {
            View v = mButtonsList.getChildAt(i);
            if (mActivatedElement == v.getId()) return v;
        }
        return null;
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
        if (mWithVotingIndicator.getVisibility() == View.VISIBLE) return;
        ObjectAnimator anim = ObjectAnimator.ofFloat(mWithVotingIndicator, "alpha", 0f, 1f);
        anim.setDuration(getResources().getInteger(R.integer.shortAnimTime));
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mWithVotingIndicator.setVisibility(View.VISIBLE);
                mWithVotingIndicator.setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mWithVotingIndicator.setAlpha(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mWithVotingIndicator.setAlpha(1f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
    }

    private void hideVotingIndicator() {
        if (mWithVotingIndicator.getVisibility() != View.VISIBLE) return;
        ObjectAnimator anim = ObjectAnimator.ofFloat(mWithVotingIndicator, "alpha", 1f, 0f);
        anim.setDuration(getResources().getInteger(R.integer.shortAnimTime));
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mWithVotingIndicator.setVisibility(View.INVISIBLE);
                mWithVotingIndicator.setAlpha(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mWithVotingIndicator.setVisibility(View.INVISIBLE);
                mWithVotingIndicator.setAlpha(1f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
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
        public void onCreatePostButtonClicked(View v);
    }
}
