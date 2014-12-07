package ru.taaasty.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.SortedList;
import ru.taaasty.UserManager;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.UpdateMessages;
import ru.taaasty.model.User;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;


//  TODO првоерять что мы получаем сообщения от pusher'а
public abstract class ConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ru.taaasty.adapters.ConversationAdapter";

    private static final int VIEW_TYPE_HEADER = R.id.conversation_view_header;
    private static final int VIEW_TYPE_MY_MESSAGE = R.id.conversation_view_my_message;
    private static final int VIEW_TYPE_THEIR_MESSAGE = R.id.conversation_view_their_message;

    private static final int HEADERS_COUNT = 1;

    private UserManager mUserManager;

    private final LayoutInflater mInflater;

    private final MessageFeed mMessages;

    protected TlogDesign mFeedDesign;

    private final AtomicBoolean mLoading = new AtomicBoolean();

    private final ImageUtils mImageUtils;

    public ConversationAdapter(Context context) {
        super();
        mMessages = new MessageFeed();
        mInflater = LayoutInflater.from(context);
        mFeedDesign = TlogDesign.DUMMY;
        mUserManager = UserManager.getInstance();
        mImageUtils = ImageUtils.getInstance();
        setHasStableIds(true);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View res;
        RecyclerView.ViewHolder holder;
        switch (viewType) {
            case VIEW_TYPE_HEADER:
                holder = onCreateHeaderViewHolder(parent);
                break;
            case VIEW_TYPE_MY_MESSAGE:
                res = mInflater.inflate(R.layout.conversation_my_message, parent, false);
                holder = ViewHolderMessage.createMyMessageHolder(res);
                break;
            case VIEW_TYPE_THEIR_MESSAGE:
                res = mInflater.inflate(R.layout.conversation_their_message, parent, false);
                holder = ViewHolderMessage.createTheirMessageHolder(res);
                break;
            default:
                throw new IllegalArgumentException();
        }

        initClickListeners(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        if (isHeaderPosition(position)) {
            bindHeader(viewHolder);
        } else if (isPendingIndicatorPosition(position)) {
            // Ничего не делаем
        } else {
            bindMessage((ViewHolderMessage) viewHolder, mMessages.get(getFeedLocation(position)));
        }
    }

    @Override
    public int getItemCount() {
        return mMessages.size() + HEADERS_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return VIEW_TYPE_HEADER;
        }

        Conversation.Message message = mMessages.get(getFeedLocation(position));
        if (mUserManager.isMe(message.userId)) {
            return VIEW_TYPE_MY_MESSAGE;
        } else {
            return VIEW_TYPE_THEIR_MESSAGE;
        }
    }

    @Override
    public long getItemId(int position) {
        return isPositionInFeed(position) ? mMessages.get(getFeedLocation(position)).id : RecyclerView.NO_ID;
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().addOnScrollChangedListener((IParallaxedHeaderHolder)holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof IParallaxedHeaderHolder) {
            holder.itemView.getViewTreeObserver().removeOnScrollChangedListener((IParallaxedHeaderHolder) holder);
        }
    }

    public void addMessages(List<Conversation.Message> messages) {
        mMessages.insertItems(messages);
    }

    public void addMessage(Conversation.Message message) {
        mMessages.insertItem(message);
    }

    public void markMessagesAsRead(List<UpdateMessages.UpdateMessageInfo> messageInfos) {
        if (messageInfos == null) return;
        LongSparseArray<UpdateMessages.UpdateMessageInfo> infosHash = new LongSparseArray<>(messageInfos.size());
        for (UpdateMessages.UpdateMessageInfo info: messageInfos) infosHash.append(info.id, info);

        int size = mMessages.size();
        for (int i = 0; i < size; ++i) {
            UpdateMessages.UpdateMessageInfo info = infosHash.get(mMessages.get(i).id);
            if (info != null) {
                mMessages.get(i).readAt = info.readAt;
                notifyItemChanged(getAdapterPosition(i));
            }
        }
    }

    public void markMessagesAsRead(Collection<Long> messageIds, Date readAt) {
        int size = mMessages.size();
        for (int i = 0; i < size; ++i) {
            if (messageIds.contains(mMessages.get(i).id)) {
                mMessages.get(i).readAt = readAt;
                notifyItemChanged(getAdapterPosition(i));
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
        return mMessages.size() + HEADERS_COUNT - 1;
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

    public void setLoading(boolean newValue) {
        if (mLoading.compareAndSet(!newValue, newValue)) {
            notifyDataSetChanged();
        }
    }

    public boolean isLoading() {
        return mLoading.get();
    }

    private boolean isHeaderPosition(int position) {
        return position == 0;
    }

    private boolean isPendingIndicatorPosition(int position) {
        return position == mMessages.size() + HEADERS_COUNT;
    }

    private boolean isPositionInFeed(int position) {
        return position != RecyclerView.NO_POSITION
                && !isHeaderPosition(position)
                && !isPendingIndicatorPosition(position);
    }

    private int getFeedLocation(int adapterPosition) {
        return adapterPosition - HEADERS_COUNT;
    }

    private int getAdapterPosition(int feedLocation) {
        return feedLocation + HEADERS_COUNT;
    }

    protected abstract RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent);

    protected abstract void bindHeader(RecyclerView.ViewHolder holder);

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

        if (author != null) {
            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append("@");
            ssb.append(author.getSlug());
            UiUtils.setNicknameSpans(ssb, 0, ssb.length(), author.getId(), holder.itemView.getContext(), R.style.TextAppearanceSlugInlineGreen);
            ssb.append(' ');
            if (!TextUtils.isEmpty(message.contentHtml)) {
                ssb.append(Html.fromHtml(message.contentHtml));
            }
            text = ssb;
        } else {
            // Автор неизвестен (не должно такого быть, но на всякий случай)
            text = TextUtils.isEmpty(message.contentHtml) ? null : Html.fromHtml(message.contentHtml);
        }

        holder.text.setText(text);
    }

    private void bindAvatar(ViewHolderMessage holder, Conversation.Message message) {
        User user = getMember(message.userId);
        mImageUtils.loadAvatar(user, holder.avatar, R.dimen.avatar_small_diameter);
    }

    // TODO: обновление даты
    // TODO: кривой индикатор
    private void bindMessageDate(ViewHolderMessage holder, Conversation.Message message) {
        DateFormat df = DateFormat.getDateTimeInstance();
        String date;
        if (message.readAt != null) {
            date = df.format(message.readAt);
        } else {
            date = df.format(message.createdAt);
        }
        holder.date.setText(date);

        if (holder.isMyMessage) {
            holder.date.setCompoundDrawablesWithIntrinsicBounds(0, 0,
                    message.readAt == null ? R.drawable.ic_done_grey600_18dp : R.drawable.ic_done_all_grey600_18dp, 0);
        }
    }

    private final class MessageFeed extends SortedList<Conversation.Message> implements SortedList.OnListChangedListener {

        public MessageFeed() {
            super(Conversation.Message.SORT_BY_ID_COMPARATOR);
            setListener(this);
        }

        @Override
        public long getItemId(Conversation.Message item) {
            return item.id;
        }

        @Override
        public void onDataSetChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemChanged(int location) {
            notifyItemChanged(getAdapterPosition(location));
        }

        @Override
        public void onItemInserted(int location) {
            notifyItemInserted(getAdapterPosition(location));
        }

        @Override
        public void onItemRemoved(int location) {
            notifyItemRemoved(getAdapterPosition(location));
        }

        @Override
        public void onItemMoved(int fromLocation, int toLocation) {
            notifyItemMoved(getAdapterPosition(fromLocation), getAdapterPosition(toLocation));
        }

        @Override
        public void onItemRangeChanged(int locationStart, int itemCount) {
            notifyItemRangeChanged(getAdapterPosition(locationStart), itemCount);
        }

        @Override
        public void onItemRangeInserted(int locationStart, int itemCount) {
            notifyItemRangeChanged(getAdapterPosition(locationStart), itemCount);
        }

        @Override
        public void onItemRangeRemoved(int locationStart, int itemCount) {
            notifyItemRangeRemoved(getAdapterPosition(locationStart), itemCount);
        }
    }

    public static class ViewHolderMessage extends RecyclerView.ViewHolder {

        public final boolean isMyMessage;

        public final ImageView avatar;

        public final TextView text;

        public final TextView date;

        public static ViewHolderMessage createMyMessageHolder(View root) {
            return new ViewHolderMessage(root, true);
        }

        public static ViewHolderMessage createTheirMessageHolder(View root) {
            return new ViewHolderMessage(root, false);
        }

        private ViewHolderMessage(View v, boolean isMyMessage) {
            super(v);
            this.isMyMessage = isMyMessage;
            avatar = (ImageView)v.findViewById(R.id.avatar);
            text = (TextView)v.findViewById(R.id.message);
            date = (TextView)v.findViewById(R.id.date);
        }
    }
}
