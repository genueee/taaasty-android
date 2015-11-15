package ru.taaasty.adapters;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.recyclerview.RecyclerView;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.SmartTextSwitcher;

/**
 * Created by alexey on 30.10.14.
 */
public class HeaderTitleSubtitleViewHolder extends RecyclerView.ViewHolder implements IParallaxedHeaderHolder {

    private final SmartTextSwitcher mTitleView;
    private final SmartTextSwitcher mSubtitleView;

    final View titleSubtitleContainer;

    private final ParallaxedView mParallaxedView;

    public TargetSetHeaderBackground feedDesignTarget;

    public String backgroundUrl;

    public HeaderTitleSubtitleViewHolder(View v) {
        super(v);
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

    public void setTitleSubtitle(int title, String subtitle) {
        mTitleView.setText(title);
        mSubtitleView.setText(subtitle);
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

    public void bindDesign(@Nullable TlogDesign design) {
        if (design == null) return;
        String backgroudUrl = design.getBackgroundUrl();

        if (TextUtils.equals(this.backgroundUrl, backgroudUrl)) return;

        feedDesignTarget = new TargetSetHeaderBackground(itemView,
                design, getBackgroundDimColorRes()) {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                super.onBitmapLoaded(bitmap, from);
                HeaderTitleSubtitleViewHolder.this.onBackgroundBitmapLoaded(bitmap, from);
            }
        };
        this.backgroundUrl = backgroudUrl;
        RequestCreator rq = Picasso.with(itemView.getContext())
                .load(backgroudUrl)
                .config(Bitmap.Config.RGB_565);
        if (itemView.getWidth() > 1 && itemView.getHeight() > 1) {
            rq.resize(itemView.getWidth() / 2, itemView.getHeight() / 2)
                    .centerCrop();
        }
        rq.into(feedDesignTarget);
    }

    protected int getBackgroundDimColorRes() {
        return Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES;
    }

    protected void onBackgroundBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        ImageUtils.getInstance().putBitmapToCache(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY, bitmap);
    }
}
