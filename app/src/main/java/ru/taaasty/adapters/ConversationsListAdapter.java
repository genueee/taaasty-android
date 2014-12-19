package ru.taaasty.adapters;

import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextSwitcher;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;

/**
 * Created by alexey on 24.11.14.
 */
public class ConversationsListAdapter extends RecyclerView.Adapter<ConversationsListAdapter.ViewHolder> {

    public static final int REFRESH_NOTIFICATIONS_PERIOD = 10000;

    private final ImageUtils mImageUtils;

    private final ConversationsList mConversations;

    private final Handler mHandler;

    public ConversationsListAdapter() {
        super();
        mConversations = new ConversationsList();
        mImageUtils = ImageUtils.getInstance();
        mHandler = new Handler();
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View root = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.conversations_list_item, viewGroup, false);
        ViewHolder holder = new ViewHolder(root);
        initClickListeners(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Conversation conversation = mConversations.get(position);
        bindReadStatus(viewHolder, conversation);
        bindAvatar(viewHolder, conversation);
        bindText(viewHolder, conversation);
        bindDate(viewHolder, conversation);
        bindUnreadMessages(viewHolder, conversation);
    }

    @Override
    public int getItemCount() {
        return mConversations.size();
    }

    @Override
    public long getItemId(int position) {
        return mConversations.get(position).id;
    }

    public void onStart() {
        mHandler.removeCallbacks(mRefreshNotificationDateRunnable);
        mHandler.postDelayed(mRefreshNotificationDateRunnable, REFRESH_NOTIFICATIONS_PERIOD);
    }

    public void onStop() {
        mHandler.removeCallbacks(mRefreshNotificationDateRunnable);
    }

    public void setConversations(List<Conversation> conversations) {
        mConversations.resetItems(conversations);
    }

    public void addConversation(Conversation conversation) {
        mConversations.insertItem(conversation);
    }

    @Nullable
    public Conversation getConversation(int adapterPosition) {
        return mConversations.get(adapterPosition);
    }

    public void initClickListeners(ConversationsListAdapter.ViewHolder holder) {
    }

    private void bindAvatar(ViewHolder holder, Conversation conversation) {
        mImageUtils.loadAvatar(conversation.recipient, holder.avatar, R.dimen.avatar_small_diameter);
    }

    private void bindReadStatus(ViewHolder holder, Conversation conversation) {
        // TODO: серый цвет текста?
    }

    private void bindText(ViewHolder holder, Conversation conversation) {
        User author = conversation.recipient;
        SpannableStringBuilder ssb = new SpannableStringBuilder("@");
        ssb.append(author.getName());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), holder.itemView.getContext(), R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        if (conversation.lastMessage != null && !TextUtils.isEmpty(conversation.lastMessage.contentHtml)) {
            ssb.append(Html.fromHtml(conversation.lastMessage.contentHtml));
        }

        holder.text.setText(ssb);
    }

    private void bindDate(ViewHolder holder, Conversation conversation) {
        // Коррекция на разницу часов серверных и устройства
        long createdAtTime = System.currentTimeMillis();
        if (conversation.createdAt.getTime() < createdAtTime) createdAtTime = conversation.createdAt.getTime();

        CharSequence newDate = DateUtils.getRelativeDateTimeString(holder.itemView.getContext(), createdAtTime,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.WEEK_IN_MILLIS,
                0);

        CharSequence oldDate = ((TextView)holder.date.getChildAt(holder.date.getDisplayedChild())).getText();
        if (oldDate == null || !newDate.toString().equals(oldDate.toString())) {
            if (holder.mDateLastConversationId != conversation.id) {
                holder.mDateLastConversationId = conversation.id;
                holder.date.setCurrentText(newDate);
            } else {
                holder.date.setText(newDate);
            }
        }
    }

    private void bindUnreadMessages(ViewHolder holder, Conversation conversation) {
        if (conversation.unreadMessagesCount > 0) {
            // TODO: анимации переходов и появлений
            holder.msgCount.setVisibility(View.VISIBLE);
            holder.msgCount.setText(String.valueOf(conversation.unreadMessagesCount));
            holder.unreceivedIndicator.setVisibility(View.INVISIBLE);
        } else {
            holder.msgCount.setVisibility(View.INVISIBLE);
            holder.unreceivedIndicator.setVisibility(conversation.unreceivedMessagesCount > 0 ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private Runnable mRefreshNotificationDateRunnable = new Runnable() {
        @Override
        public void run() {
            boolean hasItemsToRefresh = false;
            int size = mConversations.size();
            long currentTime = System.currentTimeMillis();
            for (int i=0; i < size; ++i) {
                // Обновляем даты "XXX минут назад" для всех переписок, созданных за последний час
                if (Math.abs(currentTime - mConversations.get(i).createdAt.getTime()) < DateUtils.HOUR_IN_MILLIS) {
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

    private final class ConversationsList extends SortedList<Conversation> implements SortedList.OnListChangedListener {

        public ConversationsList() {
            super(Conversation.SORT_BY_CREATED_AT_DESC_COMPARATOR);
            setListener(this);
        }

        @Override
        public long getItemId(Conversation item) {
            return item.id;
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

    public class ViewHolder extends RecyclerView.ViewHolder  {

        public final ImageView avatar;

        public final TextView text;

        public final TextSwitcher date;

        public final TextView  msgCount;

        public final View unreceivedIndicator;

        // Защита от повторного использования viewholder'а для другого поста.
        // Если пост изменился, не анимируем старое неверное значение в текущее.
        public long mDateLastConversationId = -1;

        public ViewHolder(View v) {
            super(v);
            avatar = (ImageView)v.findViewById(R.id.avatar);
            text = (TextView)v.findViewById(R.id.last_message);
            date = (TextSwitcher)v.findViewById(R.id.notification_date);
            msgCount = (TextView)v.findViewById(R.id.unread_messages_count);
            unreceivedIndicator = v.findViewById(R.id.unreceived_messages_indicator);
        }
    }

    public boolean isEmpty() {
        return mConversations.isEmpty();
    }
}
