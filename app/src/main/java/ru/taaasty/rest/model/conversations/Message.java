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

    public Attachment attachments[];

    public Message() {
    }

    public boolean isMarkedAsRead() {
        return readAt != null;
    }

    public boolean isFromMe(Conversation inConversation) {
        return inConversation.userId == this.userId;
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
        if (conversation != null ? !conversation.equals(message.conversation) : message.conversation != null)
            return false;
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
        result = 31 * result + (conversation != null ? conversation.hashCode() : 0);
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
        dest.writeParcelable(this.conversation, flags);
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
        this.conversation = in.readParcelable(Conversation.class.getClassLoader());
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
}
