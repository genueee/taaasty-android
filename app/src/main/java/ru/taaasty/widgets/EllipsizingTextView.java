package ru.taaasty.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.util.AttributeSet;

import info.piwai.android.JellyBeanSpanFixTextView;

/**
 *
 */
public class EllipsizingTextView extends JellyBeanSpanFixTextView {
    private static final String ELLIPSIS = "\u2026";

    private boolean isEllipsized;
    private boolean isStale;
    private boolean programmaticChange;
    private CharSequence fullText;
    private int maxLines = -1;
    private float lineSpacingMultiplier = 1.0f;
    private float lineAdditionalVerticalPadding = 0.0f;

    private TextUtils.TruncateAt mEllipsize;

    public EllipsizingTextView(Context context) {
        super(context);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EllipsizingTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setMaxLines(int maxLines) {
        super.setMaxLines(maxLines);
        this.maxLines = maxLines;
        isStale = true;
    }

    public int getMaxLines() {
        return maxLines;
    }

    @Override
    public void setLineSpacing(float add, float mult) {
        this.lineAdditionalVerticalPadding = add;
        this.lineSpacingMultiplier = mult;
        super.setLineSpacing(add, mult);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        if (!programmaticChange) {
            fullText = text;
            isStale = true;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // На android L все работает
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            super.onDraw(canvas);
            return;
        }
        if (mEllipsize != TextUtils.TruncateAt.END) {
            super.onDraw(canvas);
            return;
        }
        if (isStale) {
            super.setEllipsize(null);
            resetText();
        }
        super.onDraw(canvas);
    }


    @SuppressLint("NewApi")
    private void resetText() {
        int maxLines = getMaxLines();
        SpannableStringBuilder result = new SpannableStringBuilder(fullText);

        if (maxLines != -1) {
            Layout layout = createWorkingLayout(result);
            if (layout.getLineCount() > maxLines) {
                SpannableStringBuilder ellipsizedText = new SpannableStringBuilder();
                int end = layout.getLineEnd(maxLines - 1);
                CharSequence workingText = result.subSequence(0, end); // XXX: trim
                ellipsizedText.clear();
                ellipsizedText.append(workingText);
                ellipsizedText.append(ELLIPSIS);
                while (createWorkingLayout(ellipsizedText).getLineCount() > maxLines) {
                    int lastSpace = TextUtils.lastIndexOf(workingText, ' ');
                    if (lastSpace == -1) {
                        break;
                    }
                    end = lastSpace;
                    workingText = result.subSequence(0, lastSpace);
                    ellipsizedText.clear();
                    ellipsizedText.append(workingText);
                    ellipsizedText.append(ELLIPSIS);
                }
                result.replace(end, result.length(), ELLIPSIS);
            }
        }
        if (!result.equals(getText())) {
            programmaticChange = true;
            try {
                setText(result, BufferType.NORMAL);
            } finally {
                programmaticChange = false;
            }
        }
        isStale = false;
    }

    private Layout createWorkingLayout(CharSequence workingText) {
        return new StaticLayout(workingText, getPaint(), getWidth() - getPaddingLeft() - getPaddingRight(),
                Layout.Alignment.ALIGN_NORMAL, lineSpacingMultiplier, lineAdditionalVerticalPadding, false);
    }

    @Override
    public void setEllipsize(TextUtils.TruncateAt where) {
        super.setEllipsize(where);
        mEllipsize = where;
        isStale = true;
    }
}
