package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Date;

import ru.taaasty.rest.model.User;

/**
 * Приватный чат с юзером
 */
public class PrivateConversation extends Conversation implements Parcelable {

    /**
     * Собеседник.
     */
    private long recipientId;

    /**
     * Собеседник, кэшированная версия, в некоторых случаях не приходит
     */
    @Nullable
    private User recipient;


    public String getTitle() {
        return getRecipient().getNameWithPrefix();
    }

    @Nullable
    @Override
    public Type getType() {
        return Type.PRIVATE;
    }

    @Override
    public long toRealUserId(long fakeUserId) {
        return fakeUserId;
    }

    public User getRecipient() {
        return recipient != null ? recipient : User.DUMMY;
    }

    public long getRecipientId() {
        return recipientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PrivateConversation that = (PrivateConversation) o;

        if (recipientId != that.recipientId) return false;
        return recipient != null ? recipient.equals(that.recipient) : that.recipient == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (recipientId ^ (recipientId >>> 32));
        result = 31 * result + (recipient != null ? recipient.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.recipientId);
        dest.writeParcelable(this.recipient, flags);
        dest.writeLong(this.id);
        dest.writeLong(this.getUserId());
        dest.writeLong(getCreatedAt() != null ? getCreatedAt().getTime() : -1);
        dest.writeLong(getUpdatedAt() != null ? getUpdatedAt().getTime() : -1);
        dest.writeInt(this.getUnreadMessagesCount());
        dest.writeInt(this.getUnreceivedMessagesCount());
        dest.writeInt(this.getMessagesCount());
        dest.writeParcelable(this.getLastMessage(), flags);
    }

    public PrivateConversation() {
        super(Type.PRIVATE.apiValue);
    }

    protected PrivateConversation(Parcel in) {
        super(Type.PRIVATE.apiValue);
        this.recipientId = in.readLong();
        this.recipient = in.readParcelable(User.class.getClassLoader());
        this.id = in.readLong();
        this.userId = in.readLong();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
        this.unreadMessagesCount = in.readInt();
        this.unreceivedMessagesCount = in.readInt();
        this.messagesCount = in.readInt();
        this.lastMessage = in.readParcelable(Message.class.getClassLoader());
    }

    public static final Creator<PrivateConversation> CREATOR = new Creator<PrivateConversation>() {
        @Override
        public PrivateConversation createFromParcel(Parcel source) {
            return new PrivateConversation(source);
        }

        @Override
        public PrivateConversation[] newArray(int size) {
            return new PrivateConversation[size];
        }
    };
}
