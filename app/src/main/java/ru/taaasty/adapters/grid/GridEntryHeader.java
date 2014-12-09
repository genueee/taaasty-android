package ru.taaasty.adapters.grid;

import android.content.Context;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;

import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.adapters.IParallaxedHeaderHolder;
import ru.taaasty.model.Entry;
import ru.taaasty.widgets.SmartTextSwitcher;

/**
 * Created by alexey on 30.10.14.
 */
public class GridEntryHeader extends GridEntryBase implements IParallaxedHeaderHolder {

    private final SmartTextSwitcher mTitleView;
    private final SmartTextSwitcher mSubtitleView;
    final View titleSubtitleContainer;
    private final ParallaxedView mParallaxedView;

    private int mTitle;
    private CharSequence mSubtitle;

    public GridEntryHeader(Context context, View v) {
        super(context, v, 0);
        mTitleView = (SmartTextSwitcher)v.findViewById(R.id.title);
        mSubtitleView = (SmartTextSwitcher)v.findViewById(R.id.subtitle);

        titleSubtitleContainer = v.findViewById(R.id.title_subtitle_container);

        mParallaxedView = new ParallaxedView(titleSubtitleContainer) {
            // Ставим для titleSubtitleContainer прозрачность в зависимости от видимой части всего заголовка
            protected void setOpacity(View view, float offset) {
                float viewHeight = itemView.getHeight();
                float visibleHeight = itemView.getBottom() - offset; // XXX: wrong
                if (visibleHeight < viewHeight) {
                    float opacity = getParallaxOpacity(visibleHeight / viewHeight);
                    titleSubtitleContainer.setAlpha(opacity);
                } else {
                    titleSubtitleContainer.setAlpha(1);
                }
            }
        };
    }

    @Override
    public void bindEntry(Entry entry) {
        mTitleView.setText(mTitle);
        mSubtitleView.setText(mSubtitle);
    }

    public void setTitleSubtitle(int title, String subtitle) {
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
