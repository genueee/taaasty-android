package ru.taaasty.adapters;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Conversation;
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


    private static boolean isLastSenderShouldBeShown(Conversation conversation) {
        //  в групповых чатах отправитель отображается всегда, в личных - только для исходящих.
        // Получатель и так отображается, поэтому не дублируют для исходящих.
        // В чате с самим собой отправитель не отображается
        if (conversation.lastMessage == null || conversation.lastMessage.author == null) return false;
        if (conversation.isGroup()) return true;
        if (conversation.userId == conversation.recipientId) return false;
        return conversation.lastMessage.userId == conversation.userId;
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
        ConversationHelper helper = ConversationHelper.getInstance();
        helper.bindAvatarToImageView(conversation, R.dimen.avatar_small_diameter, holder.avatar);
        ConversationHelper.getInstance().setupAvatarImageViewClickableForeground(conversation, holder.avatar);

        // Last message sender
        if (isLastSenderShouldBeShown(conversation)) {
            holder.messageAvatar.setVisibility(View.VISIBLE);
            mImageUtils.loadAvatarToImageView(conversation.lastMessage.author, R.dimen.avatar_small_diameter_24dp, holder.messageAvatar);
        } else {
            Picasso.with(holder.messageAvatar.getContext()).cancelRequest(holder.messageAvatar);
            holder.messageAvatar.setVisibility(View.GONE);
        }
    }

    private void bindReadStatus(ViewHolder holder, Conversation conversation) {
        // TODO: серый цвет текста?
    }

    private void bindText(ViewHolder holder, Conversation conversation) {
        User author = conversation.recipient;
        String title = ConversationHelper.getInstance().getTitleWithoutUserPrefix(conversation, holder.title.getContext());
        SpannableStringBuilder ssb = new SpannableStringBuilder(title);
        UiUtils.setNicknameSpans(ssb, 0, ssb.length(), conversation.isGroup() ? -1 : author.getId(),
                holder.itemView.getContext(), R.style.TextAppearanceSlugInlineGreen);
        holder.title.setText(ssb);
        holder.messageText.setText(formatLastMessageText(conversation, holder.messageText.getResources()));
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

    private CharSequence formatLastMessageText(Conversation conversation, Resources resources) {
        if (conversation.lastMessage != null) {
            if(!UiUtils.isBlank(conversation.lastMessage.contentHtml)) {
                return Html.fromHtml(conversation.lastMessage.contentHtml);
            } else {
                SpannableStringBuilder ssb;
                if (conversation.lastMessage.getFirstImageAttachment() != null) {
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

        public final TextView messageText;

        public final ImageView messageAvatar;

        public final RelativeDateTextSwitcher date;

        public final TextView msgCount;

        public final View unreceivedIndicator;

        public ViewHolder(View v) {
            super(v);
            avatar = (HintedExtendedImageView) v.findViewById(R.id.avatar);
            title = (TextView) v.findViewById(R.id.title);
            date = (RelativeDateTextSwitcher) v.findViewById(R.id.notification_date);
            msgCount = (TextView) v.findViewById(R.id.unread_messages_count);
            unreceivedIndicator = v.findViewById(R.id.unreceived_messages_indicator);
            messageAvatar = (ImageView) v.findViewById(R.id.message_avatar);
            messageText = (TextView) v.findViewById(R.id.last_message);
        }
    }

    public boolean isEmpty() {
        return mConversations.isEmpty();
    }
}
