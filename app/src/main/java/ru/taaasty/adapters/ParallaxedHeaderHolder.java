package ru.taaasty.adapters;

import ru.taaasty.recyclerview.RecyclerView;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;

import ru.taaasty.Constants;

/**
 * Created by alexey on 02.11.14.
 */
public class ParallaxedHeaderHolder extends RecyclerView.ViewHolder implements IParallaxedHeaderHolder {

    ParallaxedView parallaxedView;

    public ParallaxedHeaderHolder(final View itemView, final View parallaxedContainer) {
        super(itemView);
        parallaxedView = new ParallaxedView(parallaxedContainer) {
            // Ставим для parallaxedContainer прозрачность в зависимости от видимой части всего заголовка
            protected void setOpacity(View view, float offset) {
                float viewHeight = itemView.getHeight();
                float visibleHeight = itemView.getBottom() - offset; // XXX: wrong
                if (visibleHeight < viewHeight) {
                    float opacity = getParallaxOpacity(visibleHeight / viewHeight);
                    parallaxedContainer.setAlpha(opacity);
                } else {
                    parallaxedContainer.setAlpha(1);
                }
            }
        };
    }

    @Override
    public void onScrollChanged() {
        refreshParallaxOffset();
    }

    public void refreshParallaxOffset() {
        int top = -itemView.getTop();
        float factor = Constants.DEFAULT_PARALLAX_FACTOR;
        parallaxedView.setOffset((float) top / factor);
    }
}
