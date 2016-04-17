package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.ImageInfo;
import ru.taaasty.rest.model.User;

/**
 * Чат - обсуждение нескольких записей
 */
public class PublicConversation extends Conversation implements Parcelable, HasManyUsers {

    /**
     * Обсуждаемая запись.
     */
    Entry entry;

    /**
     * Список пользователей
     */
    @Nullable
    ArrayList<User> users;

    @Nullable
    long[] usersLeft;

    //long[] usersDeleted;


    /**
     * Чат - обсуждение анонимных записей. Все ID - фейковые.
     */
    boolean isAnonymous;

    public Entry getEntry() {
        return entry;
    }

    @Nullable
    @Override
    public Type getType() {
        return Type.PUBLIC;
    }

    @Override
    public long getRealUserId() {
        return !isAnonymous ? getUserId() : User.ANONYMOUS.getId();
    }

    public String getEntryTitle() {
        return entry == null ? null : entry.getTitle();
    }

    public List<User> getUsers() {
        return users;
    }

    public long[] getUsersLeft() {
        return usersLeft;
    }

    public boolean isAnonymous() {
        return this.isAnonymous;
    }

    @Nullable
    public ImageInfo.Image2 getPreviewImage() {
        return entry == null ? null : entry.getPreviewImage();
    }

    public PublicConversation() {
        super(Type.PUBLIC.apiValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PublicConversation that = (PublicConversation) o;

        if (isAnonymous != that.isAnonymous) return false;
        if (entry != null ? !entry.equals(that.entry) : that.entry != null) return false;
        if (users != null ? !users.equals(that.users) : that.users != null) return false;
        return Arrays.equals(usersLeft, that.usersLeft);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (entry != null ? entry.hashCode() : 0);
        result = 31 * result + (users != null ? users.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(usersLeft);
        result = 31 * result + (isAnonymous ? 1 : 0);
        return result;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(this.userId);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(updatedAt != null ? updatedAt.getTime() : -1);
        dest.writeInt(this.unreadMessagesCount);
        dest.writeInt(this.unreceivedMessagesCount);
        dest.writeInt(this.messagesCount);
        dest.writeParcelable(this.lastMessage, flags);
        dest.writeByte(isAnonymous ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.entry, flags);
        dest.writeTypedList(users);
        dest.writeLongArray(this.usersLeft);
    }

    protected PublicConversation(Parcel in) {
        this();
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
        this.isAnonymous = in.readByte() != 0;
        this.entry = in.readParcelable(Entry.class.getClassLoader());
        this.users = in.createTypedArrayList(User.CREATOR);
        this.usersLeft = in.createLongArray();
    }

    public static final Creator<PublicConversation> CREATOR = new Creator<PublicConversation>() {
        @Override
        public PublicConversation createFromParcel(Parcel source) {
            return new PublicConversation(source);
        }

        @Override
        public PublicConversation[] newArray(int size) {
            return new PublicConversation[size];
        }
    };
}
