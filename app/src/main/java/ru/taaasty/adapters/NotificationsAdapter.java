package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.taaasty.R;
import ru.taaasty.model.Notification;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 24.10.14.
 */
public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.ViewHolder> {

    public static final int REFRESH_NOTIFICATIONS_PERIOD = 10000;
    private final ArrayList<Notification> mNotifications;
    private final ImageUtils mImageUtils;
    private final Picasso mPicasso;
    private final InteractionListener mListener;
    private final Context mContext;
    private final Handler mHandler;
    private long mSwitcherLastPostId = -1;
    private Drawable mStubPlaceholder;

    private Set<Long> mFollowProcess;

    public NotificationsAdapter(Context context, InteractionListener listener) {
        mContext = context;
        mNotifications = new ArrayList<>(10);
        mFollowProcess = new HashSet<>(1);
        mListener = listener;
        mImageUtils = ImageUtils.getInstance();
        mPicasso = Picasso.with(context);
        mStubPlaceholder = context.getResources().getDrawable(R.drawable.image_loading_drawable);
        mHandler = new Handler();
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.notifications_item, parent, false);
        return new ViewHolder(root);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Notification notification = mNotifications.get(position);
        bindReadStatus(viewHolder, notification);
        bindAvatar(viewHolder, notification);
        bindNotificationText(viewHolder, notification);
        bindNotificationDate(viewHolder, notification);

        if (notification.isTypeRelationship()) {
            bindRelationship(viewHolder, notification);
        } else if (notification.hasImage()) {
            bindEntryImage(viewHolder, notification);
        } else {
            viewHolder.rightContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        return mNotifications.get(position).id;
    }

    @Override
    public int getItemCount() {
        return mNotifications.size();
    }

    public void onStart() {
        mHandler.removeCallbacks(mRefreshNotificationDateRunnable);
        mHandler.postDelayed(mRefreshNotificationDateRunnable, REFRESH_NOTIFICATIONS_PERIOD);
    }

    public void onStop() {
        mHandler.removeCallbacks(mRefreshNotificationDateRunnable);
    }

    public boolean isEmpty() {
        return mNotifications.isEmpty();
    }

    public void setNotifications(List<Notification> notifications) {
        mNotifications.clear();
        mNotifications.addAll(notifications);
        Collections.sort(notifications, Notification.SORT_BY_CREATED_AT_COMPARATOR);
        notifyDataSetChanged();
    }

    public void addNotification(Notification notification) {
        if (mNotifications.isEmpty()
                || (notification.createdAt.compareTo(mNotifications.get(0).createdAt) > 0)
                ) {
            mNotifications.add(0, notification);
            notifyItemInserted(0);
        } else {
            boolean itemReplaced = false;
            int size = mNotifications.size();
            for (int i=0; i<size; ++i) {
                Notification old = mNotifications.get(i);
                if (old.id == notification.id) {
                    itemReplaced = true;
                    mNotifications.set(i, notification);
                    if (old.createdAt.equals(notification.createdAt)) {
                        notifyItemChanged(i);
                    } else {
                        Collections.sort(mNotifications, Notification.SORT_BY_CREATED_AT_COMPARATOR);
                        notifyDataSetChanged();
                    }
                    break;
                }
            }
            if (!itemReplaced) {
                mNotifications.add(notification);
                Collections.sort(mNotifications, Notification.SORT_BY_CREATED_AT_COMPARATOR);
                notifyDataSetChanged();
            }
        }

        onNotificationFollowUnfollowStopped(notification.id);
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

    private void bindReadStatus(ViewHolder holder, Notification notification) {

    }

    private void bindAvatar(ViewHolder holder, Notification notification) {
        mImageUtils.loadAvatar(notification.sender, holder.avatar, R.dimen.avatar_small_diameter);
    }

    private void bindNotificationText(ViewHolder holder, Notification notification) {
        User author = notification.sender;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getSlug());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), mContext, R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        ssb.append(notification.actionText);
        if (!TextUtils.isEmpty(notification.text)) {
            ssb.append(": ");
            ssb.append(notification.text);
        }

        holder.notification.setText(ssb);
    }

    private void bindNotificationDate(ViewHolder holder, Notification notification) {
        // Коррекция на разницу часов серверных и устройства
        long createdAtTime = System.currentTimeMillis();
        if (notification.createdAt.getTime() < createdAtTime) createdAtTime = notification.createdAt.getTime();

        CharSequence newDate = DateUtils.getRelativeDateTimeString(mContext, createdAtTime,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0);

        CharSequence oldDate = ((TextView)holder.notificationDate.getChildAt(holder.notificationDate.getDisplayedChild())).getText();
        if (oldDate == null || !newDate.toString().equals(oldDate.toString())) {
            if (mSwitcherLastPostId != notification.id) {
                mSwitcherLastPostId = notification.id;
                holder.notificationDate.setCurrentText(newDate);
            } else {
                holder.notificationDate.setText(newDate);
            }
        }
    }

    private void bindEntryImage(ViewHolder holder, Notification notification) {
        holder.rightContainer.setVisibility(View.VISIBLE);
        holder.entryImage.setVisibility(View.VISIBLE);
        holder.progressButton.setVisibility(View.GONE);
        holder.addButton.setVisibility(View.GONE);
        holder.addedButton.setVisibility(View.GONE);

        String url;
        // TODO: убрать startsWith, когда на сервере в path перестанут выдавать абсолютные ссылки на картинки
        if (!TextUtils.isEmpty(notification.image.path)
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

    private void bindRelationship(ViewHolder holder, Notification notification) {
        boolean meSubscribed = notification.isMeSubscribed();
        holder.rightContainer.setVisibility(View.VISIBLE);
        holder.entryImage.setVisibility(View.GONE);
        if (mFollowProcess.contains(notification.id)) {
            holder.progressButton.setVisibility(View.VISIBLE);
            holder.addButton.setVisibility(View.INVISIBLE);
            holder.addedButton.setVisibility(View.INVISIBLE);
        } else {
            holder.progressButton.setVisibility(View.INVISIBLE);
            holder.addButton.setVisibility(meSubscribed ? View.INVISIBLE : View.VISIBLE);
            holder.addedButton.setVisibility(meSubscribed ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private Runnable mRefreshNotificationDateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean hasItemsToRefresh = false;
            int size = mNotifications.size();
            long currentTime = System.currentTimeMillis();
            for (int i=0; i < size; ++i) {
                if (Math.abs(currentTime - mNotifications.get(i).createdAt.getTime()) < DateUtils.HOUR_IN_MILLIS) {
                    notifyItemChanged(i);
                    hasItemsToRefresh = true;
                }
            }
            if (hasItemsToRefresh) {
                mHandler.removeCallbacks(this);
                mHandler.postDelayed(this, REFRESH_NOTIFICATIONS_PERIOD);
            }
        }
    };

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final ImageView avatar;
        public final ru.taaasty.widgets.LinkifiedTextView notification;
        public final TextSwitcher notificationDate;
        public final View rightContainer;
        public final View addedButton;
        public final View addButton;
        public final View progressButton;
        public final ImageView entryImage;

        public ViewHolder(View v) {
            super(v);
            avatar = (ImageView) v.findViewById(R.id.avatar);
            notification = (ru.taaasty.widgets.LinkifiedTextView) v.findViewById(R.id.notification);
            notificationDate = (TextSwitcher) v.findViewById(R.id.notification_date);
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

    public static interface InteractionListener {
        public void onNotificationClicked(View v, Notification notification);
        public void onAvatarClicked(View v, Notification notification);
        public void onAddButtonClicked(View v, Notification notification);
        public void onAddedButtonClicked(View v, Notification notification);
    }

}
