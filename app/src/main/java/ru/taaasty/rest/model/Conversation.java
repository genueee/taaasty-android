package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import ru.taaasty.Session;
import ru.taaasty.utils.Objects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by alexey on 22.10.14.
 */
public class Conversation implements Parcelable {

    public static final String TYPE_CHAT = "PrivateConversation";
    public static final String TYPE_GROUP = "GroupConversation";
    public static final String TYPE_PUBLIC_GROUP = "PublicConversation";

    public static final String MESSAGE_TYPE_SYSTEM = "SystemMessage";

    public long id = -1;

    public long userId = -1;

    public Date createdAt;

    public Date updatedAt;

    public String type;

    public String topic;

    public long recipientId;

    public User recipient = User.DUMMY;

    public User admin;

    public ArrayList<User> users;

    public long[] users_left;

    public Entry entry;

    public int unreadMessagesCount;

    public int unreceivedMessagesCount;

    public int messagesCount;

    public Avatar avatar;

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

    public boolean isGroup() {
        return isPrivateGroup() || isPublicGroup();
    }

    public boolean isPublicGroup() {
        return type.equals(TYPE_PUBLIC_GROUP);
    }

    public boolean isPrivateGroup() {
        return type.equals(TYPE_GROUP);
    }

    public ArrayList<User> getActualUsers() {
        ArrayList<User> actualUsers = new ArrayList<>(users);
        if (users_left != null) {
            for (long userId : users_left) {
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

    public String getAvatarUrl() {
        if (isPrivateGroup() && avatar != null) {
            return avatar.url;
        }
        if (isPublicGroup() && entry.getImageUrl() != null) {
            return entry.getImageUrl();
        }
        return null;
    }

    public User getAvatarUser() {
        if (!isGroup()) {
            return recipient;
        }
        if (isPublicGroup()) {
            return entry.getAuthor();
        }
        return null;
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

        public User author;

        public String type;

        @Nullable
        public Conversation conversation;

        public Message() {
        }

        public boolean isMarkedAsRead() {
            return readAt != null;
        }

        public boolean isFromMe() {
            return Session.getInstance().isMe(userId);
        }

        public boolean isSystemMessage() {
           return MESSAGE_TYPE_SYSTEM.equals(type);
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
            dest.writeParcelable(this.author, 0);
            dest.writeString(this.type);
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
            this.author = in.readParcelable(User.class.getClassLoader());
            this.type = in.readString();
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

    public static class Avatar implements Parcelable {

        public String url;
        public String path;
        public Geometry geometry;
        public String title;
        public String source;

        public static class Geometry {
            int width;
            int height;
        }

        protected Avatar(Parcel in) {
            url = in.readString();
            path = in.readString();
            Geometry geometry = new Geometry();
            geometry.width = in.readInt();
            geometry.height = in.readInt();
            this.geometry = geometry;
            title = in.readString();
            source = in.readString();
        }

        public static final Creator<Avatar> CREATOR = new Creator<Avatar>() {
            @Override
            public Avatar createFromParcel(Parcel in) {
                return new Avatar(in);
            }

            @Override
            public Avatar[] newArray(int size) {
                return new Avatar[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(url);
            dest.writeString(path);
            dest.writeInt(geometry.width);
            dest.writeInt(geometry.height);
            dest.writeString(title);
            dest.writeSerializable(source);
        }
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
        dest.writeLongArray(users_left);
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
        this.users_left = in.createLongArray();
        this.entry = in.readParcelable(Entry.class.getClassLoader());
        this.recipient = in.readParcelable(User.class.getClassLoader());
        this.unreadMessagesCount = in.readInt();
        this.unreceivedMessagesCount = in.readInt();
        this.messagesCount = in.readInt();
        this.lastMessage = in.readParcelable(Message.class.getClassLoader());
        this.avatar = in.readParcelable(Avatar.class.getClassLoader());
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
