package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.Date;

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

    public static Comparator<Conversation> SORT_BY_CREATED_AT_COMPARATOR = new Comparator<Conversation>() {
        @Override
        public int compare(Conversation lhs, Conversation rhs) {
            int createdAtDiff = lhs.createdAt.compareTo(rhs.createdAt);
            if (createdAtDiff == 0) {
                return Objects.compare(lhs.id, rhs.id);
            } else {
                return createdAtDiff;
            }
        }
    };

    public static class Message implements Parcelable {

        public static Comparator<Message> SORT_BY_ID_COMPARATOR = new Comparator<Message>() {
            @Override
            public int compare(Message lhs, Message rhs) {
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

        public String contentHtml;

        public Message() {
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
}
