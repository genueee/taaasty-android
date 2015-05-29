package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.Comparator;
import java.util.Date;

import ru.taaasty.UserManager;
import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 22.10.14.
 */
public class Conversation implements Parcelable {

    public long id = -1;

    public long userId = -1;

    public Date createdAt;

    public Date updatedAt;

    public long recipientId;

    public User recipient = User.DUMMY;

    public int unreadMessagesCount;

    public int unreceivedMessagesCount;

    public int messagesCount;

    public Message lastMessage = Message.DUMMY;

    /**
     * Сортировка по убыванию даты создания (более новые - в начале списка)
     */
    public static Comparator<Conversation> SORT_BY_LAST_MESSAGE_CREATED_AT_DESC_COMPARATOR = new Comparator<Conversation>() {
        @Override
        public int compare(Conversation lhs, Conversation rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            } else {
                Date rhsDate = (rhs.lastMessage == null ? rhs.createdAt : rhs.lastMessage.createdAt);
                Date lhsDate = (lhs.lastMessage == null ? lhs.createdAt : lhs.lastMessage.createdAt);
                int compareDates = rhsDate.compareTo(lhsDate);
                return compareDates != 0 ? compareDates : Objects.compare(rhs.id, lhs.id);
            }
        }
    };


    public static class Message implements Parcelable {

        public static Comparator<Message> SORT_BY_ID_COMPARATOR = new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
                // В диалоге с самим собой на каждое сообщение приходит 2 сообщения с разными ID и одним и тем же UUID
                if (!TextUtils.isEmpty(lhs.uuid) && lhs.uuid.equals(rhs.uuid)) return 0;
                return Objects.unsignedCompare(lhs.id, rhs.id);
            }
        };

        public static final Message DUMMY = new Message();

        public long id = -1;

        public long userId;

        public long conversationId;

        public long recipientId;

        @Nullable
        public String uuid;

        public Date createdAt;

        @Nullable
        public Date readAt;

        /**
         * Контеткт в HTML
         */
        public String contentHtml;


        @Nullable
        public Conversation conversation;

        public Message() {
        }

        public boolean isMarkedAsRead() {
            return readAt != null;
        }

        public boolean isFromMe() {
            return UserManager.getInstance().isMe(userId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Message message = (Message) o;

            if (id != message.id) return false;
            if (userId != message.userId) return false;
            if (conversationId != message.conversationId) return false;
            if (recipientId != message.recipientId) return false;
            if (uuid != null ? !uuid.equals(message.uuid) : message.uuid != null) return false;
            if (createdAt != null ? !createdAt.equals(message.createdAt) : message.createdAt != null)
                return false;
            if (readAt != null ? !readAt.equals(message.readAt) : message.readAt != null)
                return false;
            if (contentHtml != null ? !contentHtml.equals(message.contentHtml) : message.contentHtml != null)
                return false;
            return !(conversation != null ? !conversation.equals(message.conversation) : message.conversation != null);

        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (int) (userId ^ (userId >>> 32));
            result = 31 * result + (int) (conversationId ^ (conversationId >>> 32));
            result = 31 * result + (int) (recipientId ^ (recipientId >>> 32));
            result = 31 * result + (uuid != null ? uuid.hashCode() : 0);
            result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
            result = 31 * result + (readAt != null ? readAt.hashCode() : 0);
            result = 31 * result + (contentHtml != null ? contentHtml.hashCode() : 0);
            result = 31 * result + (conversation != null ? conversation.hashCode() : 0);
            return result;
        }

        @Override
        public int describeContents() {
            return 0;

        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.id);
            dest.writeLong(this.userId);
            dest.writeLong(this.conversationId);
            dest.writeLong(this.recipientId);
            dest.writeString(this.uuid);
            dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
            dest.writeLong(readAt != null ? readAt.getTime() : -1);
            dest.writeString(this.contentHtml);
            dest.writeParcelable(this.conversation, 0);
        }

        private Message(Parcel in) {
            this.id = in.readLong();
            this.userId = in.readLong();
            this.conversationId = in.readLong();
            this.recipientId = in.readLong();
            this.uuid = in.readString();
            long tmpCreatedAt = in.readLong();
            this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
            long tmpReadAt = in.readLong();
            this.readAt = tmpReadAt == -1 ? null : new Date(tmpReadAt);
            this.contentHtml = in.readString();
            this.conversation = in.readParcelable(Conversation.class.getClassLoader());
        }

        public static final Creator<Message> CREATOR = new Creator<Message>() {
            public Message createFromParcel(Parcel source) {
                return new Message(source);
            }

            public Message[] newArray(int size) {
                return new Message[size];
            }
        };
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.userId);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
        dest.writeLong(this.recipientId);
        dest.writeParcelable(this.recipient, 0);
        dest.writeInt(this.unreadMessagesCount);
        dest.writeInt(this.unreceivedMessagesCount);
        dest.writeInt(this.messagesCount);
        dest.writeParcelable(this.lastMessage, 0);
    }

    public Conversation() {
    }

    private Conversation(Parcel in) {
        this.id = in.readLong();
        this.userId = in.readLong();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
        this.recipientId = in.readLong();
        this.recipient = in.readParcelable(User.class.getClassLoader());
        this.unreadMessagesCount = in.readInt();
        this.unreceivedMessagesCount = in.readInt();
        this.messagesCount = in.readInt();
        this.lastMessage = in.readParcelable(Message.class.getClassLoader());
    }

    public static final Parcelable.Creator<Conversation> CREATOR = new Parcelable.Creator<Conversation>() {
        public Conversation createFromParcel(Parcel source) {
            return new Conversation(source);
        }

        public Conversation[] newArray(int size) {
            return new Conversation[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Conversation that = (Conversation) o;

        if (id != that.id) return false;
        if (userId != that.userId) return false;
        if (recipientId != that.recipientId) return false;
        if (unreadMessagesCount != that.unreadMessagesCount) return false;
        if (unreceivedMessagesCount != that.unreceivedMessagesCount) return false;
        if (messagesCount != that.messagesCount) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null)
            return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null)
            return false;
        if (recipient != null ? !recipient.equals(that.recipient) : that.recipient != null)
            return false;
        return !(lastMessage != null ? !lastMessage.equals(that.lastMessage) : that.lastMessage != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + (int) (recipientId ^ (recipientId >>> 32));
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        result = 31 * result + unreadMessagesCount;
        result = 31 * result + unreceivedMessagesCount;
        result = 31 * result + messagesCount;
        result = 31 * result + (lastMessage != null ? lastMessage.hashCode() : 0);
        return result;
    }
}
