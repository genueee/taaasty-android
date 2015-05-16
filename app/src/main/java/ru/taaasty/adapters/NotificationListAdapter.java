package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.Notification;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.RelativeDateTextSwitcher;

/**
 * Created by alexey on 24.10.14.
 */
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {

    public static final int VIEW_TYPE_ITEM = R.id.view_type_item;

    public static final int VIEW_TYPE_PENDING_INDICATOR = R.id.view_type_pending_indicator;

    private final NotificationsList mNotifications;
    private final ImageUtils mImageUtils;
    private final Picasso mPicasso;
    private final InteractionListener mListener;
    private final Context mContext;
    private Drawable mStubPlaceholder;

    private Set<Long> mFollowProcess;

    private boolean mPendingIndicatorShown;

    public NotificationListAdapter(Context context, InteractionListener listener) {
        mContext = context;
        mNotifications = new NotificationsList();
        mFollowProcess = new HashSet<>(1);
        mPendingIndicatorShown = false;
        mListener = listener;
        mImageUtils = ImageUtils.getInstance();
        mPicasso = Picasso.with(context);
        mStubPlaceholder = context.getResources().getDrawable(R.drawable.image_loading_drawable);
        setHasStableIds(true);
        registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                Log.v("", "onChanged");
            }

            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
            }

            @Override
            public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount);
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View root;
        final ViewHolder holder;

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case VIEW_TYPE_PENDING_INDICATOR:
                root = inflater.inflate(R.layout.endless_loading_indicator, parent, false);
                holder = new ViewHolderPendingIndicator(root);
                break;
            case VIEW_TYPE_ITEM:
                root = inflater.inflate(R.layout.notifications_item, parent, false);
                holder = new ViewHolderItem(root);
                break;
            default:
                throw new IllegalStateException();
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        if (isPendingIndicatorPosition(position)) {
            return;
        } else {
            ViewHolderItem holderItem = (ViewHolderItem)viewHolder;
            Notification notification = mNotifications.get(position);
            bindReadStatus(holderItem, notification);
            bindAvatar(holderItem, notification);
            bindNotificationText(holderItem, notification);
            bindNotificationDate(holderItem, notification);

            if (notification.isTypeRelationship()) {
                bindRelationship(holderItem, notification);
            } else if (notification.hasImage()) {
                bindEntryImage(holderItem, notification);
            } else {
                holderItem.rightContainer.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return isPendingIndicatorPosition(position) ? RecyclerView.NO_ID : mNotifications.get(position).id;
    }

    @Override
    public int getItemViewType(int position) {
        return isPendingIndicatorPosition(position) ? VIEW_TYPE_PENDING_INDICATOR : VIEW_TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        return mNotifications.size() + (mPendingIndicatorShown ? 1 : 0);
    }

    public boolean isEmpty() {
        return mNotifications.isEmpty();
    }

    public void setNotifications(List<Notification> notifications) {
        mNotifications.resetItems(notifications);
    }

    public void addNotification(Notification notification) {
        mNotifications.add(notification);
        onNotificationFollowUnfollowStopped(notification.id);
    }

    public NotificationsList getNotifications() {
        return mNotifications;
    }

    public void onNotificationFollowUnfollowStarted(long notificationId) {
        if (mFollowProcess.add(notificationId)) {
            int size = mNotifications.size();
            for (int i=0; i<size; ++i) {
                if (notificationId == mNotifications.get(i).id) notifyItemChanged(i);
            }
        }
    }

    public void onNotificationFollowUnfollowStopped(long notificationId) {
        if (mFollowProcess.remove(notificationId)) {
            int size = mNotifications.size();
            for (int i=0; i<size; ++i) {
                if (notificationId == mNotifications.get(i).id) notifyItemChanged(i);
            }
        }
    }

    public void setShowPendingIndicator(boolean newValue) {
        if (newValue == mPendingIndicatorShown) return;
        mPendingIndicatorShown = newValue;
        if (newValue) {
            notifyItemInserted(getPendingIndicatorPosition());
        } else {
            notifyItemRemoved(getPendingIndicatorPosition());
        }
    }

    private int getPendingIndicatorPosition() {
        return mNotifications.size();
    }

    private boolean isPendingIndicatorPosition(int position) {
        if (!mPendingIndicatorShown) return false;
        return position == getPendingIndicatorPosition();
    }

    private void bindReadStatus(ViewHolderItem holder, Notification notification) {
        holder.unreadIndicator.setVisibility(notification.isMarkedAsRead() ? View.INVISIBLE : View.VISIBLE);
    }

    private void bindAvatar(ViewHolderItem holder, Notification notification) {
        mImageUtils.loadAvatar(notification.sender, holder.avatar, R.dimen.avatar_small_diameter);
    }

    private void bindNotificationText(ViewHolderItem holder, Notification notification) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getName());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext, R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        ssb.append(notification.actionText);
        if (!TextUtils.isEmpty(notification.text)) {
            ssb.append(": ");
            ssb.append(notification.text);
        }

        holder.notification.setText(ssb);
    }

    private void bindNotificationDate(ViewHolderItem holder, Notification notification) {
        holder.notificationDate.setRelativeDate(notification.createdAt.getTime());
    }

    private void bindEntryImage(ViewHolderItem holder, Notification notification) {
        holder.rightContainer.setVisibility(View.VISIBLE);
        holder.entryImage.setVisibility(View.VISIBLE);
        holder.progressButton.setVisibility(View.GONE);
        holder.addButton.setVisibility(View.GONE);
        holder.addedButton.setVisibility(View.GONE);

        String url;
        if (!TextUtils.isEmpty(notification.image.path)
                // Можно убрать, не актуально, сервер http://, и пр. больше не возвращает
                && !notification.image.path.startsWith("http://")
                && !notification.image.path.startsWith("https://")
                ) {
            ThumborUrlBuilder tb = NetworkUtils.createThumborUrlFromPath(notification.image.path);
            if (holder.entryImage.getWidth() != 0) {
                tb.resize(holder.entryImage.getWidth(), holder.entryImage.getHeight());
            }
            url = tb.toUrl();
        } else {
            url = notification.image.url;
        }

        mPicasso
                .load(url)
                .placeholder(mStubPlaceholder)
                .error(R.drawable.image_load_error)
                .fit()
                .centerCrop()
                .into(holder.entryImage);
    }

    private void bindRelationship(ViewHolderItem holder, Notification notification) {
        boolean meSubscribed = notification.isMeSubscribed();

        holder.rightContainer.setVisibility(View.VISIBLE);
        holder.entryImage.setVisibility(View.GONE);
        if (mFollowProcess.contains(notification.id)) {
            holder.progressButton.setVisibility(View.VISIBLE);
            holder.addButton.setVisibility(View.INVISIBLE);
            holder.addedButton.setVisibility(View.INVISIBLE);
        } else {
            holder.progressButton.setVisibility(View.INVISIBLE);
            if (notification.isFollowingRequest()) {
                holder.addButton.setVisibility(View.INVISIBLE);
                holder.addedButton.setVisibility(View.INVISIBLE);
            } else {
                holder.addButton.setVisibility(meSubscribed ? View.INVISIBLE : View.VISIBLE);
                holder.addedButton.setVisibility(meSubscribed ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }

    public final class NotificationsList extends SortedList<Notification> {

        public NotificationsList() {
            super(Notification.class, new SortedListAdapterCallback<Notification>(NotificationListAdapter.this) {
                @Override
                public int compare(Notification o1, Notification o2) {
                    return Notification.SORT_BY_CREATED_AT_DESC_COMPARATOR.compare(o1, o2);
                }

                @Override
                public boolean areContentsTheSame(Notification oldItem, Notification newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areItemsTheSame(Notification item1, Notification item2) {
                    return item1.id == item2.id;
                }
            });
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public final class ViewHolderPendingIndicator extends ViewHolder {
        public ViewHolderPendingIndicator(View itemView) {
            super(itemView);
        }
    }

    public final class ViewHolderItem extends ViewHolder implements  View.OnClickListener {
        public final View unreadIndicator;
        public final ImageView avatar;
        public final ru.taaasty.widgets.LinkifiedTextView notification;
        public final RelativeDateTextSwitcher notificationDate;
        public final View rightContainer;
        public final View addedButton;
        public final View addButton;
        public final View progressButton;
        public final ImageView entryImage;

        public ViewHolderItem(View v) {
            super(v);
            unreadIndicator = v.findViewById(R.id.unread_indicator);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            notification = (ru.taaasty.widgets.LinkifiedTextView) v.findViewById(R.id.notification);
            notificationDate = (RelativeDateTextSwitcher) v.findViewById(R.id.notification_date);
            addButton = v.findViewById(R.id.add_relationship);
            addedButton = v.findViewById(R.id.relationship_added);
            progressButton = v.findViewById(R.id.change_relationship_progress);
            rightContainer = v.findViewById(R.id.add_remove_relationship_container);
            entryImage = (ImageView)v.findViewById(R.id.entry_image);

            notification.setMaxLines(3);

            Animation in = AnimationUtils.loadAnimation(mContext,
                    android.R.anim.fade_in);
            Animation out = AnimationUtils.loadAnimation(mContext,
                    android.R.anim.fade_out);
            notificationDate.setInAnimation(in);
            notificationDate.setOutAnimation(out);


            v.setOnClickListener(this);
            avatar.setOnClickListener(this);
            addButton.setOnClickListener(this);
            addedButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Notification notification = mNotifications.get(getPosition());
            switch (v.getId()) {
                case R.id.avatar:
                    mListener.onAvatarClicked(v, notification);
                    break;
                case R.id.add_relationship:
                    onNotificationFollowUnfollowStarted(notification.id);
                    mListener.onAddButtonClicked(v, notification);
                    break;
                case R.id.relationship_added:
                    onNotificationFollowUnfollowStarted(notification.id);
                    mListener.onAddedButtonClicked(v, notification);
                    break;
                default:
                    mListener.onNotificationClicked(v, notification);
                    break;
            }
        }
    }

    public interface InteractionListener {
        void onNotificationClicked(View v, Notification notification);
        void onAvatarClicked(View v, Notification notification);
        void onAddButtonClicked(View v, Notification notification);
        void onAddedButtonClicked(View v, Notification notification);
    }

}
