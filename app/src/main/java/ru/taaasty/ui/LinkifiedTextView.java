package ru.taaasty.ui;

import android.content.Context;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import ru.taaasty.BuildConfig;

public class LinkifiedTextView extends TextView {
    public LinkifiedTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        TextView widget = (TextView) this;
        Object text = new SpannableString(widget.getText());
        if (text instanceof Spanned) {
            Spannable buffer = (Spannable)text;

            int action = event.getAction();

            if (action == MotionEvent.ACTION_UP
                    || action == MotionEvent.ACTION_DOWN) {
                int x = (int) event.getX();
                int y = (int) event.getY();

                x -= widget.getTotalPaddingLeft();
                y -= widget.getTotalPaddingTop();

                x += widget.getScrollX();
                y += widget.getScrollY();

                Layout layout = widget.getLayout();
                int line = layout.getLineForVertical(y);
                int off = layout.getOffsetForHorizontal(line, x);

                if ((y > layout.getLineBottom(line))
                        || (x > layout.getLineWidth(line))) {
                    // Переходим по ссылкам только если юзер очень хорошо прицелился перед тыком.
                    // Не переходим, если он ткнул где-нибудь далеко справа от текста
                    if (BuildConfig.DEBUG) Log.v("LinkifiedTextView",
                            "y: " + y + " line bottom:  " + layout.getLineBottom(line)
                            + " x: " + x + " line end: " + layout.getLineWidth(line));
                    return false;
                }

                ClickableSpan[] link = buffer.getSpans(off, off,
                        ClickableSpan.class);

                if (link.length != 0) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget);
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer,
                                buffer.getSpanStart(link[0]),
                                buffer.getSpanEnd(link[0]));
                    }
                    return true;
                }
            }

        }

        return false;
    }
}
