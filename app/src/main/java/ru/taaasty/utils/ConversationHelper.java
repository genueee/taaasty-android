package ru.taaasty.utils;


import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import ru.taaasty.R;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.widgets.DefaultUserpicDrawable;

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
        // Для аватарок тут немного ахтунг
        Context context = dst.getContext();

        if (conversation.avatar != null) {
            // У чата установлена аватарка. В первую очередь, показываем её
            bindAvatarToImageView(conversation.avatar.url, getTitleWithoutUserPrefix(conversation, context), dstSizeRes, dst);
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
                            getTitleWithoutUserPrefix(conversation, context), dstSizeRes, dst);
                    return;
                }
            }

            // Чат - групповой из нескольких участников и либо аватарка не установлена, либо одно из двух
            Picasso.with(context).cancelRequest(dst);
            Drawable defaultDrawable = new DefaultUserpicDrawable(context,
                    getTitleWithoutUserPrefix(conversation, context),
                    ContextCompat.getColor(context, R.color.avatar_default),
                    Color.WHITE
            );
            defaultDrawable.setBounds(0, 0, dstSizeRes, dstSizeRes);
            dst.setImageDrawable(defaultDrawable);
        }
    }

    public void bindAvatarToImageView(String url, String chatTitle, @DimenRes int dstSizeRes, ImageView dst) {
        Context context = dst.getContext();
        int imageSize = dst.getResources().getDimensionPixelSize(dstSizeRes);

        ThumborUrlBuilder thumborUrl = NetworkUtils.createThumborUrl(url);

        @SuppressWarnings("ConstantConditions")
        Drawable stubPlaceholder = context.getResources().getDrawable(R.drawable.ic_user_stub).mutate();
        stubPlaceholder.setBounds(0, 0, imageSize, imageSize);

        Drawable defaultDrawable = new DefaultUserpicDrawable(context,
                chatTitle,
                ContextCompat.getColor(context, R.color.avatar_default),
                Color.WHITE
        );
        defaultDrawable.setBounds(0, 0, dstSizeRes, dstSizeRes);

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
                .error(defaultDrawable)
                .resize(imageSize, imageSize)
                .onlyScaleDown()
                .centerCrop()
                .noFade()
                .transform(CircleTransformation.getInstance())
                .into(dst);
        return;
    }
}
