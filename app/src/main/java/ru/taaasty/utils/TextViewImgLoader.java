package ru.taaasty.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.HashSet;
import java.util.Set;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.ui.ResizeImageSpan;

public class TextViewImgLoader {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TextViewImgLoader";

    public static interface OnClickListener {
        void onImageClicked(TextView widget, String source);
    }

    private final Picasso mPicasso;

    private TextView mTextView;

    @Nullable
    private final OnClickListener mListener;

    final Set<ImgTarget> mTargets = new HashSet<>(2);

    private static TextViewImgLoader bindToView(TextView view, OnClickListener listener) {
        TextViewImgLoader imgLoader = (TextViewImgLoader)view.getTag(R.id.tag_text_view_img_loader);
        if (imgLoader == null) {
            imgLoader = new TextViewImgLoader(view.getContext(), listener);
            // anti picasso weak-ref
            view.setTag(R.id.tag_text_view_img_loader, imgLoader);
        }
        return imgLoader;
    }

    public static TextViewImgLoader bindAndLoadImages(TextView view, OnClickListener listener) {
        TextViewImgLoader loader = bindToView(view, listener);
        loader.loadImages(view);
        return loader;
    }

    public static TextViewImgLoader bindAndLoadImages(TextView view) {
        return bindAndLoadImages(view, null);
    }

    public TextViewImgLoader(Context context, OnClickListener listener) {
        mListener = listener;
        mPicasso = Picasso.with(context);
    }

    public void loadImages(TextView view) {
        reset();
        ImageSpan imageSpans[] = getTextViewImageSpans(view);
        if (imageSpans == null) {
            // Загружать нечего
            return;
        }
        setupClickableSpans(view);
        for (ImageSpan span: imageSpans) {
            if (TextUtils.isEmpty(span.getSource())) continue;
            ImgTarget target = new ImgTarget(span);
            mTextView = view;
            mTargets.add(target);

            int maxWidth = guessMaxWidth(mTextView, span);
            int maxHeight = ImageUtils.getMaxTextureSize();

            if (DBG) Log.v(TAG, "picasso resize: " + maxWidth + "x" + maxHeight);

            mPicasso
                    .load(span.getSource())
                    .error(R.drawable.image_load_error)
                    .config(Bitmap.Config.RGB_565)
                    .resize(maxWidth, maxHeight)
                    .onlyScaleDown()
                    .centerInside()
                    .into(target);
        }
        if (mTargets.isEmpty()) mTextView = null;
    }

    public void reset() {
        for (ImgTarget t: mTargets) mPicasso.cancelRequest(t);
        mTargets.clear();
        mTextView = null;
    }

    private void setupClickableSpans(TextView view) {
        Spannable spannable;
        ImageSpan imageSpans[];

        if (mListener == null) return;

        imageSpans = getTextViewImageSpans(view);
        if (imageSpans == null) return;
        CharSequence charSeq = view.getText();
        spannable = Spannable.Factory.getInstance().newSpannable(charSeq);

        for (ImageSpan imageSpan: imageSpans) {
            int start = spannable.getSpanStart(imageSpan);
            int end = spannable.getSpanEnd(imageSpan);
            int flags = spannable.getSpanFlags(imageSpan);
            ClickableSpan clickSpan = new ImgClickSpan(view, imageSpan.getSource());
            // Удаляем URL'ы, нафиг они не нужны
            URLSpan urlSpans[] = spannable.getSpans(start, end, URLSpan.class);
            if (urlSpans != null) {
                for (URLSpan s: urlSpans) spannable.removeSpan(s);
            }
            // XXX: удалять все ClickableSpan'ы?
            spannable.setSpan(clickSpan, start, end, flags);
        }
        view.setText(spannable);
    }

    void removeTarget(ImgTarget target) {
        mPicasso.cancelRequest(target);
        mTargets.remove(target);
        if (mTargets.isEmpty()) mTextView = null;
    }

