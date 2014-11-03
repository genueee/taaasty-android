package ru.taaasty.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;

import ru.taaasty.Constants;

/**
 * Created by alexey on 02.11.14.
 */
public class ParallaxedHeaderHolder extends RecyclerView.ViewHolder implements IParallaxedHeaderHolder {

    ParallaxedView parallaxedView;

    public ParallaxedHeaderHolder(View itemView) {
        super(itemView);
        parallaxedView = new ParallaxedView(itemView);
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
