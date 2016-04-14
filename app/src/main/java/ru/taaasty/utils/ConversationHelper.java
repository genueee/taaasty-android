package ru.taaasty.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.Arrays;

import ru.taaasty.R;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.widgets.ExtendedImageView;

public final class ConversationHelper {

    private static volatile ConversationHelper sInstance;

    private ConversationHelper() {
    }

    public static ConversationHelper getInstance() {
        if (sInstance == null) {
            synchronized (ConversationHelper.class) {
                if (sInstance == null) sInstance = new ConversationHelper();
            }
        }
        return sInstance;
    }

    public String getTitle(Conversation conversation, Context context) {
        return getTitle(conversation, context, true);
    }

    public String getTitleWithoutUserPrefix(Conversation conversation, Context context) {
        return getTitle(conversation, context, false);
    }

    private String getTitle(Conversation conversation, Context context, boolean recipientWithPrefix) {
        if (conversation.isPrivateGroup()) {
            return conversation.topic;
        } else if (conversation.isPublicGroup()) {
            if (TextUtils.isEmpty(conversation.entry.getTitle().trim())) {
                return context.getString(R.string.public_conversation_default_title);
            } else {
                return conversation.entry.getTitle().trim();
            }
        } else {
            return recipientWithPrefix ? conversation.recipient.getNameWithPrefix() : conversation.recipient.getName();
        }
    }

    public void bindAvatarToImageView(Conversation conversation, @DimenRes int dstSizeRes, ImageView dst) {
        Context context = dst.getContext();
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.group_post_default_avatar).mutate();
        drawable.setBounds(0, 0, dstSizeRes, dstSizeRes);
        bindAvatarToImageView(conversation, dstSizeRes, dst, drawable);
    }

    public void bindAvatarToImageView(Conversation conversation, @DimenRes int dstSizeRes, ImageView dst, Drawable defaultGroupDrawable) {
        // Для аватарок тут немного ахтунг
        Context context = dst.getContext();

        if (conversation.avatar != null) {
            // У чата установлена аватарка. В первую очередь, показываем её
            bindAvatarToImageView(conversation.avatar.url, getTitleWithoutUserPrefix(conversation, context), dstSizeRes, dst, defaultGroupDrawable);
            return;
        }

        if (!conversation.isGroup()) {
            // Приватный чат с одним пользователем. Показываем его аватарку.
            // Если аватарка у юзера не установлена, то ставим дефолтную аватарку _пользователя_
            // для таких случаев
            ImageUtils.getInstance().loadAvatarToImageView(conversation.recipient, dstSizeRes, dst);
        } else {
            // Групповой чат. Никогда не показываем аватарок каких-лбио пользователей
            if (conversation.isPublicGroup()) {
                // Чат - обсуждение записи. Попробуем показать какую-нибудь картинку из поста.
                if (conversation.entry != null && conversation.entry.getPreviewImage() != null) {
                    bindAvatarToImageView(conversation.entry.getPreviewImage().url,
                            getTitleWithoutUserPrefix(conversation, context), dstSizeRes, dst, defaultGroupDrawable);
                    return;
                }
            }

            // Чат - групповой из нескольких участников и либо аватарка не установлена, либо одно из двух
            Picasso.with(context).cancelRequest(dst);
            dst.setImageDrawable(defaultGroupDrawable);
        }
    }

    public void setupAvatarImageViewClickableForeground(Conversation conversation, ExtendedImageView imageView) {
        // кликабельное состояние
        if (conversation.isGroup()) {
            imageView.setForegroundDrawable(null); // TODO
        } else {
            imageView.setForegroundResource(R.drawable.clickable_item_circle_background);
        }
    }

    private void bindAvatarToImageView(String url, String chatTitle, @DimenRes int dstSizeRes, ImageView dst, Drawable errorDrawable) {
        Context context = dst.getContext();
        int imageSize = dst.getResources().getDimensionPixelSize(dstSizeRes);

        ThumborUrlBuilder thumborUrl = NetworkUtils.createThumborUrl(url);

        @SuppressWarnings("ConstantConditions")
        Drawable stubPlaceholder = context.getResources().getDrawable(R.drawable.ic_user_stub).mutate();
        stubPlaceholder.setBounds(0, 0, imageSize, imageSize);

        String userpicUrl = thumborUrl.resize(imageSize, imageSize)
                .filter(ThumborUrlBuilder.noUpscale())
                .toUrlUnsafe();

        // Picasso.placeholder() может сработать слишком поздно и на экране на пару
        // кадров появится левое изображение, поэтому ставим сами
        dst.setImageDrawable(stubPlaceholder);

        Picasso.with(context).cancelRequest(dst); // не нужно, но хрен с ним, пусть будет
        Picasso.with(context)
                .load(userpicUrl)
                .placeholder(stubPlaceholder)
                .error(errorDrawable)
                .resize(imageSize, imageSize)
                .onlyScaleDown()
                .centerCrop()
                .noFade()
                .transform(Arrays.asList(
                        RoundedCornersTransformation.create(dst.getResources().getDimensionPixelSize(R.dimen.group_avatar_corner_radius)),
                        new DrawBackgroundTransformation(context, R.drawable.group_post_default_avatar)))
                .into(dst);
        return;
    }

    // Рисование Drawable на Bitmap
    public static class DrawBackgroundTransformation implements Transformation {

        private final int mDrawableResId;

        private final Drawable mDrawable;

        public DrawBackgroundTransformation(Context context, int drawableResId) {
            mDrawableResId = drawableResId;
            mDrawable = ResourcesCompat.getDrawable(context.getResources(), drawableResId, null).mutate();
        }

        @Override
        public Bitmap transform(Bitmap source) {
            Bitmap bitmap;
            if (source.isMutable() && source.getConfig() == Bitmap.Config.ARGB_8888) {
                bitmap = source;
            } else {
                bitmap = source.copy(Bitmap.Config.ARGB_8888, true);
                source.recycle();
            }

            Canvas canvas = new Canvas(bitmap);
            mDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            mDrawable.draw(canvas);

            return bitmap;
        }

        @Override
        public String key() {
            return "DrawBackgroundTransformation(drawable=" + mDrawableResId + ")";
        }
    }
}