    @Nullable
    private static ImageSpan[] getTextViewImageSpans(TextView view) {
        CharSequence charSeq = view.getText();
        Spanned text;
        if (TextUtils.isEmpty(charSeq)) {
            return null;
        }
        if (!(charSeq instanceof Spanned)) {
            Log.v(TAG, "charSeq is not spanned: " + charSeq.getClass() );
            return null;
        }
        text = (Spanned)charSeq;
        ImageSpan imageSpans[] = text.getSpans(0, text.length(), ImageSpan.class);
        if (imageSpans == null || imageSpans.length == 0) {
            return null;
        }
        return imageSpans;
    }

    private class ImgClickSpan extends ClickableSpan {

        private final String mSource;

        private final TextView mTextView;

        public ImgClickSpan(TextView textView, String source) {
            super();
            this.mTextView = textView;
            this.mSource = source;
        }

        @Override
        public void onClick(View widget) {
            if (mListener != null) mListener.onImageClicked(this.mTextView, mSource);
        }
    }

    private static int guessMaxWidth(@Nullable TextView textView, ImageSpan span) {
        int width;
        if (textView != null
                && textView.getWidth() != 0
                && textView.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) {
            width = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
            if (DBG) Log.v(TAG, "guessMaxWidth() textView width: " + width);
        } else {
            width = span.getDrawable().getIntrinsicWidth();
            if (width == 0) width = span.getDrawable().getBounds().width();
            if (DBG) Log.v(TAG, "guessMaxWidth() span intristic width: "
                    + span.getDrawable().getIntrinsicWidth() + " bounds width: "
                    + span.getDrawable().getBounds().width());
        }

        int maxTextureSize = ImageUtils.getMaxTextureSize();

        return width == 0 ? maxTextureSize : Math.min(width, maxTextureSize);
    }

    private class ImgTarget implements Target {

        private final ImageSpan mImageSpan;

        public ImgTarget(ImageSpan imageSpan) {
            this.mImageSpan = imageSpan;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            // Масштабируем вручную, иначе какие-то проблемы с памятью в ImageView
            // Размеры TextView тут могут быть еще неизвестны. Пытаемся угадать откуда-нибудь из drawable
            int width = guessMaxWidth(mTextView, mImageSpan);
            int bitmapWidth = bitmap.getWidth();
            if (DBG) Log.v(TAG, "onBitmapLoaded() bitmap width: " + bitmapWidth + " guessed width: " + width);
            if (width != 0 && (bitmapWidth > width)) {
                float scale = (float)width / (float)bitmapWidth;
                Matrix m = new Matrix();
                m.preScale(scale, scale);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmap.getHeight(), m, false);
            }

            Drawable d = new BitmapDrawable(mTextView.getResources(), bitmap);
            ImageSpan newSpan = new ResizeImageSpan(d, mImageSpan.getSource(), width);
            replaceSpan(newSpan);
            removeTarget(this);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            if (errorDrawable == null) throw new IllegalStateException();
            errorDrawable.setBounds(mImageSpan.getDrawable().getBounds());
            ImageSpan newSpan = new ImageSpan(errorDrawable);
            if (DBG) Log.e(TAG, "onBitmapFailed.");
            replaceSpan(newSpan);
            removeTarget(this);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }

        private void replaceSpan(ImageSpan newSpan) {
            ImageSpan[] spans;
            CharSequence charSeq;
            Spannable spannable;

            spans = getTextViewImageSpans(mTextView);
            if (spans == null) return;
            int spanNo = -1;
            for (int n = 0; n < spans.length; n++) {
                if (mImageSpan.equals(spans[n])) spanNo = n;
            }
            if (spanNo == -1) return;
            charSeq = mTextView.getText();
            spannable = Spannable.Factory.getInstance().newSpannable(charSeq);

            int start = spannable.getSpanStart(mImageSpan);
            int end = spannable.getSpanEnd(mImageSpan);
            int flags = spannable.getSpanFlags(mImageSpan);
            spannable.removeSpan(mImageSpan);
            spannable.setSpan(newSpan, start, end, flags);
            mTextView.setText(spannable);
        }
    }

}
