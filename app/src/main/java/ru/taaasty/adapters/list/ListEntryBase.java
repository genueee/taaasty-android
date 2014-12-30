package ru.taaasty.adapters.list;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.EntryBottomActionBar;


public abstract class ListEntryBase extends RecyclerView.ViewHolder {

    private final FontManager mFontManager;
    private final ViewGroup mAvatarAuthor;
    private final ImageView mAvatar;
    private final TextView mAuthor;
    private final EntryBottomActionBar mEntryActionBar;
    private final boolean mShowUserAvatar;

    protected int mParentWidth;

    public ListEntryBase(Context context, View v, boolean showUserAvatar) {
        super(v);
        mFontManager = FontManager.getInstance();
        mShowUserAvatar = showUserAvatar;
        mAvatarAuthor = (ViewGroup) v.findViewById(R.id.avatar_author);
        mAvatar = (ImageView) mAvatarAuthor.findViewById(R.id.avatar);
        mAuthor = (TextView) mAvatarAuthor.findViewById(R.id.author);

        mEntryActionBar = new EntryBottomActionBar(v.findViewById(R.id.entry_bottom_action_bar), true);

        if (!showUserAvatar) mAvatarAuthor.setVisibility(View.GONE);
    }

    public void setupEntry(Entry entry, TlogDesign design) {
        mEntryActionBar.setupEntry(entry);
        setAuthor(entry);
    }

    public void applyFeedStyle(TlogDesign design) {
        int textColor = design.getFeedTextColor(getResources());
        if (mShowUserAvatar) mAuthor.setTextColor(textColor);
        mEntryActionBar.setTlogDesign(design);
    }

    private void setAuthor(Entry item) {
        if (!mShowUserAvatar) return;
        User author = item.getAuthor();
        mAuthor.setText(author.getName());
        ImageUtils.getInstance().loadAvatar(author.getUserpic(), author.getName(), mAvatar, R.dimen.avatar_small_diameter);
    }

    public void setParentWidth(int width) {
        mParentWidth = width;
    }

    protected Resources getResources() {
        return mAuthor.getResources();
    }

    protected FontManager getFontManager() {
        return mFontManager;
    }

    public EntryBottomActionBar getEntryActionBar() {
        return mEntryActionBar;
    }

    public ViewGroup getAvatarAuthorView() { return mAvatarAuthor; }

    public abstract void recycle();

}
