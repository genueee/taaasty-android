package ru.taaasty.adapters;

import android.content.Context;
import android.graphics.Point;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.SortedList;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.UpdateMessages;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.ImageLoadingGetter;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.LinkMovementMethodNoSelection;
import ru.taaasty.utils.TextViewImgLoader;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.RelativeDateTextSwitcher;


public abstract class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationAdapter";

    public static final int VIEW_TYPE_HEADER_MORE_BUTTON = R.id.conversation_view_more_button;
    public static final int VIEW_TYPE_MY_MESSAGE = R.id.conversation_view_my_message;
    public static final int VIEW_TYPE_THEIR_MESSAGE = R.id.conversation_view_their_message;

    private Session mSession;

    private final LayoutInflater mInflater;

    private final MessageFeed mMessages;

    private boolean showLoadMoreButton;

    protected TlogDesign mFeedDesign;

    private final ImageUtils mImageUtils;

    private ImageLoadingGetter mImageGetterMyMessage;

    private ImageLoadingGetter mImageGetterTheirMessage;

    public ConversationAdapter(Context context) {
        super();
        mMessages = new MessageFeed();
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

                    ViewGroup.MarginLayoutParams lpAvatar = (ViewGroup.MarginLayoutParams) myHolder.avatar.getLayoutParams();
                    maxBubbleTextSize = maxBubbleTextSize
                            - myHolder.avatar.getPaddingLeft() - myHolder.avatar.getPaddingRight()
                            - lpAvatar.leftMargin - lpAvatar.rightMargin - lpAvatar.width
                    ;

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
            bindMessage((ViewHolderMessage) viewHolder, mMessages.get(getFeedLocation(position)));
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
        if (isLoadMoreIndicatorPosition(position)) {
            return VIEW_TYPE_HEADER_MORE_BUTTON;
        }

        Conversation.Message message = mMessages.get(getFeedLocation(position));
        if (mSession.isMe(message.userId)) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_THEIR_MESSAGE;
        }
    }

    @Override
    public long getItemId(int position) {
        Conversation.Message message = getMessage(position);
        return message != null ? message.id : RecyclerView.NO_ID;
    }

    @Nullable
    public Conversation.Message getMessage(int adapterPosition) {
        if (!isPositionInFeed(adapterPosition)) return null;
        return mMessages.get(getFeedLocation(adapterPosition));
    }

    public void addMessages(Conversation.Message[] messages) {
        mMessages.addOrUpdateItems(messages);
    }

    public void addMessage(Conversation.Message message) {
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
    public Conversation.Message getMessage(ViewHolderMessage holder) {
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

    public void setFeedDesign(TlogDesign design) {
        mFeedDesign = design;
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

    private void bindMessage(ViewHolderMessage holder, Conversation.Message message) {
        bindMessageText(holder, message);
        bindMessageDate(holder, message);
        bindAvatar(holder, message);
    }

    private void bindMessageText(ViewHolderMessage holder, Conversation.Message message) {
        User author = getMember(message.userId);
        CharSequence text;

        Html.ImageGetter imageGetter = holder.isMyMessage ? mImageGetterMyMessage : mImageGetterTheirMessage;

        CharSequence msg = UiUtils.removeTrailingWhitespaces(Html.fromHtml(message.contentHtml, imageGetter, null));
        msg = UiUtils.replaceUrlSpans(msg, true);

        if (author != null) {
            SpannableStringBuilder ssb = new SpannableStringBuilder(author.getNameWithPrefix());
            UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), holder.itemView.getContext(), R.style.TextAppearanceSlugInlineGreen);
            ssb.append(' ');
            if (!TextUtils.isEmpty(msg)) {
                ssb.append(msg);
            }
            text = ssb;
        } else {
            // Автор неизвестен, скорее всего, переписка ещё не загружена
            text = TextUtils.isEmpty(msg) ? null : msg;
        }

        holder.text.setText(text);
        holder.textImgLoader.loadImages(holder.text);
    }

    private void bindAvatar(ViewHolderMessage holder, Conversation.Message message) {
        User user = getMember(message.userId);
        mImageUtils.loadAvatarToImageView(user, R.dimen.avatar_small_diameter, holder.avatar);
    }

    private void bindMessageDate(ViewHolderMessage holder, Conversation.Message message) {
        holder.relativeDate.setRelativeDate(message.createdAt.getTime());
        if (holder.isMyMessage) {
            holder.relativeDate.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    message.readAt == null ? R.drawable.ic_done_grey_10dp : R.drawable.ic_done_all_grey_10dp, 0);
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

    private final class MessageFeed extends SortedList<Conversation.Message> {

        public MessageFeed() {
            super(Conversation.Message.class, new Callback<Conversation.Message>() {
                @Override
                public int compare(Conversation.Message o1, Conversation.Message o2) {
                    return Conversation.Message.SORT_BY_ID_COMPARATOR.compare(o1, o2);
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
                public boolean areContentsTheSame(Conversation.Message oldItem, Conversation.Message newItem) {
                    // Тут огромное количество левых полей, которые не нужно сравнивать, поэтому сравниваем вручную
                    // Пропускаем: uuid, recipient, userId
                    //if (oldItem.id != newItem.id) return false;
                    //if (oldItem.userId != newItem.userId) return false;
                    if (oldItem.conversationId != newItem.conversationId) return false;
                    if (oldItem.recipientId != newItem.recipientId) return false;
                    if (oldItem.createdAt != null ? !oldItem.createdAt.equals(newItem.createdAt) : newItem.createdAt != null)
                        return false;
                    if (oldItem.readAt != null ? !oldItem.readAt.equals(newItem.readAt) : newItem.readAt != null)
                        return false;
                    if (oldItem.contentHtml != null ? !oldItem.contentHtml.equals(newItem.contentHtml) : newItem.contentHtml != null)
                        return false;
                    return true;
                }

                @Override
                public boolean areItemsTheSame(Conversation.Message item1, Conversation.Message item2) {
                    if (item1.id == item2.id) return true;
                    if (item1.uuid != null && !item1.uuid.isEmpty() && item1.uuid.equals(item2.uuid))
                        return true;
                    return false;
                }
            });
        }
    }

    public static class ViewHolderMessage extends RecyclerView.ViewHolder {

        public final boolean isMyMessage;

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
