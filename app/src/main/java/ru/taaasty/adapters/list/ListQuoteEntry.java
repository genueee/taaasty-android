package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.UiUtils;

public class ListQuoteEntry extends  ListEntryBase {
    private final TextView mText;
    private final TextView mSource;

    private User mUser;

    public ListQuoteEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mText = (TextView) v.findViewById(R.id.feed_item_text);
        mSource = (TextView) v.findViewById(R.id.source);

        mText.setMovementMethod(LinkMovementMethodNoSelection.getInstance());
        mSource.setMovementMethod(LinkMovementMethodNoSelection.getInstance());

        if (Build.VERSION.SDK_INT <= 16) {
            // Оно там глючное, текст в списке съезжает вправо иногда
            mText.setTextIsSelectable(false);
            mSource.setTextIsSelectable(false);
        }
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design) {
        super.setupEntry(entry, design);
        mUser = entry.getAuthor();
        setupText(entry);
        setupSource(entry);
        applyFeedStyle(design);
    }

    @Override
    public void recycle() {
        mText.setText(null);
        mSource.setText(null);
    }

    @Override
    public void applyFeedStyle(TlogDesign design) {
        super.applyFeedStyle(design);
        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? getFontManager().getPostSerifTypeface() : getFontManager().getPostSansSerifTypeface();
        mText.setTextColor(textColor);
        mText.setTypeface(tf);
        mSource.setTypeface(tf);
        mSource.setTextColor(textColor);
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
