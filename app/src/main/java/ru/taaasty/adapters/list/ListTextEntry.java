package ru.taaasty.adapters.list;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.util.TimingLogger;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;

public class ListTextEntry extends ListEntryBase {
    private final Context mContext;
    public final TextView mTitle;
    public final TextView mText;

    private ImageLoadingGetter mImageGetter;

    private User mUser;

    private TextViewImgLoader mTitleImgLoader;
    private TextViewImgLoader mTextImgLoader;

    public ListTextEntry(Context context, View v, boolean showUserAvatar) {
        super(context, v, showUserAvatar);
        mContext = context;
        mTitle = (TextView) v.findViewById(R.id.feed_item_title);
        mText = (TextView) v.findViewById(R.id.feed_item_text);

        mTitle.setMovementMethod(LinkMovementMethodNoSelection.getInstance());
        mText.setMovementMethod(LinkMovementMethodNoSelection.getInstance());

        if (Build.VERSION.SDK_INT <= 16) {
            // Оно там глючное, текст в списке съезжает вправо иногда
            mText.setTextIsSelectable(false);
            mTitle.setTextIsSelectable(false);
        }
    }

    @Override
    public void setupEntry(Entry entry, TlogDesign design, String feedId) {
        TimingLogger timings = null;
        if (BuildConfig.DEBUG) {
            timings = new TimingLogger(Constants.LOG_TAG, "setup TextEntry");
        }
        super.setupEntry(entry, design, feedId);

        mUser = entry.getAuthor();
        if (mImageGetter == null) {
            mImageGetter = new ImageLoadingGetter(
                    guessViewVisibleWidth(mText),
                    mContext);
        } else {
            if (BuildConfig.DEBUG) guessViewVisibleWidth(mText); // Assertion
        }
        setupTitle(entry);
        if (BuildConfig.DEBUG) timings.addSplit("setupTitle() done");

        setupText(entry);
        if (BuildConfig.DEBUG) timings.addSplit("setupText() done");

        applyFeedStyle(design);

        if (BuildConfig.DEBUG && timings != null) {
            timings.addSplit("applyFeedStyle() done. setup TextEntry end");
            timings.dumpToLog();
        }
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
    }

    private void setupTitle(Entry entry) {
        if (entry.hasTitle()) {
            CharSequence title = UiUtils.formatEntryTextSpanned(entry.getTitleSpanned(), mImageGetter);
            mTitle.setText(title, TextView.BufferType.NORMAL);
            mTitleImgLoader = TextViewImgLoader.bindAndLoadImages(mTitle, onImgClickListener);
            mTitle.setVisibility(View.VISIBLE);
        } else {
            mTitle.setVisibility(View.GONE);
        }
    }

    private void setupText(Entry item) {
        if (item.hasText()) {
            CharSequence text = UiUtils.formatEntryTextSpanned(item.getTextSpanned(), mImageGetter);
            mText.setText(text, TextView.BufferType.NORMAL); // Главный тормоз
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
                ShowPhotoActivity.startShowPhotoActivity(widget.getContext(), mTitle.getText().toString(), sources, null, widget);
            }
        }
    };
}
