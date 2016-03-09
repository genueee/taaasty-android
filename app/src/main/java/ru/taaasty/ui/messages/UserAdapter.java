package ru.taaasty.ui.messages;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.PopupMenu.OnMenuItemClickListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.ImageUtils;

import java.util.ArrayList;

/**
 * Created by arhis on 01.03.2016.
 */
public class UserAdapter extends RecyclerView.Adapter {

    public interface AdapterListener {
        void onUserAvatarClicked(View view, User user);
        void onUserRemoved(User user);
    }

    private Context context;
    private AdapterListener listener;
    private boolean isReadOnly;

    SortedList<User> users = new SortedList<>(User.class, new SortedListAdapterCallback<User>(this) {
        @Override
        public int compare(User user1, User user2) {
            if (Session.getInstance().isMe(user1.getId())) {
                return -Integer.MAX_VALUE;
            }
            return user1.getName().compareTo(user2.getName());
        }

        @Override
        public boolean areContentsTheSame(User oldItem, User newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(User item1, User item2) {
            return item2.equals(item1);
        }
    });

    public UserAdapter(Context context, AdapterListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setIsReadonly() {
        isReadOnly = true;
    }

    public void setUsers(ArrayList<User> usersList) {
        ArrayList<User> toRemove = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            if (!usersList.contains(users.get(i))) {
                toRemove.add(users.get(i));
            }
        }
        users.beginBatchedUpdates();
        for (User user : toRemove) {
            users.remove(user);
        }
        for (User user : usersList) {
            users.add(user);
        }
        users.endBatchedUpdates();
    }

    public void removeUser(User user) {
        users.remove(user);
    }

    public void addUser(User user) {
        users.add(user);
    }

    OnClickListener onAvatarClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            User user = (User) v.getTag();
            if (listener != null) {
                listener.onUserAvatarClicked(v, user);
            }
        }
    };

    OnClickListener onMenuClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            final User user = (User) v.getTag();
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenu().add(Menu.NONE, 0, 0, R.string.delete_post);
            popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (listener != null) {
                        listener.onUserRemoved(user);
                    }
                    return false;
                }
            });
            popupMenu.show();
        }
    };

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        UserHolder holder = new UserHolder(LayoutInflater.from(context).inflate(R.layout.list_item_chat_user, parent, false));
        holder.avatar.setOnClickListener(onAvatarClickListener);
        if (!isReadOnly) {
            holder.menuButton.setOnClickListener(onMenuClickListener);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        User user = users.get(position);
        UserHolder userHolder = (UserHolder) holder;
        userHolder.name.setText(user.getName());
        ImageUtils.getInstance().loadAvatar(user.getUserpic(), user.getName(),
                userHolder.avatar, R.dimen.avatar_small_diameter);
        userHolder.avatar.setTag(user);
        userHolder.menuButton.setTag(user);
        userHolder.menuButton.setVisibility(Session.getInstance().isMe(user.getId()) || isReadOnly ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserHolder extends ViewHolder {

        private View root;
        private TextView name;
        private ImageView avatar;
        private View menuButton;

        public UserHolder(View itemView) {
            super(itemView);
            root = itemView;
            name = (TextView) itemView.findViewById(R.id.flow_title);
            avatar = (ImageView) itemView.findViewById(R.id.flow_image);
            menuButton = itemView.findViewById(R.id.menu);
        }
    }
}
