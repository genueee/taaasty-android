package ru.taaasty.adapters.grid;

import android.content.Context;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 28.09.14.
 */
public class GridTextEntry {
    private final Context mContext;
    private final TextView mTitle;
    private final TextView mText;

    private ImageLoadingGetter mImageGetter;

    public GridTextEntry(Context context, View v) {
        mContext = context;
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mText = (TextView) v.findViewById(R.id.feed_item_text);
        mTitle.setMaxLines(2);
        mText.setMaxLines(10);
    }

    public void setupEntry(Entry entry, int parentWidth) {
        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(parentWidth, mContext);
        setupTitle(entry);
        setupText(entry);
    }

    private void setupTitle(Entry entry) {
        if (entry.hasTitle()) {
            CharSequence title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(entry.getTitle(), mImageGetter, null));
            mTitle.setText(title, TextView.BufferType.NORMAL);
            TextViewImgLoader.bindAndLoadImages(mTitle);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }
    }

    private void setupText(Entry item) {
        if (item.hasText()) {
            CharSequence text = UiUtils.removeTrailingWhitespaces(Html.fromHtml(item.getText(), mImageGetter, null));
            mText.setText(text, TextView.BufferType.NORMAL);
            TextViewImgLoader.bindAndLoadImages(mText);
            mText.setVisibility(View.VISIBLE);
        } else {
            mText.setVisibility(View.GONE);
        }
    }
}
