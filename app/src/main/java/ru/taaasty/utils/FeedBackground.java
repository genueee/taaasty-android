package ru.taaasty.utils;

import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.widgets.FeedBackgroundDrawable;

/**
 * Created by alexey on 09.08.15.
 */
public class FeedBackground {

    private final View mRoot;

    private final float mHeaderHeight;

    private final int mFeedHorizontalMargin;

    private TlogDesign mTlogDesign;

    private FeedBackgroundDrawable mDrawable;

    private String mBackgroundUrl;

    private boolean mRefreshQueued;

    private int mFeedPaddingTop;

    public FeedBackground(View root, @Nullable TlogDesign design, @DimenRes int headerHeightResId) {
        this.mRoot = root;
        this.mTlogDesign = design != null ? design : TlogDesign.DUMMY;
        mFeedHorizontalMargin = mRoot.getResources().getDimensionPixelSize(R.dimen.feed_horizontal_margin);
        mHeaderHeight = mRoot.getResources().getDimension(headerHeightResId);
        mFeedPaddingTop = (int)mHeaderHeight;
        setDummyDrawable();
        refreshBackground();
    }

    public void refreshBackground() {
        if (mRefreshQueued && TextUtils.equals(mBackgroundUrl, mTlogDesign.getBackgroundUrl())) {
            return;
        }

        mDrawable.setFeedDesign(mRoot.getResources(), mTlogDesign);
        reloadBackgroundDrawable();
    }

    public void setTlogDesign(TlogDesign design) {
        if (mTlogDesign.equals(design)) return;
        mTlogDesign = design;
        refreshBackground();
    }

    public void setHeaderVisibleFraction(float fraction) {
        fraction = UiUtils.clamp(fraction, 0f, 1f);
        int paddingTop = (int)(fraction * mHeaderHeight);
        if (paddingTop != mFeedPaddingTop) {
            mFeedPaddingTop = paddingTop;

            mDrawable.setFeedMargin(
                    mFeedHorizontalMargin,
                    mFeedPaddingTop,
                    mFeedHorizontalMargin,
                    0);
        }
    }

    private void setDummyDrawable() {
        if (mDrawable != null && mDrawable.getBitmap() == null) {
            mDrawable.setFeedDesign(mRoot.getResources(), mTlogDesign);
        } else {
            mDrawable = new FeedBackgroundDrawable(mRoot.getResources(), null, (int)mHeaderHeight);
            mDrawable.setFeedDesign(mRoot.getResources(), mTlogDesign);
        }
        mDrawable.setFeedMargin(
                mFeedHorizontalMargin,
                mFeedPaddingTop,
                mFeedHorizontalMargin,
                0
        );
        mRoot.setBackgroundDrawable(mDrawable);
    }

    private void reloadBackgroundDrawable() {
        if (mBackgroundUrl == null) setDummyDrawable();

        if (TextUtils.equals(mBackgroundUrl, mTlogDesign.getBackgroundUrl())) {
            return;
        }

        if (mTlogDesign.getBackgroundUrl() == null) {
            Picasso.with(mRoot.getContext()).cancelRequest(mPicassoTarget);
            mBackgroundUrl = null;
            setDummyDrawable();
            return;
        }

        mBackgroundUrl = mTlogDesign.getBackgroundUrl();

        if (mRoot.getWidth() > 0 && mRoot.getHeight() > 0) {
            reloadBackgroundDrawableAfterPreDraw();
        } else {
            mRefreshQueued = true;
            if (!mRoot.getViewTreeObserver().isAlive()) return;
            mRoot.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    mRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    mRefreshQueued = false;
                    reloadBackgroundDrawableAfterPreDraw();
                    return true;
                }
            });
        }
    }

    private void reloadBackgroundDrawableAfterPreDraw() {
        Picasso.with(mRoot.getContext())
                .load(mBackgroundUrl)
                .resize(mRoot.getWidth(), mRoot.getHeight())
                .onlyScaleDown()
                .centerCrop()
                .config(Bitmap.Config.RGB_565)
                .into(mPicassoTarget);
    }

    private Target mPicassoTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (mRoot.getResources() == null) return;
            mDrawable = new FeedBackgroundDrawable(mRoot.getResources(), bitmap, (int)mHeaderHeight);
            mDrawable.setFilterBitmap(true);
            mDrawable.setFeedMargin(
                    mFeedHorizontalMargin,
                    mFeedPaddingTop,
                    mFeedHorizontalMargin,
                    0
            );

            mDrawable.setColorFilter(new PorterDuffColorFilter(
                    mRoot.getResources().getColor(Constants.FEED_TITLE_BACKGROUND_DIM_COLOR_RES),
                    PorterDuff.Mode.SRC_OVER
            ));
            mDrawable.setFeedDesign(mRoot.getResources(), mTlogDesign);
            mRoot.setBackgroundDrawable(mDrawable);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            if (mRoot.getResources() == null) return;
            setDummyDrawable();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }
    };
}
