package ru.taaasty.adapters;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Date;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.HintedExtendedImageView;
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
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View root = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.list_item_conversation, viewGroup, false);
        ViewHolder holder = new ViewHolder(root);
        initClickListeners(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        Conversation conversation = mConversations.get(position);
        bindAvatar(viewHolder, conversation);
        bindTitle(viewHolder, conversation);
        bindDate(viewHolder, conversation);
        bindUnreadMessages(viewHolder, conversation);
        bindLastMessage(viewHolder, conversation);
    }

    private static boolean isLastSenderShouldBeShown(Conversation conversation) {
        //  в групповых чатах отправитель отображается всегда, в личных - только для исходящих.
        // Получатель и так отображается, поэтому не дублируют для исходящих.
        // В чате с самим собой отправитель не отображается
        if (conversation.getLastMessage() == null
                || conversation.getLastMessage().author == null
                || conversation.getLastMessage().isSystemMessage()) return false;
        if (conversation.getType() != Conversation.Type.PRIVATE) return true;
        if (conversation.getUserId() == ((PrivateConversation)conversation).getRecipientId()) return false;
        return conversation.getLastMessage().isFromMe(conversation);
    }

    @Override
    public int getItemCount() {
        return mConversations.size();
    }

    @Override
    public long getItemId(int position) {
        return mConversations.get(position).getId();
    }

    @Nullable
    public Conversation getConversation(int adapterPosition) {
        return mConversations.get(adapterPosition);
    }

    public void initClickListeners(ConversationsListAdapter.ViewHolder holder) {
    }

    private void bindAvatar(ViewHolder holder, Conversation conversation) {
        ConversationHelper helper = ConversationHelper.getInstance();
        helper.bindConversationIconToImageView(conversation, R.dimen.avatar_size_conversation_list, holder.avatar);
//        ConversationHelper.getInstance().setupAvatarImageViewClickableForeground(conversation, holder.avatar);
    }


    private void bindTitle(ViewHolder holder, Conversation conversation) {
        String title = ConversationHelper.getInstance().getTitleWithoutUserPrefix(conversation, holder.title.getContext());
        holder.title.setText(title);

        if (conversation.isNotDisturbTurnedOn()) {
            Drawable drawable = ResourcesCompat.getDrawable(holder.title.getResources(), R.drawable.ic_mute_off_24dp, null).mutate();
            drawable.setColorFilter(
                    ResourcesCompat.getColor(holder.title.getResources(), R.color.conversation_list_text_secondary, null),
                    PorterDuff.Mode.SRC_ATOP
            );
            drawable.setBounds(0, 0,
                    (int)(14 * holder.itemView.getResources().getDisplayMetrics().scaledDensity + 0.5f),
                    (int)(14 * holder.itemView.getResources().getDisplayMetrics().scaledDensity + 0.5f)
                    );
            holder.title.setCompoundDrawables(null, null, drawable, null);
        } else {
            holder.title.setCompoundDrawables(null, null, null, null);
        }
    }

    private void bindDate(ViewHolder holder, Conversation conversation) {
        Date date;
        if (conversation.getLastMessage() != null) {
            date = conversation.getLastMessage().readAt != null ? conversation.getLastMessage().readAt : conversation.getLastMessage().createdAt;
        } else {
            date = conversation.getUpdatedAt() != null ? conversation.getUpdatedAt() : conversation.getCreatedAt();
        }
        holder.date.setRelativeDate(date.getTime());

        holder.date.setCompoundDrawablesWithIntrinsicBounds(getLastMessageReadStatus(holder, conversation),
                null, null, null);
    }

    private Drawable getLastMessageReadStatus(ViewHolder holder, Conversation conversation) {
        if (conversation.getLastMessage() == null) return null;
        if (!conversation.getLastMessage().isFromMe(conversation)) return null;
        if (conversation.getLastMessage().isSystemMessage()) return null;
        int drawableResId = conversation.getLastMessage().readAt == null ? R.drawable.ic_done_grey_10dp : R.drawable.ic_done_all_grey_10dp;
        Drawable drawable = ResourcesCompat.getDrawable(holder.itemView.getResources(),
                drawableResId, null).mutate();
        drawable.setColorFilter(holder.itemView.getResources().getColor(R.color.text_color_green), PorterDuff.Mode.SRC_ATOP);
        return drawable;
    }

    private void bindUnreadMessages(ViewHolder holder, Conversation conversation) {
        if (conversation.getUnreadMessagesCount() > 0) {
            holder.unreadMessageCount.setText(String.valueOf(conversation.getUnreadMessagesCount()));
            holder.unreadMessageCount.setVisibility(View.VISIBLE);
        } else {
            holder.unreadMessageCount.setVisibility(View.GONE);
        }
    }

    private void bindLastMessage(ViewHolder holder, Conversation conversation) {
        if (isLastSenderShouldBeShown(conversation)) {
            holder.lastMsgNoAvatar.setVisibility(View.GONE);
            holder.messageAvatar.setVisibility(View.VISIBLE);
            holder.lastMsgSenderName.setVisibility(View.VISIBLE);
            holder.lastMsgWithAvatar.setVisibility(View.VISIBLE);
            bindLastMessageAvatar(holder, conversation);
            bindLastMsgUsername(holder, conversation);

            bindLastMessageText(holder, conversation, holder.lastMsgWithAvatar);
            if (conversation.isTyped()) {
                holder.lastMsgWithAvatar.setText(R.string.typing);
            }
        } else {
            holder.lastMsgNoAvatar.setVisibility(View.VISIBLE);
            holder.messageAvatar.setVisibility(View.GONE);
            holder.lastMsgSenderName.setVisibility(View.GONE);
            holder.lastMsgWithAvatar.setVisibility(View.GONE);

            bindLastMessageText(holder, conversation, holder.lastMsgNoAvatar);
            if (conversation.isTyped()) {
                holder.lastMsgNoAvatar.setText(R.string.typing);
            }
        }

    }

    private void bindLastMessageAvatar(ViewHolder holder, Conversation conversation) {
        mImageUtils.loadAvatarToImageView(conversation.getLastMessage().author, R.dimen.avatar_size_conversation_list_small, holder.messageAvatar);
    }

    private void bindLastMsgUsername(ViewHolder holder, Conversation conversation) {
        holder.lastMsgSenderName.setText(conversation.getLastMessage().author.getName());
    }

    private void bindLastMessageText(ViewHolder holder, Conversation conversation, TextView textView) {
        textView.setText(formatLastMessageText(conversation, textView.getResources()));
    }

    private CharSequence formatLastMessageText(Conversation conversation, Resources resources) {
        if (conversation.getLastMessage() != null) {
            if(!UiUtils.isBlank(conversation.getLastMessage().contentHtml)) {
                return Html.fromHtml(conversation.getLastMessage().contentHtml);
            } else {
                SpannableStringBuilder ssb;
                if (conversation.getLastMessage().getFirstImageAttachment() != null) {
                    ssb = new SpannableStringBuilder(resources.getText(R.string.conversation_list_image_attachment_photo));
                } else {
                    ssb = new SpannableStringBuilder(resources.getText(R.string.conversation_list_image_attachment));
                }
                return ssb;
            }
        }
        return "";
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public final HintedExtendedImageView avatar;

        public final TextView title;

        public final RelativeDateTextSwitcher date;

        public final TextView unreadMessageCount;

        public final ImageView messageAvatar;

        public final TextView lastMsgSenderName;

        public final TextView lastMsgWithAvatar;

        public final TextView lastMsgNoAvatar;

        public ViewHolder(View v) {
            super(v);
            avatar = (HintedExtendedImageView) v.findViewById(R.id.avatar);
            title = (TextView) v.findViewById(R.id.title);
            date = (RelativeDateTextSwitcher) v.findViewById(R.id.notification_date);
            unreadMessageCount = (TextView) v.findViewById(R.id.unread_messages_count);
            messageAvatar = (ImageView) v.findViewById(R.id.message_avatar);
            lastMsgSenderName = (TextView)v.findViewById(R.id.last_msg_sender_name);
            lastMsgWithAvatar = (TextView) v.findViewById(R.id.last_message_with_avatar);
            lastMsgNoAvatar = (TextView) v.findViewById(R.id.last_message_no_avatar);
        }
    }

    public boolean isEmpty() {
        return mConversations.isEmpty();
    }
}
