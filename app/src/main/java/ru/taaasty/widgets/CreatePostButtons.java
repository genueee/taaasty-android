package ru.taaasty.widgets;

import android.content.Context;
import android.support.annotation.IdRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import ru.taaasty.R;

public class CreatePostButtons extends LinearLayout {

    private int mActivatedElement;

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
            v.setActivated(mActivatedElement == v.getId());
        }
    }

    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onCreatePostButtonClicked(v);
        }
    };

    public interface onCreatePostButtonsListener {
        public void onCreatePostButtonClicked(View v);
    }
}
