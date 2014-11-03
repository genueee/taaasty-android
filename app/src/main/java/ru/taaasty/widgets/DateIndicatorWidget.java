package ru.taaasty.widgets;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.taaasty.R;

/**
 * Created by alexey on 03.11.14.
 */
public class DateIndicatorWidget extends ViewSwitcher {

    private boolean mInflated;

    private SimpleDateFormat mDayFormat;
    private SimpleDateFormat mDatetimeFormat;

    private DateBackgroundDrawable mBackgroundDrawable;

    private Date mDate;

    private View mRootView0;
    private TextView mDayView0;
    private TextView mDatetimeView0;

    private View mRootView1;
    private TextView mDayView1;
    private TextView mDatetimeView1;

    private Animation mScrollDownInAnim;
    private Animation mScrollDownOutAnim;
    private Animation mScrollUpInAnim;
    private Animation mScrollUpOutAnim;

    public DateIndicatorWidget(Context context) {
        super(context, null);
        initDateIndicator(getContext());
    }

    public DateIndicatorWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDateIndicator(getContext());
    }

    private void initDateIndicator(Context context) {
        if (mInflated) return;
        else mInflated = true;

        mRootView0 = LayoutInflater.from(context).inflate(R.layout.date_indicator, this, false);
        mRootView1 = LayoutInflater.from(context).inflate(R.layout.date_indicator, this, false);

        mBackgroundDrawable = new DateBackgroundDrawable(context.getResources());
        setBackgroundDrawable(mBackgroundDrawable);
        mDayView0 = (TextView) mRootView0.findViewById(R.id.date_indicator_day);
        mDatetimeView0 = (TextView) mRootView0.findViewById(R.id.date_indicator_datetime);
        mDayView1 = (TextView) mRootView1.findViewById(R.id.date_indicator_day);
        mDatetimeView1 = (TextView) mRootView1.findViewById(R.id.date_indicator_datetime);

        mDayView0.setTypeface(Typeface.MONOSPACE);
        mDatetimeView0.setTypeface(Typeface.MONOSPACE);
        mDayView1.setTypeface(Typeface.MONOSPACE);
        mDatetimeView1.setTypeface(Typeface.MONOSPACE);

        addView(mRootView0);
        addView(mRootView1);

        if (isInEditMode()) {
            mDayFormat = new SimpleDateFormat("dd", Locale.US);
            mDatetimeFormat = new SimpleDateFormat("MMM\n\nHH:mm", Locale.US);
            setDate(new Date());
        } else {
            mDayFormat = new SimpleDateFormat("dd", Locale.getDefault());
            mDatetimeFormat = new SimpleDateFormat("MMMM\ncc\nHH:mm", Locale.getDefault());
        }

        mScrollDownInAnim = AnimationUtils.loadAnimation(context, R.anim.scroll_down_in);
        mScrollDownOutAnim = AnimationUtils.loadAnimation(context, R.anim.scroll_down_out);

        mScrollUpInAnim = AnimationUtils.loadAnimation(context, R.anim.scroll_up_in);
        mScrollUpOutAnim = AnimationUtils.loadAnimation(context, R.anim.scroll_up_out);

        setInAnimation(mScrollUpInAnim);
        setOutAnimation(mScrollUpOutAnim);
    }


    public void setDate(@Nullable Date date) {
        setDate(date, true);
    }

    public void setDate(@Nullable Date date, boolean animScrollUp) {
        CharSequence newDay;
        CharSequence newDatetime;
        boolean scrollUp;

        if (date != null && date.equals(mDate)) return;

        if (date == null) {
            newDay = null;
            newDatetime = null;
        } else {
            newDay = mDayFormat.format(date);
            newDatetime = mDatetimeFormat.format(date).toLowerCase();
        }

        if (getNextView() == mRootView0) {
            mDayView0.setText(newDay);
            mDatetimeView0.setText(newDatetime);
        } else {
            mDayView1.setText(newDay);
            mDatetimeView1.setText(newDatetime);
        }

        if (animScrollUp) {
            setInAnimation(mScrollUpInAnim);
            setOutAnimation(mScrollUpOutAnim);
        } else {
            setInAnimation(mScrollDownInAnim);
            setOutAnimation(mScrollDownOutAnim);
        }

        showNext();

        mDate = date;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed)
            Log.v("DateIndicatorWidget", "onLayout() " + left + " " + top + " " + right + " " + bottom + " ");
    }


    private Runnable mUpdateBackgroundRunnable = new Runnable() {
        @Override
        public void run() {
            setBackground(mBackgroundDrawable);
        }
    };

}
