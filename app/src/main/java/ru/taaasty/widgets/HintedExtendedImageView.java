package ru.taaasty.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

/**
 * Extended image view to show the content description in a Toast view when
 * the author long presses.
 *
 * Note: `android:contentDescription` must be set in order for the toast to
 * work
 *
 * @author Callum Taylor <http://callumtaylor.net>
 */
public class HintedExtendedImageView extends ExtendedImageView implements OnLongClickListener
{
    private OnLongClickListener mOnLongClickListener;

    public HintedExtendedImageView(Context context)
    {
        super(context);

        setOnLongClickListener(this);
    }

    public HintedExtendedImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        setOnLongClickListener(this);
    }

    @Override public void setOnLongClickListener(OnLongClickListener l)
    {
        if (l == this)
        {
            super.setOnLongClickListener(l);
            return;
        }

        mOnLongClickListener = l;
    }

    @Override public boolean onLongClick(View v)
    {
        if (mOnLongClickListener != null)
        {
            if (!mOnLongClickListener.onLongClick(v))
            {
                handleLongClick();
                return true;
            }
        }
        else
        {
            handleLongClick();
            return true;
        }

        return false;
    }

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
