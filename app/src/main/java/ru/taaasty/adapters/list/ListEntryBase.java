package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import junit.framework.Assert;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.EntryBottomActionBar;
import ru.taaasty.widgets.PicassoDrawable;


public abstract class ListEntryBase extends RecyclerView.ViewHolder {

    private final FontManager mFontManager;
    private final TextView mAvatarAuthor;
    private final EntryBottomActionBar mEntryActionBar;
    private final boolean mShowUserAvatar;

    protected int mParentWidth;
    protected final Picasso picasso;

    public ListEntryBase(Context context, View v, boolean showUserAvatar) {
        super(v);
        mFontManager = FontManager.getInstance(context);
        mShowUserAvatar = showUserAvatar;
        mAvatarAuthor = (TextView) v.findViewById(R.id.avatar_author);
        mEntryActionBar = new EntryBottomActionBar(v.findViewById(R.id.entry_bottom_action_bar));
        picasso = Picasso.with(context);

        if (!showUserAvatar) {
            mAvatarAuthor.setVisibility(View.GONE);
        }
    }

    public void setupEntry(Entry entry, TlogDesign design, String feedId) {
        mEntryActionBar.setupEntry(entry);
        setAuthor(entry, feedId);
    }

    public void applyFeedStyle(TlogDesign design) {
        int textColor = design.getFeedTextColor(getResources());
        if (mShowUserAvatar) {
            if (design.isLightTheme()) {
                mAvatarAuthor.setTextColor(ContextCompat.getColorStateList(itemView.getContext(), R.color.text_color_feed_white_clickable));
            } else {
                mAvatarAuthor.setTextColor(textColor);
            }
        }
        mEntryActionBar.setTlogDesign(design);
    }

    /**
     * Верхная часть: автор (если не отключен)
     * @param item
     */
    private void setAuthor(Entry item, String feedId) {
        if (!mShowUserAvatar) return;
        User author = item.getAuthor();
        mAvatarAuthor.setText(getAvatarDescription(item, feedId));
        ImageUtils.getInstance().loadAvatar(itemView.getContext(),
                author.getUserpic(),
                author.getName(),
                mAvatarTarget,
                R.dimen.avatar_extra_small_diameter_34dp);
    }

    private CharSequence getAvatarDescription(Entry item, @Nullable  String feedId) {
        String template = "";
        String author = String.valueOf(item.getAuthor().getId());
        String tlog = String.valueOf(item.getTlog().id);

        if (!TextUtils.isEmpty(feedId)) {
            // Просматриваем чей-то фид или поток
            if (author.equals(tlog)) { // Автор написал в свой тлог
                if (author.equals(feedId)) {
                    template = "$from";
                } else {
                    template = getResources().getString(R.string.repost_from);
                }
            } else { // Авто написал не в свой тлог
                if (tlog.equals(feedId)) {
                    template = "$from";
                } else { // author, tlog, feedId не совпадают между собой
                    template =  getResources().getString(R.string.author_wrote_to_tlog);
                }
            }
        } else {
            // Просматриваем общий фид. Здесь репостов нет
            if (author.equals(tlog)) { // Юзер написал в свой тлог
                template = "$from";
            } else { // не в свой
                template = getResources().getString(R.string.author_wrote_to_tlog);
            }
        }

        return TextUtils.replace(template,
                new String[] {"$from", "$to"}, new CharSequence[] {
                item.getAuthor().getNameWithPrefix(), item.getTlog().author.getNameWithPrefix()
        });
    }

    public void setParentWidth(int width) {
        mParentWidth = width;
    }

    protected Resources getResources() {
        return itemView.getResources();
    }

    protected FontManager getFontManager() {
        return mFontManager;
    }

    public EntryBottomActionBar getEntryActionBar() {
        return mEntryActionBar;
    }

    public TextView getAvatarAuthorView() { return mAvatarAuthor; }

    public void recycle() {
        picasso.cancelRequest(mAvatarTarget);
    };

    int guessViewVisibleWidth(View view) {
        if (view.getWidth() > 0) {
            int width = view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
            if (BuildConfig.DEBUG && (mParentWidth != 0)) {
                int guessedWidth = guesViewVisibleWidthFromDimensions(view);
                Assert.assertEquals("Ширина может определяться неверно. width: " + width + " guessed: " + guessedWidth,
                        width, guessedWidth);
            }
            return width;
        } else {
            return guesViewVisibleWidthFromDimensions(view);
        }
    }

    private int guesViewVisibleWidthFromDimensions(View view) {
        if (mParentWidth == 0) return 0;
        ViewGroup.MarginLayoutParams itemViewLayoutParams = (ViewGroup.MarginLayoutParams)itemView.getLayoutParams();
        ViewGroup.MarginLayoutParams viewLayoutParams = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
        return mParentWidth -
                itemViewLayoutParams.leftMargin - itemViewLayoutParams.rightMargin -
                itemView.getPaddingLeft() - itemView.getPaddingRight() -
                viewLayoutParams.leftMargin - viewLayoutParams.rightMargin -
                view.getPaddingLeft() - view.getPaddingRight();
    }


    private final ImageUtils.DrawableTarget mAvatarTarget = new ImageUtils.DrawableTarget() {

        @Override
        public void onDrawableReady(Drawable drawable) {
            mAvatarAuthor.setCompoundDrawables(drawable, null, null, null);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            final Drawable newDrawable;

            if (Picasso.LoadedFrom.MEMORY.equals(from)) {
                newDrawable = new BitmapDrawable(mAvatarAuthor.getResources(), bitmap);
            } else {
                Drawable placeholder = mAvatarAuthor.getResources().getDrawable(R.drawable.ic_user_stub);
                newDrawable = new PicassoDrawable(mAvatarAuthor.getContext(), bitmap, placeholder, from, false, false);
            }
            newDrawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());

            mAvatarAuthor.setCompoundDrawables(newDrawable, null, null, null);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            mAvatarAuthor.setCompoundDrawables(errorDrawable, null, null, null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            mAvatarAuthor.setCompoundDrawables(placeHolderDrawable, null, null, null);
        }
    };

}
