package ru.taaasty.ui.post;

import android.support.v4.view.ViewPager;
import android.view.View;

/**
 * Новый фрагмент наезжает на старый, старый исчезает и уезжает медленнее обычного
 */
public class FadePageTransformer implements ViewPager.PageTransformer {

    public void transformPage(View view, float position) {
        int pageWidth = view.getWidth();

        if (position <= -1) { // [-Infinity,-1]
            // This page is way off-screen to the left.
            view.setAlpha(0);
            view.setTranslationX(0);
        } else if (position <= 0) { // (-1,0]
            view.setAlpha(position + 1f);
            view.setTranslationX(0.85f * (pageWidth * -position));
        } else if (position <= 1) { // (0,1]
        } else { // (1,+Infinity]
            // This page is way off-screen to the right.
            view.setAlpha(0);
            view.setTranslationX(0);
        }
    }
}
