package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.support.annotation.Nullable;

import java.util.Date;

import ru.taaasty.rest.model.User;

public class UnsupportedTypeConversation extends Conversation {

    public UnsupportedTypeConversation() {
        super("unknown");
    }

    @Nullable
    @Override
    public Type getType() {
        return null;
    }

    @Override
    public long getRealUserId() {
        return User.DUMMY.getId();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeLong(this.id);
        dest.writeString(this.type);
        dest.writeLong(this.getUserId());
        dest.writeLong(getCreatedAt() != null ? getCreatedAt().getTime() : -1);
        dest.writeLong(getUpdatedAt() != null ? getUpdatedAt().getTime() : -1);
        dest.writeInt(this.getUnreadMessagesCount());
        dest.writeInt(this.getUnreceivedMessagesCount());
        dest.writeInt(this.getMessagesCount());
        dest.writeParcelable(this.getLastMessage(), flags);
    }

    protected UnsupportedTypeConversation(Parcel in) {
        super(in.readString());
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

    public static final Creator<UnsupportedTypeConversation> CREATOR = new Creator<UnsupportedTypeConversation>() {
        @Override
        public UnsupportedTypeConversation createFromParcel(Parcel source) {
            return new UnsupportedTypeConversation(source);
        }

        @Override
        public UnsupportedTypeConversation[] newArray(int size) {
            return new UnsupportedTypeConversation[size];
        }
    };
}
