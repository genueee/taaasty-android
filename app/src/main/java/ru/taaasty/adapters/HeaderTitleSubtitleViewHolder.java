package ru.taaasty.adapters;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SafeOnPreDrawListener;
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

    private SafeOnPreDrawListener mSafeOnPreDrawListener;

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
        final String backgroudUrl = design.getBackgroundUrl();

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
        if (ViewCompat.isLaidOut(itemView)) {
            loadDesignAfterSizeKnown(backgroudUrl, itemView.getWidth(), itemView.getHeight());
        } else {
            if (mSafeOnPreDrawListener != null) {
                mSafeOnPreDrawListener.cancelAndRemoveListener();
            }
            mSafeOnPreDrawListener = new SafeOnPreDrawListener(itemView, root -> {
                loadDesignAfterSizeKnown(backgroudUrl, itemView.getWidth(), itemView.getHeight());
                return false;
            });
            itemView.getViewTreeObserver().addOnPreDrawListener(mSafeOnPreDrawListener);
        }
    }

    private void loadDesignAfterSizeKnown(String originUrl, int viewWidth, int viewHeight) {
        if (BuildConfig.DEBUG) Log.v("HeaderVH", "loadDesignAfterSizeKnown() url: " + originUrl + " view width: " + viewWidth + " view height: " + viewHeight);

        String url = NetworkUtils.createThumborUrl(originUrl)
                .resize(viewWidth / 2, viewHeight / 2)
                .filter(ThumborUrlBuilder.noUpscale())
                .toUrlUnsafe();

        Picasso.with(itemView.getContext())
                .load(url)
                .config(Bitmap.Config.RGB_565)
                .resize(viewWidth / 2, viewHeight / 2)
                .onlyScaleDown()
                .centerCrop()
                .into(feedDesignTarget);
    }

    protected int getBackgroundDimColorRes() {
        return Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES;
    }

    protected void onBackgroundBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
        ImageUtils.getInstance().putBitmapToCache(Constants.MY_FEED_HEADER_BACKGROUND_BITMAP_CACHE_KEY, bitmap);
    }
}
