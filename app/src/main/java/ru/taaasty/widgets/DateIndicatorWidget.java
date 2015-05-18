package ru.taaasty.widgets;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
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
import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 03.11.14.
 */
public class DateIndicatorWidget extends ViewSwitcher {

    private boolean mInflated;

    private SimpleDateFormat mDayFormat;
    private SimpleDateFormat mDatetimeFormat;

    private Date mDate;

    private View mRootView0;
    private TextView mDayView0;
    private TextView mDatetimeView0;

    private TextView mDayView1;
    private TextView mDatetimeView1;

    private Animation mScrollDownInAnim;
    private Animation mScrollDownOutAnim;
    private Animation mScrollUpInAnim;
    private Animation mScrollUpOutAnim;

    private boolean mAuthoShow = true;

    public DateIndicatorWidget(Context context) {
        super(context, null);
        initDateIndicator(getContext());
    }

    public DateIndicatorWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDateIndicator(getContext());
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.date = this.mDate;
        ss.whichState = getDisplayedChild();
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState)state;
        super.onRestoreInstanceState(ss.getSuperState());

        this.mDate = ss.date;
    }

    private void initDateIndicator(Context context) {
        if (mInflated) return;
        else mInflated = true;

        mRootView0 = LayoutInflater.from(context).inflate(R.layout.date_indicator, this, false);
        View rootView1 = LayoutInflater.from(context).inflate(R.layout.date_indicator, this, false);

        setBackgroundDrawable(new DateBackgroundDrawable(context.getResources()));
        mDayView0 = (TextView) mRootView0.findViewById(R.id.date_indicator_day);
        mDatetimeView0 = (TextView) mRootView0.findViewById(R.id.date_indicator_datetime);
        mDayView1 = (TextView) rootView1.findViewById(R.id.date_indicator_day);
        mDatetimeView1 = (TextView) rootView1.findViewById(R.id.date_indicator_datetime);

        mDayView0.setTypeface(Typeface.MONOSPACE);
        mDatetimeView0.setTypeface(Typeface.MONOSPACE);
        mDayView1.setTypeface(Typeface.MONOSPACE);
        mDatetimeView1.setTypeface(Typeface.MONOSPACE);

        addView(mRootView0);
        addView(rootView1);

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


    public void setAuthoShow(boolean show) {
        mAuthoShow = show;
    }

    public void setDate(@Nullable Date date) {
        setDate(date, true);
    }

    public void setDate(@Nullable Date date, boolean animScrollUp) {
        CharSequence newDay;
        CharSequence newDatetime;

        if (Objects.equals(date, mDate)) return;

        if (date == null) {
            newDay = null;
            newDatetime = null;
        } else {
            newDay = mDayFormat.format(date);
            newDatetime = mDatetimeFormat.format(date).toLowerCase(Locale.getDefault());
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

        if (mAuthoShow && (getVisibility() != View.VISIBLE)) showIndicatorSmoothly();
    }

    private void showIndicatorSmoothly() {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f)
                .setDuration(getResources().getInteger(R.integer.longAnimTime));
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setVisibility(View.VISIBLE);
                setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setAlpha(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                setAlpha(1f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    static class SavedState extends BaseSavedState {
        int whichState;
        Date date;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.whichState = in.readInt();
            Long dateInt = (Long)in.readValue(Long.class.getClassLoader());
            this.date = dateInt == null ? null : new Date(dateInt);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(whichState);
            out.writeValue(date == null ? (Long)null : Long.valueOf(date.getTime()));
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
