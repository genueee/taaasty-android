package ru.taaasty.adapters;

import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;

import ru.taaasty.R;

/**
 * Created by alexey on 17.11.14.
 */
public class ParallaxedHeaderHolderTitleSubtitle extends ParallaxedHeaderHolder {

    View titleSubtitleContainer;

    ParallaxedView parallaxedView;

    public ParallaxedHeaderHolderTitleSubtitle(final View itemView) {
        super(itemView);
        titleSubtitleContainer = itemView.findViewById(R.id.title_subtitle_container);
        parallaxedView = new ParallaxedView(titleSubtitleContainer) {
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
}
