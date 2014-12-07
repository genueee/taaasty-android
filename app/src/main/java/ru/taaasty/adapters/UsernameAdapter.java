package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.Collection;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.PicassoDrawable;

/**
 * Created by alexey on 06.12.14.
 */
public class UsernameAdapter extends RecyclerView.Adapter<UsernameAdapter.ViewHolder> {

    public static final int AVATAR_SIZE_RES = R.dimen.avatar_small_diameter;
    private final UserList mUsers;

    private final LayoutInflater mInflater;

    private final ImageUtils mImageUtils;

    public UsernameAdapter(Context context) {
        super();
        mUsers = new UserList();
        mInflater = LayoutInflater.from(context);
        mImageUtils = ImageUtils.getInstance();
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View child = mInflater.inflate(R.layout.create_conversation_user_name_item, parent, false);
        return new ViewHolder(child);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        User user = mUsers.get(position);
        viewHolder.textView.setText(user.getName());
        mImageUtils.loadAvatar(viewHolder.itemView.getContext(), user.getUserpic(),
                user.getName(), viewHolder.mAvatarTarget, AVATAR_SIZE_RES);
    }

    @Override
    public long getItemId(int position) {
        return mUsers.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return mUsers.size();
    }

    public void setUsers(Collection<User> users) {
        mUsers.resetItems(users);
    }

    public User findUser(long userId) {
        return mUsers.findItem(userId);
    }

    public boolean isEmpty() {
        return mUsers.isEmpty();
    }

    private final class UserList extends SortedList<ru.taaasty.model.User> implements SortedList.OnListChangedListener {

        public UserList() {
            super(User.SORT_BY_NAME_COMPARATOR);
            setListener(this);
        }

        @Override
        public long getItemId(ru.taaasty.model.User item) {
            return item.getId();
        }

        @Override
        public void onDataSetChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemChanged(int location) {
            notifyItemChanged(location);
        }

        @Override
        public void onItemInserted(int location) {
            notifyItemInserted(location);
        }

        @Override
        public void onItemRemoved(int location) {
            notifyItemRemoved(location);
        }

        @Override
        public void onItemMoved(int fromLocation, int toLocation) {
            notifyItemMoved(fromLocation, toLocation);
        }

        @Override
        public void onItemRangeChanged(int locationStart, int itemCount) {
            notifyItemRangeChanged(locationStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int locationStart, int itemCount) {
            notifyItemRangeChanged(locationStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int locationStart, int itemCount) {
            notifyItemRangeRemoved(locationStart, itemCount);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        public final TextView textView;

        private final int avatarSize;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
            avatarSize = textView.getResources().getDimensionPixelSize(AVATAR_SIZE_RES);
        }

        public final ImageUtils.DrawableTarget mAvatarTarget = new ImageUtils.DrawableTarget() {

            @Override
            public void onDrawableReady(Drawable drawable) {
                textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }

            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                final Drawable newDrawable;

                if (Picasso.LoadedFrom.MEMORY.equals(from)) {
                    newDrawable = new BitmapDrawable(textView.getResources(), bitmap);
                } else {
                    Drawable placeholder = textView.getResources().getDrawable(R.drawable.ic_user_stub);
                    newDrawable = new PicassoDrawable(textView.getContext(), bitmap, placeholder, from, false, false);
                }

                newDrawable.setBounds(0, 0, avatarSize, avatarSize);
                textView.setCompoundDrawables(newDrawable, null, null, null);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                errorDrawable.setBounds(0, 0, avatarSize, avatarSize);
                textView.setCompoundDrawables(errorDrawable, null, null, null);
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                placeHolderDrawable.setBounds(0, 0, avatarSize, avatarSize);
                textView.setCompoundDrawables(placeHolderDrawable, null, null, null);
            }
        };

    }



}
