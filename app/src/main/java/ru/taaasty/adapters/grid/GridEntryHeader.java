package ru.taaasty.adapters.grid;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.nirhart.parallaxscroll.views.ParallaxedView;

import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.model.Entry;

/**
 * Created by alexey on 30.10.14.
 */
public class GridEntryHeader extends GridEntryBase implements IParallaxedHeaderHolder {

    private final TextView mTitleView;
    private final TextView mSubtitleView;
    private final ParallaxedView mParallaxedView;

    private CharSequence mTitle;
    private CharSequence mSubtitle;

    public GridEntryHeader(Context context, View v) {
        super(context, v, 0);
        mTitleView = (TextView)v.findViewById(R.id.title);
        mSubtitleView = (TextView)v.findViewById(R.id.subtitle);
        mParallaxedView = new ParallaxedView(itemView);
    }

    @Override
    public void bindEntry(Entry entry) {
        mTitleView.setText(mTitle);
        mSubtitleView.setText(mSubtitle);
    }

    public void setTitleSubtitle(String title, String subtitle) {
        mTitle = title;
        mSubtitle = subtitle;
    }

    @Override
    public void recycle() {
    }

    @Override
    public void onScrollChanged() {
        refreshParallaxOffset();
    }

    public void refreshParallaxOffset() {
        int top = -itemView.getTop();
        float factor = Constants.DEFAULT_PARALLAX_FACTOR;
        mParallaxedView.setOffset((float) top / factor);
    }
}
