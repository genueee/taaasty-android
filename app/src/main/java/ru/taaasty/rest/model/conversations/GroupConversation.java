package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import ru.taaasty.rest.model.User;

/**
 * Created by alexey on 16.04.16.
 */
public class GroupConversation extends Conversation implements Parcelable, HasManyUsers {

    /**
     * Тема.
     */
    @Nullable
    String topic;

    /**
     * Админ чата.
     */
    @Nullable
    User admin;

    /**
     * Список пользователей
     */
    @Nullable
    ArrayList<User> users;

    @Nullable
    long[] usersLeft;

    //long[] usersDeleted;

    /**
     * Пконка чата. Только если она установлена.
     * null в остальных случаях
     */
    @Nullable
    GroupPicture avatar;

    public User getGroupAdmin() {
        return admin;
    }

    @Nullable
    public String getTopic() {
        return topic;
    }

    @Nullable
    @Override
    public Type getType() {
        return Type.GROUP;
    }

    @Override
    public long getRealUserId() {
        return getUserId();
    }


    public List<User> getUsers() {
        return users;
    }

    public long[] getUsersLeft() {
        return usersLeft;
    }

    public GroupPicture getAvatar() {
        return avatar;
    }

    public GroupConversation() {
        super(Type.GROUP.apiValue);
    }

    public boolean isMeAdmin() {
        return admin != null && admin.getId() == getId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GroupConversation that = (GroupConversation) o;

        if (topic != null ? !topic.equals(that.topic) : that.topic != null) return false;
        if (admin != null ? !admin.equals(that.admin) : that.admin != null) return false;
        if (users != null ? !users.equals(that.users) : that.users != null) return false;
        if (!Arrays.equals(usersLeft, that.usersLeft)) return false;
        return avatar != null ? avatar.equals(that.avatar) : that.avatar == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (topic != null ? topic.hashCode() : 0);
        result = 31 * result + (admin != null ? admin.hashCode() : 0);
        result = 31 * result + (users != null ? users.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(usersLeft);
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        return result;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.topic);
        dest.writeParcelable(this.admin, flags);
        dest.writeTypedList(users);
        dest.writeLongArray(this.usersLeft);
        dest.writeParcelable(this.avatar, flags);
        dest.writeLong(this.id);
        dest.writeLong(this.getUserId());
        dest.writeLong(getCreatedAt() != null ? getCreatedAt().getTime() : -1);
        dest.writeLong(getUpdatedAt() != null ? getUpdatedAt().getTime() : -1);
        dest.writeInt(this.getUnreadMessagesCount());
        dest.writeInt(this.getUnreceivedMessagesCount());
        dest.writeInt(this.getMessagesCount());
        dest.writeParcelable(this.getLastMessage(), flags);
    }

    protected GroupConversation(Parcel in) {
        this();
        this.topic = in.readString();
        this.admin = in.readParcelable(User.class.getClassLoader());
        this.users = in.createTypedArrayList(User.CREATOR);
        this.usersLeft = in.createLongArray();
        this.avatar = in.readParcelable(GroupPicture.class.getClassLoader());
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

    public static final Creator<GroupConversation> CREATOR = new Creator<GroupConversation>() {
        @Override
        public GroupConversation createFromParcel(Parcel source) {
            return new GroupConversation(source);
        }

        @Override
        public GroupConversation[] newArray(int size) {
            return new GroupConversation[size];
        }
    };
}
