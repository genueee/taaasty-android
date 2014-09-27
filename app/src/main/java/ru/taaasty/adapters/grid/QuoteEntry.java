package ru.taaasty.adapters.grid;

import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.utils.UiUtils;

/**
* Created by alexey on 28.09.14.
*/
public class QuoteEntry {
    private final TextView mText;
    private final TextView mSource;

    public QuoteEntry(View v) {
        mText = (TextView) v.findViewById(R.id.feed_item_text);
        mSource = (TextView) v.findViewById(R.id.source);
        mText.setMaxLines(10);
    }

    public void setupEntry(Entry entry) {
        setupText(entry);
        setupSource(entry);
    }

    private void setupText(Entry entry) {
        Spanned text = UiUtils.formatQuoteText(entry.getText());
        if (text != null) {
            mText.setText(text);
            mText.setVisibility(View.VISIBLE);
        } else {
            mText.setVisibility(View.GONE);
        }
    }

    private void setupSource(Entry entry) {
        Spanned source = UiUtils.formatQuoteSource(entry.getSource());
        if (source != null) {
            mSource.setText(source);
            mSource.setVisibility(View.VISIBLE);
        } else {
            mSource.setVisibility(View.GONE);
        }
    }
}
