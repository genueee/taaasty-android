package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by alexey on 22.10.14.
 */
public class Conversation implements Parcelable {

    public long id = -1;

    public long userId = -1;

    public Date createdAt;

    public Date updatedAt;

    public long recipient_id;

    public User recipient = User.DUMMY;

    public int unreadMessagesCount;

    public int unreceivedMessagesCount;

    public int messagesCount;

    public Message lastMessage = Message.DUMMY;

    public static class Message implements Parcelable {

        public static final Message DUMMY = new Message();

        public long id = -1;

        public long userId;

        public long recipientId;

        public Date created_at;

        public String content_html;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.id);
            dest.writeLong(this.userId);
            dest.writeLong(this.recipientId);
            dest.writeLong(created_at != null ? created_at.getTime() : -1);
            dest.writeString(this.content_html);
        }

        public Message() {
        }

        private Message(Parcel in) {
            this.id = in.readLong();
            this.userId = in.readLong();
            this.recipientId = in.readLong();
            long tmpCreated_at = in.readLong();
            this.created_at = tmpCreated_at == -1 ? null : new Date(tmpCreated_at);
            this.content_html = in.readString();
        }

        public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
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
        dest.writeLong(this.recipient_id);
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
        this.recipient_id = in.readLong();
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
