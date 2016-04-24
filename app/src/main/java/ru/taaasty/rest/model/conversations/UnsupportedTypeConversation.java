package ru.taaasty.rest.model.conversations;

import android.os.Parcel;

import java.util.Date;

public class UnsupportedTypeConversation extends Conversation {

    public UnsupportedTypeConversation() {
        super("unknown");
    }

    @Override
    public Type getType() {
        return Type.OTHER;
    }

    @Override
    public long toRealUserId(long fakeUserId) {
        return fakeUserId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.type);
        dest.writeLong(this.id);
        dest.writeLong(this.userId);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
        dest.writeInt(this.unreadMessagesCount);
        dest.writeInt(this.unreceivedMessagesCount);
        dest.writeInt(this.messagesCount);
        dest.writeByte(notDisturb ? (byte) 1 : (byte) 0);
        dest.writeByte(isDisabled ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.lastMessage, flags);
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
        this.notDisturb = in.readByte() != 0;
        this.isDisabled = in.readByte() != 0;
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
