package ru.taaasty.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Collection;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.Objects;

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
        mImageUtils
                .loadAvatarToLeftDrawableOfTextView(user, AVATAR_SIZE_RES, viewHolder.textView);
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

    public User getById(long id) {
        for (int i = 0; i < mUsers.size(); i++) {
            if (mUsers.get(i).getId() == id) {
                return mUsers.get(i);
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return mUsers.isEmpty();
    }

    private final class UserList extends SortedList<User> {

        public UserList() {
            super(User.class, new SortedListAdapterCallback<User>(UsernameAdapter.this) {

                @Override
                public int compare(User o1, User o2) {
                    return User.SORT_BY_NAME_COMPARATOR.compare(o1, o2);
                }

                @Override
                public boolean areContentsTheSame(User oldItem, User newItem) {
                    if (!TextUtils.equals(oldItem.getName(), newItem.getName())) return false;
                    return Objects.equals(oldItem.getUserpic(), newItem.getUserpic());
                }

                @Override
                public boolean areItemsTheSame(User item1, User item2) {
                    return item1.getId() == item2.getId();
                }
            });
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView textView;

        private final int avatarSize;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
            avatarSize = textView.getResources().getDimensionPixelSize(AVATAR_SIZE_RES);
        }



    }


}
