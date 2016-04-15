package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.User;
import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 22.10.14.
 */
public class Conversation implements Parcelable {

    /**
     * Чат между двумя пользователями
     */
    public static final String TYPE_PRIVATE_CONVERSATION = "PrivateConversation";

    /**
     * Чат - групповой, между несколькими пользователями, приватный
     */
    public static final String TYPE_GROUP_CONVERSATION = "GroupConversation";

    /**
     * Обсуждения записи в форме чата.
     */
    public static final String TYPE_PUBLIC_CONVERSATION = "PublicConversation";

    public long id = -1;

    public String type;

    /**
     * В приватных чатах - всегда ты (текущий заголиненный пользователь)
     * в публичных - ID, по которому можно определить, от тебя это сообщение или нет.
     * В большинстве случаев это ID залогигенного пользователя, но в обсуждениях этот ID - фейковый
     */
    public long userId = -1;

    public Date createdAt;

    public Date updatedAt;

    /**
     * Тема. Только для {@linkplain #TYPE_GROUP_CONVERSATION} чатов. null - для остальных
     */
    @Nullable
    public String topic;

    /**
     * Собеседник. Только для {@linkplain #TYPE_PRIVATE_CONVERSATION} чатов. null - для остальных
     */
    public long recipientId;

    public User recipient = User.DUMMY;

    /**
     * Админ чата. Только для групповых чатов. Часто не приходит
     */
    @Nullable
    private User admin;

    /**
     * Список пользователей. Только для групповых чатов.
     */
    @Nullable
    public ArrayList<User> users;

    @Nullable
    public long[] usersLeft;

    /**
     * Обсуждаемая запись. Только для типа {@link #TYPE_PUBLIC_CONVERSATION}
     */
    @Nullable
    public Entry entry;


    public int unreadMessagesCount;

    public int unreceivedMessagesCount;

    public int messagesCount;

    /**
     * Аватарка чата, только для типа {@link #TYPE_GROUP_CONVERSATION} и только если она установлена.
     * null в остальных случаях
     */
    @Nullable
    public ConversationAvatar avatar;

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

    /**
     * @return Чат - групповой (с несколькими пользователями, а не с одним)
     */
    public boolean isGroup() {
        return isPrivateGroup() || isPublicGroup();
    }

    /**
     * Чат - обсуждение записи
     */
    public boolean isPublicGroup() {
        return type.equals(TYPE_PUBLIC_CONVERSATION);
    }

    /**
     * @return Чат - приватный, между нескольими пользователями
     */
    public boolean isPrivateGroup() {
        return type.equals(TYPE_GROUP_CONVERSATION);
    }

    public ArrayList<User> getActualUsers() {
        ArrayList<User> actualUsers = new ArrayList<>(users);
        if (usersLeft != null) {
            for (long userId : usersLeft) {
                User userLeft;
                if ((userLeft = findUserById(userId)) != null) {
                    actualUsers.remove(userLeft);
                }
            }
        }
        return actualUsers;
    }

    public User getGroupAdmin() {
        return admin;
    }

    public String getTitle() {
        if (isPrivateGroup()) {
            return topic;
        } else if (isPublicGroup()) {
            return entry.getTitle();
        } else {
            return recipient.getNameWithPrefix();
        }
    }

    public User findUserById(long userId) {
        if (users == null) {
            return null;
        } else {
            for (User user: users) {
                if (user.getId() == userId) {
                    return user;
                }
            }
        }
        return null;
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
        dest.writeString(this.type);
        dest.writeString(this.topic);
        dest.writeLong(this.recipientId);
        dest.writeParcelable(this.admin, 0);
        dest.writeTypedList(users);
        dest.writeLongArray(usersLeft);
        dest.writeParcelable(entry, 0);
        dest.writeParcelable(this.recipient, 0);
        dest.writeInt(this.unreadMessagesCount);
        dest.writeInt(this.unreceivedMessagesCount);
        dest.writeInt(this.messagesCount);
        dest.writeParcelable(this.lastMessage, 0);
        dest.writeParcelable(this.avatar, 0);
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
        this.type = in.readString();
        this.topic = in.readString();
        this.recipientId = in.readLong();
        this.admin = in.readParcelable(User.class.getClassLoader());
        this.users = new ArrayList<>();
        in.readTypedList(users, User.CREATOR);
        this.usersLeft = in.createLongArray();
        this.entry = in.readParcelable(Entry.class.getClassLoader());
        this.recipient = in.readParcelable(User.class.getClassLoader());
        this.unreadMessagesCount = in.readInt();
        this.unreceivedMessagesCount = in.readInt();
        this.messagesCount = in.readInt();
        this.lastMessage = in.readParcelable(Message.class.getClassLoader());
        this.avatar = in.readParcelable(ConversationAvatar.class.getClassLoader());
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
