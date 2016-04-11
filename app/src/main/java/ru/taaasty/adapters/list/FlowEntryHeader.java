package ru.taaasty.adapters.list;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.TextViewCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Userpic;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.AspectRatioImageView;
import ru0xdc.NdkStackBlur;

/**
 * ViewHolder для шапки поста в списках - поток
 */
public abstract class FlowEntryHeader {

    public final View root;

    public final AspectRatioImageView image;

    public final TextView title;

    public final TextView subscribersCount;

    public final TextView entriesCount;

    private final Picasso mPicasso;

    private final FlowHeaderTransformation mFlowHeaderTransfrom;

    public FlowEntryHeader(View root) {
        this.root = root;
        mPicasso = Picasso.with(root.getContext());
        mFlowHeaderTransfrom = new FlowHeaderTransformation();
        this.image = (AspectRatioImageView)root.findViewById(R.id.flow_image);
        this.title = (TextView)root.findViewById(R.id.flow_name);
        this.subscribersCount = (TextView)root.findViewById(R.id.subscribers_count);
        this.entriesCount = (TextView)root.findViewById(R.id.entries_count);

        Drawable entriesCountDrawable = DrawableCompat.wrap(
                ResourcesCompat.getDrawable(root.getResources(), R.drawable.ic_posts_count_20dp, null).mutate());
        DrawableCompat.setTintList(entriesCountDrawable, subscribersCount.getTextColors());
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(entriesCount, entriesCountDrawable, null, null, null);

        Drawable subsribersDrawable = DrawableCompat.wrap(
                ResourcesCompat.getDrawable(root.getResources(), R.drawable.ic_subscribers_count_20dp, null).mutate());
        DrawableCompat.setTintList(subsribersDrawable, subscribersCount.getTextColors());
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(subscribersCount,subsribersDrawable, null, null, null);

        mFlowHeaderTransfrom.setAvatarMetrics(62, 16, 16, root.getResources());

    }

    @SuppressLint("SetTextI18n")
    public void setupEntry(Entry entry) {
        this.title.setText('#' + UiUtils.capitalize(entry.getTlog().author.getName()));
        this.subscribersCount.setText(String.valueOf(entry.getTlog().relationshipsSummary.followersCount));
        this.entriesCount.setText(String.valueOf(entry.getTlog().author.getPublicEntriesCount()));
        setupImage(entry);
    }

    public void stopImageLoading() {
        mPicasso.cancelRequest(this.image);
    }

    // XXX придумать что-нибудь
    public abstract int guessVisibleWidth(View view);

    private void setupImage(Entry entry) {
        int viewWidth, viewHeight;
        if (ViewCompat.isLaidOut(this.image)) {
            viewWidth = this.image.getWidth();
            viewHeight = this.image.getHeight();
        } else {
            viewWidth = guessVisibleWidth(this.image);
            viewHeight = (int) (viewWidth * image.getAspectRatio());
        }
        setupImageAfterSizeKnown(entry, viewWidth, viewHeight);
    }

    private void setupImageAfterSizeKnown(Entry entry, int viewWidth, int viewHeight) {
        Userpic userpic = entry.getTlog().author.getUserpic();
        if (userpic == null || TextUtils.isEmpty(userpic.originalUrl)) {
            mPicasso.load(R.color.embedd_play_gray_background).into(this.image);
        } else {
            String url = NetworkUtils.createThumborUrl(userpic.originalUrl)
                    .resize(viewWidth, viewHeight)
                    .filter(ThumborUrlBuilder.noUpscale())
                    .toUrlUnsafe();

            mPicasso.load(url)
                    .placeholder(R.color.embedd_play_gray_background)
                    .error(R.color.embedd_play_gray_background)
                    .transform(mFlowHeaderTransfrom)
                    .resize(viewWidth, viewHeight)
                    .into(this.image);
        }
    }


    private static class FlowHeaderTransformation implements Transformation {

        private final NdkStackBlur mBlurer = NdkStackBlur.create();

        private int mAvatarSize;

        private int mMarginTop;

        private int mMarginLeft;

        public void setAvatarMetrics(int size, int marginTop, int marginLeft, Resources resources) {
            float density = resources.getDisplayMetrics().density;
            this.mAvatarSize = (int)(size * density + 0.5f);
            this.mMarginTop = (int)(marginTop * density + 0.5f);
            this.mMarginLeft = (int)(marginLeft * density + 0.5f);

        }

        private static final Rect srcRect = new Rect();
        private static final Rect dstRect = new Rect();

        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap bitmap;
            Bitmap avatar;

            if (source.isMutable() && source.getConfig() == Bitmap.Config.ARGB_8888) {
                bitmap = source;
            } else {
                bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
                if (bitmap == null) return source;
                source.recycle();
            }

            avatar = createAvatarBitmap(bitmap);

            mBlurer.blur(32, bitmap);

            Paint paint = new Paint();
            paint.setFilterBitmap(true);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.argb(128, 0, 0, 0));

            int top = (bitmap.getHeight() - mAvatarSize) / 2;
            if (top < 0) top = 0;

            srcRect.set(0, 0, avatar.getWidth(), avatar.getHeight());
            dstRect.set(mMarginLeft, top,
                    mMarginLeft + mAvatarSize, top + mAvatarSize);
            canvas.drawBitmap(avatar, srcRect, dstRect, paint);

            avatar.recycle();

            return bitmap;
        }

        private Bitmap createAvatarBitmap(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;
            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);
            float r = size/2f;
            canvas.drawCircle(r, r, r, paint);
            if (source != squaredBitmap) squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "flowHeaderTransformation()";
        }
    }


}
