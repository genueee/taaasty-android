package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import junit.framework.Assert;

import java.util.ArrayList;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;

public class ListTextEntry extends ListEntryBase {
    private final Context mContext;
    private final TextView mTitle;
    private final TextView mText;

    private ImageLoadingGetter mImageGetter;

    private User mUser;

    private TextViewImgLoader mTitleImgLoader;
    private TextViewImgLoader mTextImgLoader;

    public ListTextEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mContext = context;
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mText = (TextView) v.findViewById(R.id.feed_item_text);

        mTitle.setMovementMethod(LinkMovementMethod.getInstance());
        mText.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design) {
        super.setupEntry(entry, design);

        if (BuildConfig.DEBUG) {
            if (mText.getWidth() != 0) {
                int textWidth = mText.getWidth() - mText.getPaddingLeft() - mText.getPaddingRight();
                int textWidth2 = mParentWidth - mText.getPaddingLeft() - mText.getPaddingRight();
                Assert.assertEquals("Ширина может определяться неверно. textWidth: " + textWidth + "parent width: " +textWidth2,
                        textWidth, textWidth2);
            }
        }

        mUser = entry.getAuthor();

        if (mImageGetter == null) mImageGetter = new ImageLoadingGetter(
                (mParentWidth == 0 ? 0 : mParentWidth
                        - mText.getPaddingLeft()
                        - mText.getPaddingRight()),
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

    @Override
    public void recycle() {
        if (mTextImgLoader != null) mTextImgLoader.reset();
        if (mTitleImgLoader != null) mTitleImgLoader.reset();
        mText.setText(null);
        mTitle.setText(null);
    }

    private void setupTitle(Entry entry) {
        if (entry.hasTitle()) {
            CharSequence title = UiUtils.removeTrailingWhitespaces(Html.fromHtml(entry.getTitle(), mImageGetter, null));
            mTitle.setText(title, TextView.BufferType.NORMAL);
            mTitleImgLoader = TextViewImgLoader.bindAndLoadImages(mTitle, onImgClickListener);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }
    }

    private void setupText(Entry item) {
        if (item.hasText()) {
            CharSequence text = UiUtils.removeTrailingWhitespaces(Html.fromHtml(item.getText(), mImageGetter, null));
            mText.setText(text, TextView.BufferType.NORMAL);
            mTextImgLoader = TextViewImgLoader.bindAndLoadImages(mText, onImgClickListener);
            mText.setVisibility(View.VISIBLE);
        } else {
            mText.setVisibility(View.GONE);
        }
    }

    private final TextViewImgLoader.OnClickListener onImgClickListener = new TextViewImgLoader.OnClickListener() {
        @Override
        public void onImageClicked(TextView widget, String source) {
            CharSequence seq = widget.getText();
            ArrayList<String> sources = UiUtils.getImageSpanUrls(seq);
            if (!sources.isEmpty()) {
                ShowPhotoActivity.startShowPhotoActivity(widget.getContext(), mUser, mTitle.getText().toString(), sources, null, widget);
            }
        }
    };
}
