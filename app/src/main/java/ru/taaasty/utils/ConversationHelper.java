package ru.taaasty.utils;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DimenRes;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.TextUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;
import com.squareup.pollexor.ThumborUrlBuilder;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.rest.model.ImageInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.GroupConversation;
import ru.taaasty.rest.model.conversations.GroupPicture;
import ru.taaasty.rest.model.conversations.HasManyUsers;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.widgets.DefaultConversationIconDrawable;
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

    @Nullable
    private String getTitle(Conversation conversation, Context context, boolean recipientWithPrefix) {
        switch (conversation.getType()) {
            case PRIVATE:
                User recipient = ((PrivateConversation) conversation).getRecipient();
                return recipientWithPrefix ? recipient.getNameWithPrefix() : recipient.getName();
            case GROUP:
                return UiUtils.trimUnblank(((GroupConversation) conversation).getTopic());
            case PUBLIC:
                String entryTitle = UiUtils.trimUnblank(((PublicConversation) conversation).getEntryTitle());
                return !TextUtils.isEmpty(entryTitle) ? entryTitle :  context.getString(R.string.public_conversation_default_title);
            default:
                return null;
        }
    }

    public int countActiveUsers(Conversation conversation) {
        switch (conversation.getType()) {
            case PRIVATE:
                return 2;
            case GROUP:
            case PUBLIC:
                return countActiveUsers(((HasManyUsers)conversation).getUsers(),
                        ((HasManyUsers)conversation).getUsersLeft());
            case OTHER:
            default:
                return -1;
        }
    }

    private int countActiveUsers(List<User> usersAll, @Nullable long[] usersLeft) {
        if (usersLeft == null || usersLeft.length == 0) return usersAll.size();
        int cnt = 0;
        for (User user: usersAll) {
            if (!ArrayUtils.contains(usersLeft, user.getId())) {
                cnt += 1;
            }
        }
        return cnt;
    }

    public void getActiveUsers(HasManyUsers conversation, List<User> dst) {
        for (User user: conversation.getUsers()) {
            if (!ArrayUtils.contains(conversation.getUsersLeft(), user.getId())) {
                dst.add(user);
            }
        }
    }

    public List<User> getActiveUsers(HasManyUsers conversation) {
        List<User> users = new ArrayList<>();
        getActiveUsers(conversation, users);
        return users;
    }

    public User findUserById(List<User> users, long userId) {
        if (users == null) return null;
        for (User user: users) if (user.getId() == userId) return user;
        return null;
    }

    public void bindConversationIconToImageView(Conversation conversation, @DimenRes int dstSizeRes, ImageView dst) {
        Context context = dst.getContext();
        Drawable drawable = DefaultConversationIconDrawable.create(dst.getContext(), conversation);
        drawable.setBounds(0, 0, dstSizeRes, dstSizeRes);
        bindConversationIconToImageView(conversation, dstSizeRes, dst, drawable);
    }

    public boolean isNullOrAnonymousConversation(@Nullable Conversation conversation) {
        if (conversation == null) return true;
        if (conversation.getType() == Conversation.Type.PUBLIC
                && ((PublicConversation)conversation).isAnonymous()) return true;
        return false;
    }

    @Nullable
    public String getConversationIconUrl(Conversation conversation) {
        switch (conversation.getType()) {
            case PRIVATE:
                PrivateConversation privateChat = (PrivateConversation) conversation;
                if (privateChat.getRecipient() != null
                        && privateChat.getRecipient().getUserpic() != null) {
                    return privateChat.getRecipient().getUserpic().originalUrl;
                }
                break;
            case GROUP:
                GroupPicture picture = ((GroupConversation)conversation).getAvatar();
                if (picture != null) return picture.url;
                break;
            case PUBLIC:
                ImageInfo.Image2 picture2 = ((PublicConversation)conversation).getPreviewImage();
                if (picture2 != null) return picture2.url;
                break;
            case OTHER:
                break;
        }
        return null;
    }

    public void bindConversationIconToImageView(Conversation conversation, @DimenRes int dstSizeRes, ImageView dst, Drawable defaultGroupDrawable) {
        // Для аватарок тут немного ахтунг
        Context context = dst.getContext();

        if (conversation.getType() == Conversation.Type.PRIVATE) {
            // Приватный чат с одним пользователем. Показываем его аватарку.
            // Если аватарка у юзера не установлена, то ставим дефолтную аватарку _пользователя_
            // для таких случаев
            ImageUtils.getInstance().loadAvatarToImageView(((PrivateConversation) conversation).getRecipient(), dstSizeRes, dst);
            return;
        }

        // В групповых чатах никогда не показываем аватарок каких-либо пользователей
        String url = getConversationIconUrl(conversation);
        if (url != null) {
            bindConversationIconToImageView(url,
                    getTitleWithoutUserPrefix(conversation, context),
                    dstSizeRes,
                    dst,
                    defaultGroupDrawable);
        } else {
            // Для всех остальных случаев ставим дефолтную аватарку для групп.
            Picasso.with(context).cancelRequest(dst);
            dst.setImageDrawable(defaultGroupDrawable);
        }
    }

    public void setupAvatarImageViewClickableForeground(Conversation conversation, ExtendedImageView imageView) {
        // кликабельное состояние
        if (conversation.getType() == Conversation.Type.PRIVATE) {
            imageView.setForegroundResource(R.drawable.clickable_item_circle_background);
        } else {
            imageView.setForegroundDrawable(null); // TODO
        }
    }

    private void bindConversationIconToImageView(String url, String chatTitle, @DimenRes int dstSizeRes, ImageView dst, Drawable errorDrawable) {
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
                        new DrawBackgroundTransformation(context, R.drawable.group_post_default_avatar_stroke)))
                .into(dst);
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
