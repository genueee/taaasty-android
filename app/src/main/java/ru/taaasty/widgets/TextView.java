package ru.taaasty.widgets;

import android.content.Context;
import android.util.AttributeSet;

import ru.taaasty.utils.FontManager;


public class TextView extends android.widget.TextView {

    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setTypeface(FontManager.getInstance(context).getMainFont());
    }

}
