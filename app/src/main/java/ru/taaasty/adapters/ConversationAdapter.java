package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.RemovedUserMessages;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.UpdateMessages;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Attachment;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.DefaultUserpicDrawable;
import ru.taaasty.widgets.RelativeDateTextSwitcher;


public abstract class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationAdapter";

    public static final int VIEW_TYPE_HEADER_MORE_BUTTON = R.id.conversation_view_more_button;
    public static final int VIEW_TYPE_MY_MESSAGE = R.id.conversation_view_my_message;
    public static final int VIEW_TYPE_MY_MESSAGE_UPLOAD_IN_PROGRESS = R.id.conversation_view_my_message_upload_in_progress;
    public static final int VIEW_TYPE_THEIR_MESSAGE = R.id.conversation_view_their_message;
    public static final int VIEW_TYPE_SYSTEM_MESSAGE = R.id.conversation_view_system_message;

    private Session mSession;

    private final LayoutInflater mInflater;

    private final MessageSortedList mMessages;

    private boolean showLoadMoreButton;

    protected TlogDesign mFeedDesign;

    private final ImageUtils mImageUtils;

    private ImageLoadingGetter mImageGetterMyMessage;

    private ImageLoadingGetter mImageGetterTheirMessage;

    private Conversation mConversation = null;

    public ConversationAdapter(Context context) {
        super();
        mMessages = new MessageSortedList();
        mInflater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mSession = Session.getInstance();
        mImageUtils = ImageUtils.getInstance();
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View res;
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case VIEW_TYPE_HEADER_MORE_BUTTON:
                holder = onCreateHeaderViewHolder(parent, viewType);
                break;
            case VIEW_TYPE_MY_MESSAGE:
                res = mInflater.inflate(R.layout.conversation_my_message, parent, false);

                ConversationAdapter.ViewHolderMessage myHolder;
                myHolder = ViewHolderMessage.createMyMessageHolder(res);
                if (mImageGetterMyMessage == null) {
                    // Вычисление примерного максимального размера бабла с текстом
                    // TODO придумать что-нибудь поприличнее, чтобы не зависеть от разметки
                    int maxBubbleTextSize = getParentWidth(parent);

                    ViewGroup.MarginLayoutParams lpItemView = (ViewGroup.MarginLayoutParams) myHolder.itemView.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - myHolder.itemView.getPaddingLeft() - myHolder.itemView.getPaddingRight()
                            - lpItemView.leftMargin - lpItemView.rightMargin;

                    ViewGroup.MarginLayoutParams lpText = (ViewGroup.MarginLayoutParams) myHolder.text.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - myHolder.text.getPaddingLeft() - myHolder.text.getPaddingRight()
                            - lpText.leftMargin - lpText.rightMargin;

                    mImageGetterMyMessage = new ImageLoadingGetter(maxBubbleTextSize, parent.getContext());
                }
                holder = myHolder;
                break;
            case VIEW_TYPE_MY_MESSAGE_UPLOAD_IN_PROGRESS:
                res = mInflater.inflate(R.layout.conversation_my_message_upload_in_progress, parent, false);
                myHolder = ViewHolderMessage.createMyMessageHolder(res);
                if (mImageGetterMyMessage == null) {
                    // Вычисление примерного максимального размера бабла с текстом
                    // TODO придумать что-нибудь поприличнее, чтобы не зависеть от разметки
                    int maxBubbleTextSize = getParentWidth(parent);

                    ViewGroup.MarginLayoutParams lpItemView = (ViewGroup.MarginLayoutParams) myHolder.itemView.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - myHolder.itemView.getPaddingLeft() - myHolder.itemView.getPaddingRight()
                            - lpItemView.leftMargin - lpItemView.rightMargin;

                    ViewGroup.MarginLayoutParams lpText = (ViewGroup.MarginLayoutParams) myHolder.text.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - myHolder.text.getPaddingLeft() - myHolder.text.getPaddingRight()
                            - lpText.leftMargin - lpText.rightMargin;

                    mImageGetterMyMessage = new ImageLoadingGetter(maxBubbleTextSize, parent.getContext());
                }
                holder = myHolder;
                break;
            case VIEW_TYPE_THEIR_MESSAGE:
                res = mInflater.inflate(R.layout.conversation_their_message, parent, false);
                ConversationAdapter.ViewHolderMessage theirHolder = ViewHolderMessage.createTheirMessageHolder(res);
                if (mImageGetterTheirMessage == null) {
                    int maxBubbleTextSize;
                    maxBubbleTextSize = getParentWidth(parent);

                    ViewGroup.MarginLayoutParams lpItemView = (ViewGroup.MarginLayoutParams) theirHolder.itemView.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - theirHolder.itemView.getPaddingLeft() - theirHolder.itemView.getPaddingRight()
                            - lpItemView.leftMargin - lpItemView.rightMargin;

                    ViewGroup.MarginLayoutParams lpText = (ViewGroup.MarginLayoutParams) theirHolder.text.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - lpText.leftMargin - lpText.rightMargin
                            - theirHolder.text.getPaddingLeft() - theirHolder.text.getPaddingRight()
                    ;

                    ViewGroup.MarginLayoutParams lpAvatar = (ViewGroup.MarginLayoutParams) theirHolder.avatar.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - theirHolder.avatar.getPaddingLeft() - theirHolder.avatar.getPaddingRight()
                            - lpAvatar.leftMargin - lpAvatar.rightMargin - lpAvatar.width;

                    mImageGetterTheirMessage = new ImageLoadingGetter(maxBubbleTextSize, parent.getContext());
                }
                holder = theirHolder;
                break;
            case VIEW_TYPE_SYSTEM_MESSAGE:
                View child = mInflater.inflate(R.layout.conversation_system_message, parent, false);
                holder = new SystemMessageHolder(child);
                break;
            default:
                throw new IllegalArgumentException();
        }

        initClickListeners(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (isLoadMoreIndicatorPosition(position)) {
            bindHeader(viewHolder, position);
        } else {
            Message message = mMessages.get(getFeedLocation(position));
            if (message.isSystemMessage()) {
                bindSystemMessage((SystemMessageHolder) viewHolder, position);
            } else {
                bindMessage((ViewHolderMessage) viewHolder, message);
            }
        }
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof ViewHolderMessage) {
            ((ViewHolderMessage) holder).textImgLoader.reset();
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size() + getHeadersCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoadMoreIndicatorPosition(position)) return VIEW_TYPE_HEADER_MORE_BUTTON;

        Message message = mMessages.get(getFeedLocation(position));
        if (message.isSystemMessage()) return VIEW_TYPE_SYSTEM_MESSAGE;

        boolean isFromMe;
        if (mConversation != null) {
            isFromMe = message.isFromMe(mConversation);
        } else {
            isFromMe = mSession.isMe(message.getUserId()); // Есть шанс, что попадем
        }
        if (isFromMe && !message.isOnServer && !message.getImageAttachments().isEmpty()) {
            return VIEW_TYPE_MY_MESSAGE_UPLOAD_IN_PROGRESS;
        } else if (isFromMe) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_THEIR_MESSAGE;
        }
    }

    @Override
    public long getItemId(int position) {
        Message message = getMessage(position);
        return message != null ? message.id : RecyclerView.NO_ID;
    }

    public void bindSystemMessage(SystemMessageHolder holder, int position) {
        Message message = getMessage(position);
        holder.messageText.setText(message.contentHtml);
    }

    @Nullable
    public Message getMessage(int adapterPosition) {
        if (!isPositionInFeed(adapterPosition)) return null;
        return mMessages.get(getFeedLocation(adapterPosition));
    }

    public void addMessages(Message[] messages) {
        mMessages.addOrUpdateItems(messages);
    }

    public void changeMessages(RemovedUserMessages.RemovedMessage messages[]) {
        if (mConversation == null) return;
        mMessages.beginBatchedUpdates();
        for (int i = mMessages.size() - 1; i >= 0; --i) {
            Message old = mMessages.get(i);
            for (RemovedUserMessages.RemovedMessage msg: messages) {
                if (msg.id == old.id) {
                    Message newMessage = old.newBuilder()
                            .contentHtml(msg.content)
                            .type(msg.type)
                            .build();
                    mMessages.add(newMessage);
                    break;
                }
            }
        }
        mMessages.endBatchedUpdates();
    }

    /**
     * Скрытие своих сообщений
     * Удаляем из списка соощения при условии, что они мои (может быть вызвано, если я
     *  удаляю сообщения в браузере только для себя и слежу за изменениями в телефоне)
     * @param messageIds
     */
    public void hideMyMessages(long[] messageIds) {
        if (mConversation == null) return;
        mMessages.beginBatchedUpdates();
        for (int i = mMessages.size() - 1; i >= 0; --i) {
            Message m = getMessage(i);
            if (!m.isFromMe(mConversation)) continue;
            if (ArrayUtils.contains(messageIds, m.id)) mMessages.removeItemAt(i);
        }
        mMessages.endBatchedUpdates();
    }

    public void addMessage(Message message) {
        mMessages.addOrUpdate(message);
    }

    public void markMessagesAsRead(List<UpdateMessages.UpdateMessageInfo> messageInfos) {
        if (messageInfos == null) return;
        LongSparseArray<UpdateMessages.UpdateMessageInfo> infosHash = new LongSparseArray<>(messageInfos.size());
        for (UpdateMessages.UpdateMessageInfo info : messageInfos) infosHash.append(info.id, info);

        int size = mMessages.size();
        for (int i = 0; i < size; ++i) {
            UpdateMessages.UpdateMessageInfo info = infosHash.get(mMessages.get(i).id);
            if (info != null) {
                if ((mMessages.get(i).readAt == null)
                        || (Math.abs(mMessages.get(i).readAt.getTime() - info.readAt.getTime()) > 5000)) {
                    mMessages.get(i).readAt = info.readAt;
                    notifyItemChanged(getAdapterPosition(i));
                }
            }
        }
    }

    public void markMessagesAsRead(Collection<Long> messageIds, Date readAt) {
        int size = mMessages.size();
        for (int i = 0; i < size; ++i) {
            if (messageIds.contains(mMessages.get(i).id)) {
                long date1 = mMessages.get(i).readAt == null ? 0 : mMessages.get(i).readAt.getTime();
                long date2 = readAt == null ? 0 : readAt.getDate();
                if (Math.abs(date1 - date2) > 5000) {
                    mMessages.get(i).readAt = readAt;
                    notifyItemChanged(getAdapterPosition(i));
                }
            }
        }
    }

    public boolean isEmpty() {
        return mMessages.isEmpty();
    }

    public int getMessagesCount() {
        return mMessages.size();
    }

    public abstract void initClickListeners(RecyclerView.ViewHolder holder);

    public int getLastPosition() {
        return mMessages.size() + getHeadersCount() - 1;
    }

    @Nullable
    public Message getMessage(ViewHolderMessage holder) {
        if (holder.getPosition() == RecyclerView.NO_POSITION) return null;
        return mMessages.get(getFeedLocation(holder.getPosition()));
    }

    @Nullable
    public Integer findPositionById(long messageId) {
        int size = mMessages.size();
        for (int i = 0; i < size; ++i) {
            if (mMessages.get(i).id == messageId) return getAdapterPosition(i);
        }
        return null;
    }

    @Nullable
    public Long getTopMessageId() {
        if (mMessages.isEmpty()) return null;
        return mMessages.get(0).id;
    }

    public void setConversation(Conversation conversation) {
        if (Objects.equals(mConversation, conversation)) return;
        mConversation = conversation;
        if (conversation.getType() == Conversation.Type.PRIVATE) {
            mFeedDesign = ((PrivateConversation)conversation).getRecipient().getDesign();
        }
        notifyDataSetChanged();
    }


    public void setShowLoadMoreButton(boolean show) {
        if (show != showLoadMoreButton) {
            showLoadMoreButton = show;
            notifyDataSetChanged();
        }
    }

    protected boolean isLoadMoreIndicatorPosition(int position) {
        if (!showLoadMoreButton) {
            return false;
        } else {
            return position == 0;
        }
    }

    protected boolean isPositionInFeed(int position) {
        return position != RecyclerView.NO_POSITION
                && !isLoadMoreIndicatorPosition(position);
    }

    private int getHeadersCount() {
        return showLoadMoreButton ? 1 : 0;
    }

    private int getFeedLocation(int adapterPosition) {
        return adapterPosition - getHeadersCount();
    }

    private int getAdapterPosition(int feedLocation) {
        return feedLocation + getHeadersCount();
    }

    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType);

    protected abstract void bindHeader(RecyclerView.ViewHolder holder, int position);

    @Nullable
    protected abstract User getMember(long userUuid);

    private void bindMessage(ViewHolderMessage holder, Message message) {
        bindMessageText(holder, message);
        bindMessageDate(holder, message);
        bindAvatar(holder, message);
    }

    private void bindMessageText(ViewHolderMessage holder, Message message) {
        SpannableStringBuilder text;

        Html.ImageGetter imageGetter = holder.isMyMessage ? mImageGetterMyMessage : mImageGetterTheirMessage;

        text = new SpannableStringBuilder();
        // Аттачи.
        // TODO более вменяемый вариант
        List<Attachment> attachmentList = message.getImageAttachments();
        boolean isFirst = true;
        for (Attachment attachment : attachmentList) {
            if (isFirst) {
                isFirst = false;
            } else {
                text.append('\n');
            }
            Drawable defaultDrawable = imageGetter.getDrawable(attachment.url);
            ImageSpan span = new ImageSpan(defaultDrawable, attachment.url);

            int len = text.length();
            text.append("<img>");
            text.setSpan(span, len, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Текст
        CharSequence msg = UiUtils.removeTrailingWhitespaces(Html.fromHtml(message.contentHtml, imageGetter, null));
        msg = UiUtils.replaceUrlSpans(msg, true);
        if (!UiUtils.isBlank(msg)) {
            if (!attachmentList.isEmpty()) text.append("\n");
            text.append(msg);
        }

        holder.text.setText(text);

        holder.textImgLoader.loadImages(holder.text);
    }

    private void bindAvatar(ViewHolderMessage holder, Message message) {
        if (holder.isMyMessage) {
            return;
        }

        if (mConversation != null && mConversation.getType() == Conversation.Type.PRIVATE) {
            holder.avatar.setVisibility(View.GONE);
            Picasso.with(holder.itemView.getContext()).cancelRequest(holder.avatar);
        } else {
            User user = getMember(message.getUserId());
            if (user == null
                    && mConversation != null
                    && mConversation.getType() == Conversation.Type.PUBLIC
                    && ((PublicConversation)mConversation).isAnonymous()) {
                DefaultUserpicDrawable drawable = DefaultUserpicDrawable.createAnonymousDefault(holder.itemView.getContext());
                mImageUtils.loadAvatarToImageView(user, R.dimen.avatar_small_diameter, holder.avatar, drawable);
            } else {
                mImageUtils.loadAvatarToImageView(user, R.dimen.avatar_small_diameter, holder.avatar);
            }
        }
    }

    private void bindMessageDate(ViewHolderMessage holder, Message message) {
        holder.relativeDate.setRelativeDate(message.createdAt.getTime());
        if (holder.isMyMessage) {
            //!!!!
            int drawableResId = message.readAt == null ? R.drawable.ic_done_grey_10dp : R.drawable.ic_done_all_grey_10dp;
//            holder.relativeDate.setCompoundDrawablesWithIntrinsicBounds(null, , iconId, 0);
                        Drawable drawable = ResourcesCompat.getDrawable(holder.itemView.getResources(),
                    drawableResId, null).mutate();
            if (message.isOnServer) {
                drawable.setColorFilter(holder.itemView.getResources().getColor(R.color.text_color_green), PorterDuff.Mode.SRC_ATOP);
            }
            holder.relativeDate.setCompoundDrawablesWithIntrinsicBounds(null, null , drawable, null);
        }
    }

    private int getParentWidth(ViewGroup parent) {
        int parentWidth = parent.getWidth();
        if (parentWidth == 0) {
            WindowManager wm = (WindowManager) parent.getContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) parent.getLayoutParams();
            parentWidth = size.x - lp.leftMargin - lp.rightMargin;
            if (DBG) Log.v(TAG, "display width: " + size.x);
        } else {
            if (DBG) Log.v(TAG, "parent width: " + parentWidth);
        }
        return parentWidth - parent.getPaddingLeft() - parent.getPaddingRight();
    }

    private final class MessageSortedList extends SortedList<Message> {
        @Override
        public void updateItemAt(int index, Message newMessageValue) {
            Message oldMessageValue = get(index);
            if (!newMessageValue.isOnServer && oldMessageValue.isOnServer) return;
            // if a sent message already in the list leave old url, that point on a file from the disk
            if (!oldMessageValue.isOnServer){
                newMessageValue.attachments = oldMessageValue.attachments;
            }
            super.updateItemAt(index, newMessageValue);
        }

        public MessageSortedList() {
            super(Message.class, new Callback<Message>() {

                @Override
                public int compare(Message o1, Message o2) {
                    return Message.ORDER_BY_CREATE_DATE_ASC_ID_COMPARATOR.compare(o1, o2);
                }

                @Override
                public void onInserted(int position, int count) {
                    notifyItemRangeInserted(getAdapterPosition(position), count);
                }

                @Override
                public void onRemoved(int position, int count) {
                    notifyItemRangeRemoved(getAdapterPosition(position), count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    notifyItemMoved(getAdapterPosition(fromPosition), getAdapterPosition(toPosition));
                }

                @Override
                public void onChanged(int position, int count) {
                    notifyItemRangeChanged(getAdapterPosition(position), count);
                }

                @Override
                public boolean areContentsTheSame(Message oldItem, Message newItem) {
                    // Тут огромное количество левых полей, которые не нужно сравнивать, поэтому сравниваем вручную
                    // Пропускаем: uuid, recipient, userId
                    //if (oldItem.id != newItem.id) return false;
                    //if (oldItem.userId != newItem.userId) return false;
                    if (oldItem.conversationId != newItem.conversationId) return false;
                    if (oldItem.getRecipientId() != newItem.getRecipientId()) return false;
                    if (oldItem.createdAt != null ? !oldItem.createdAt.equals(newItem.createdAt) : newItem.createdAt != null)
                        return false;
                    if (oldItem.readAt != null ? !oldItem.readAt.equals(newItem.readAt) : newItem.readAt != null)
                        return false;
                    if (oldItem.contentHtml != null ? !oldItem.contentHtml.equals(newItem.contentHtml) : newItem.contentHtml != null)
                        return false;
                    return true;
                }

                @Override
                public boolean areItemsTheSame(Message item1, Message item2) {
                    if (item1.id==0 || item2.id==0){
                        return  item1.uuid.equals(item2.uuid);
                    }else {
                        if (item1.id == item2.id) return true;
                        if (item1.uuid != null && !item1.uuid.isEmpty() && item1.uuid.equals(item2.uuid))
                            return true;
                        return false;
                    }
                }
            });
        }
    }

    public static class ViewHolderMessage extends RecyclerView.ViewHolder {

        public final boolean isMyMessage;

        @Nullable
        public final ImageView avatar;

        public final TextView text;

        public final RelativeDateTextSwitcher relativeDate;

        public final TextViewImgLoader textImgLoader;

        public static ViewHolderMessage createMyMessageHolder(View root) {
            return new ViewHolderMessage(root, true);
        }

        public static ViewHolderMessage createTheirMessageHolder(View root) {
            return new ViewHolderMessage(root, false);
        }

        private ViewHolderMessage(View v, boolean isMyMessage) {
            super(v);
            this.isMyMessage = isMyMessage;
            avatar = (ImageView) v.findViewById(R.id.avatar);
            text = (TextView) v.findViewById(R.id.message);
            text.setMovementMethod(LinkMovementMethodNoSelection.getInstance());
            relativeDate = (RelativeDateTextSwitcher) v.findViewById(R.id.relative_date);
            textImgLoader = new TextViewImgLoader(v.getContext(), SHOW_PHOTO_ON_CLICK_LISTENER);
            textImgLoader.setMaxHeight(v.getResources().getDimensionPixelSize(R.dimen.conversation_text_image_height));
            //textImgLoader.setMaxWidth(v.getResources().getDimensionPixelSize(R.dimen.conversation_text_image_width));

        }
    }

    public static class SystemMessageHolder extends RecyclerView.ViewHolder {

        TextView messageText;

        public SystemMessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.message);
        }
    }

    private static final TextViewImgLoader.OnClickListener SHOW_PHOTO_ON_CLICK_LISTENER = new TextViewImgLoader.OnClickListener() {
        @Override
        public void onImageClicked(TextView widget, String source) {
            CharSequence seq = widget.getText();
            ArrayList<String> sources = UiUtils.getImageSpanUrls(seq);
            ShowPhotoActivity.startShowPhotoActivity(widget.getContext(), "", sources, null, widget);
        }
    };

}
