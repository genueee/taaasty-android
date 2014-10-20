package ru.taaasty.ui;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;

public class CustomTypefaceSpan extends TextAppearanceSpan {
    private final Typeface mTypeface;

    public CustomTypefaceSpan(Context context, int appearance, Typeface type) {
        this(context, appearance, -1, type);
    }

    public CustomTypefaceSpan(Context context, int appearance, int colorList, Typeface type) {
        super(context, appearance, colorList);
        mTypeface = type;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        super.updateDrawState(ds);
        applyCustomTypeFace(ds, mTypeface);
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
        super.updateMeasureState(paint);
        applyCustomTypeFace(paint, mTypeface);
    }

    private static void applyCustomTypeFace(Paint paint, Typeface tf) {
        Typeface old = paint.getTypeface();
        if (old != tf) paint.setTypeface(tf);
    }
}