package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
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

    public ListEntryBase(Context context, View v, boolean showUserAvatar) {
        super(v);
        mFontManager = FontManager.getInstance();
        mShowUserAvatar = showUserAvatar;
        mAvatarAuthor = (TextView) v.findViewById(R.id.avatar_author);
        mEntryActionBar = new EntryBottomActionBar(v.findViewById(R.id.entry_bottom_action_bar), true);

        if (!showUserAvatar) {
            mAvatarAuthor.setVisibility(View.GONE);
        }
    }

    public void setupEntry(Entry entry, TlogDesign design) {
        mEntryActionBar.setupEntry(entry);
        setAuthor(entry);
    }

    public void applyFeedStyle(TlogDesign design) {
        int textColor = design.getFeedTextColor(getResources());
        if (mShowUserAvatar) mAvatarAuthor.setTextColor(textColor);
        mEntryActionBar.setTlogDesign(design);
    }

    private void setAuthor(Entry item) {
        if (!mShowUserAvatar) return;
        User author = item.getAuthor();
        mAvatarAuthor.setText(author.getName());
        ImageUtils.getInstance().loadAvatar(itemView.getContext(),
                author.getUserpic(),
                author.getName(),
                mAvatarTarget,
                R.dimen.avatar_extra_small_diameter_34dp);
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

    public abstract void recycle();

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
