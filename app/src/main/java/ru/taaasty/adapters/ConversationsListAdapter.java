package ru.taaasty.adapters;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.RelativeDateTextSwitcher;


public class ConversationsListAdapter extends RecyclerView.Adapter<ConversationsListAdapter.ViewHolder> {

    private final ImageUtils mImageUtils;

    private final SortedList<Conversation> mConversations;

    public ConversationsListAdapter(SortedList<Conversation> conversationList) {
        super();
        mConversations = conversationList;
        mImageUtils = ImageUtils.getInstance();
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
        SpannableStringBuilder ssb = new SpannableStringBuilder(author.getNameWithPrefix());
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), holder.itemView.getContext(), R.style.TextAppearanceSlugInlineGreen);
        ssb.append(' ');
        if (conversation.lastMessage != null && !TextUtils.isEmpty(conversation.lastMessage.contentHtml)) {
            ssb.append(Html.fromHtml(conversation.lastMessage.contentHtml));
        }

        holder.text.setText(ssb);
    }

    private void bindDate(ViewHolder holder, Conversation conversation) {
        holder.date.setRelativeDate(conversation.createdAt.getTime());
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

    public class ViewHolder extends RecyclerView.ViewHolder  {

        public final ImageView avatar;

        public final TextView text;

        public final RelativeDateTextSwitcher date;

        public final TextView  msgCount;

        public final View unreceivedIndicator;

        public ViewHolder(View v) {
            super(v);
            avatar = (ImageView)v.findViewById(R.id.avatar);
            text = (TextView)v.findViewById(R.id.last_message);
            date = (RelativeDateTextSwitcher)v.findViewById(R.id.notification_date);
            msgCount = (TextView)v.findViewById(R.id.unread_messages_count);
            unreceivedIndicator = v.findViewById(R.id.unreceived_messages_indicator);
        }
    }

    public boolean isEmpty() {
        return mConversations.isEmpty();
    }
}
