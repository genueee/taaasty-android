package ru.taaasty.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TextView с подсказкой из ContentDescription
 */
public class HintedTextView extends TextView {
    private boolean textViewInitialized;
    private OnLongClickListener mOnLongClickListener;

    public HintedTextView(Context context)
    {
        super(context);
    }

    public HintedTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initTextView(context, attrs);
    }

    public HintedTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initTextView(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public HintedTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initTextView(context, attrs);
    }

    private void initTextView(Context context, AttributeSet attrs) {
        super.setOnLongClickListener(mOurOnLongClickListener);
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l)
    {
        if (l == mOurOnLongClickListener)
        {
            super.setOnLongClickListener(l);
            return;
        }

        mOnLongClickListener = l;
    }

    private final OnLongClickListener mOurOnLongClickListener = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            if (mOnLongClickListener != null) {
                if (!mOnLongClickListener.onLongClick(v)) handleLongClick();
            } else {
                handleLongClick();
            }
            return true;
        }
    };

    private void handleLongClick()
    {
        CharSequence contentDesc = getContentDescription();
        if (!TextUtils.isEmpty(contentDesc))
        {
            int[] pos = new int[2];
            getLocationInWindow(pos);

            Resources r = getResources();
            int dy = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, r.getDisplayMetrics()) + 0.5f);

            Toast t = Toast.makeText(getContext(), contentDesc, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.TOP | Gravity.LEFT, pos[0] - ((contentDesc.length() / 2) * 12), pos[1] - dy);
            t.show();
        }
    }
}
