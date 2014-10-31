package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;

public class ListTextEntry extends ListEntryBase {
    private final Context mContext;
    private final TextView mTitle;
    private final TextView mText;

    private ImageLoadingGetter mImageGetter;

    public ListTextEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mContext = context;
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mText = (TextView) v.findViewById(R.id.feed_item_text);
        mTitle.setMaxLines(2);
        mText.setMaxLines(10);
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design) {
        super.setupEntry(entry, design);
        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(
                (mParentWidth == 0 ? 0 : mParentWidth
                        - getResources().getDimensionPixelSize(R.dimen.feed_item_padding_left)
                        - getResources().getDimensionPixelSize(R.dimen.feed_item_padding_left)),
                mContext);
        setupTitle(entry);
        setupText(entry);
        applyFeedStyle(design);
    }

    @Override
    public void applyFeedStyle(TlogDesign design) {
        super.applyFeedStyle(design);
        int textColor = design.getFeedTextColor(getResources());
        Typeface tf = design.isFontTypefaceSerif() ? getFontManager().getPostSerifTypeface() : getFontManager().getPostSansSerifTypeface();
        mText.setTextColor(textColor);
        mText.setTypeface(tf);
        mTitle.setTypeface(tf);
        mTitle.setTextColor(textColor);
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
