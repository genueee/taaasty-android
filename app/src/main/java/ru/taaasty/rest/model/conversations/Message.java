package ru.taaasty.rest.model.conversations;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import ru.taaasty.rest.model.User;
import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 14.04.16.
 */
public class Message implements Parcelable {

    private static final String MESSAGE_TYPE_SYSTEM = "SystemMessage";

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

    /**
     * ID отправителя. Может быть фейковым в случае анонимного диалога.
     */
    long userId;

    public long conversationId;

    /**
     * ID получателя. Может быть фейковым в случае анонимного диалога.
     */
    long recipientId;

    @Nullable
    public String uuid;

    public Date createdAt;

    @Nullable
    public Date readAt;

    /**
     * Контеткт в HTML
     */
    public String contentHtml;

    /**
     * Отправитель. МОжет быть с фейковым ID в случае обсуждения анонимной записи
     */
    public User author;

    public String type;

    public Attachment attachments[];

    public Message() {
    }

    private Message(Builder builder) {
        attachments = builder.attachments;
        id = builder.id;
        userId = builder.userId;
        conversationId = builder.conversationId;
        recipientId = builder.recipientId;
        uuid = builder.uuid;
        createdAt = builder.createdAt;
        readAt = builder.readAt;
        contentHtml = builder.contentHtml;
        author = builder.author;
        type = builder.type;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(Message copy) {
        Builder builder = new Builder();
        builder.attachments = copy.attachments;
        builder.id = copy.id;
        builder.userId = copy.userId;
        builder.conversationId = copy.conversationId;
        builder.recipientId = copy.recipientId;
        builder.uuid = copy.uuid;
        builder.createdAt = copy.createdAt;
        builder.readAt = copy.readAt;
        builder.contentHtml = copy.contentHtml;
        builder.author = copy.author;
        builder.type = copy.type;
        return builder;
    }

    public boolean isMarkedAsRead() {
        return readAt != null;
    }

    /**
     *  Сообщение - исходящее, от меня
     */
    public boolean isFromMe(Conversation inConversation) {
        return inConversation.getUserId() == this.userId;
    }

    public boolean isSystemMessage() {
        return MESSAGE_TYPE_SYSTEM.equals(type);
    }


    public List<Attachment> getImageAttachments() {
        if (attachments == null || attachments.length == 0) return Collections.emptyList();
        List<Attachment> attachments = new ArrayList<>(this.attachments.length);
        for (Attachment a: this.attachments) if (a.isImage()) attachments.add(a);
        return attachments;
    }

    @Nullable
    public Attachment getFirstImageAttachment() {
        if (attachments == null) return null;
        for (Attachment a: attachments) if (a.isImage()) return a;
        return null;
    }

    /**
     * @return ID отправителя. Может быть фейковым (в анонимных паблик чатах)
     */
    public long getUserId() {
        return userId;
    }

    /**
     * @return ID получателя. Может быть фейковым (в анонимных паблик чатах)
     */
    public long getRecipientId() {
        return recipientId;
    }

    public long getRealUserId(Conversation inConversation) {
        return inConversation.toRealUserId(userId);
    }

    public long getRecipientUserId(Conversation inConversation){
        if (recipientId == 0) return 0;
        return inConversation.toRealUserId(recipientId);
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
        if (readAt != null ? !readAt.equals(message.readAt) : message.readAt != null) return false;
        if (contentHtml != null ? !contentHtml.equals(message.contentHtml) : message.contentHtml != null)
            return false;
        if (author != null ? !author.equals(message.author) : message.author != null) return false;
        if (type != null ? !type.equals(message.type) : message.type != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(attachments, message.attachments);

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
        result = 31 * result + (author != null ? author.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(attachments);
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
        dest.writeParcelable(this.author, flags);
        dest.writeString(this.type);
        dest.writeTypedArray(this.attachments, flags);
    }

    protected Message(Parcel in) {
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
        this.author = in.readParcelable(User.class.getClassLoader());
        this.type = in.readString();
        this.attachments = in.createTypedArray(Attachment.CREATOR);
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public static final class Builder {
        private Attachment[] attachments;
        private long id;
        private long userId;
        private long conversationId;
        private long recipientId;
        private String uuid;
        private Date createdAt;
        private Date readAt;
        private String contentHtml;
        private User author;
        private String type;

        private Builder() {
        }

        public Builder attachments(Attachment[] val) {
            attachments = val;
            return this;
        }

        public Builder id(long val) {
            id = val;
            return this;
        }

        public Builder userId(long val) {
            userId = val;
            return this;
        }

        public Builder conversationId(long val) {
            conversationId = val;
            return this;
        }

        public Builder recipientId(long val) {
            recipientId = val;
            return this;
        }

        public Builder uuid(String val) {
            uuid = val;
            return this;
        }

        public Builder createdAt(Date val) {
            createdAt = val;
            return this;
        }

        public Builder readAt(Date val) {
            readAt = val;
            return this;
        }

        public Builder contentHtml(String val) {
            contentHtml = val;
            return this;
        }

        public Builder author(User val) {
            author = val;
            return this;
        }

        public Builder type(String val) {
            type = val;
            return this;
        }

        public Message build() {
            return new Message(this);
        }
    }
}
