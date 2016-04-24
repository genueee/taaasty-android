package ru.taaasty.rest.model.conversations;

import android.os.Parcelable;

import com.google.gson.JsonElement;

import java.util.Comparator;
import java.util.Date;

import io.gsonfire.TypeSelector;
import ru.taaasty.utils.Objects;

/**
 * Created by alexey on 22.10.14.
 */
public abstract class Conversation implements Parcelable {

    public enum Type {

        /**
         * Чат между двумя пользователями
         */
        PRIVATE("PrivateConversation"),

        /**
         * Чат - приватная группа между несколькими пользователями
         */
        GROUP("GroupConversation"),

        /**
         * Чат - обсуждение записи
         */
        PUBLIC("PublicConversation"),


        /**
         * Все остальные типы чатов. На случай, если вдруг ещё что-то новое придумают.
         */
        OTHER("")

        ;

        final String apiValue;

        Type(String apiType) {
            this.apiValue = apiType;
        }
    };

    long id = -1;

    // Поле в JSON. Храним если вдруг понадобится передавать на сервер
    final String type;

    long userId = -1;

    Date createdAt;

    Date updatedAt;

    int unreadMessagesCount;

    int unreceivedMessagesCount;

    int messagesCount;

    boolean notDisturb;

    boolean isDisabled = false;

    Message lastMessage = Message.DUMMY;

    /**
     * Сортировка по убыванию даты обновления последниего сообщения, либо переписки (более поздние даты - в начале списка)
     */
    public static Comparator<Conversation> SORT_BY_LAST_MESSAGE_UPDATED_AT_DESC_COMPARATOR = new Comparator<Conversation>() {
        @Override
        public int compare(Conversation lhs, Conversation rhs) {
            if (lhs == null && rhs == null) {
                return 0;
            } else if (lhs == null) {
                return -1;
            } else if (rhs == null) {
                return 1;
            } else {
                Date rhsDate = guessDate(rhs);
                Date lhsDate = guessDate(lhs);
                int compareDates = rhsDate.compareTo(lhsDate);
                return compareDates != 0 ? compareDates : Objects.compare(rhs.id, lhs.id);
            }
        }

        private Date guessDate(Conversation conversation) {
            Date date;
            if (conversation.getLastMessage() != null) {
                date = conversation.getLastMessage().readAt != null ? conversation.getLastMessage().readAt : conversation.getLastMessage().createdAt;
            } else {
                date = conversation.getUpdatedAt() != null ? conversation.getUpdatedAt() : conversation.getCreatedAt();
            }
            return date;
        }
    };

    Conversation(String type) {
        this.type = type;
    }

    /**
     * @return Тип чата
     */
    public abstract Type getType();

    /**
     * @return ID пользователя, который можно использовать за пределеами чата.
     * В случае анонимок возвращает ID юзера {@link ru.taaasty.rest.model.User#ANONYMOUS}
     */
    public long getRealUserId() {
        return toRealUserId(getUserId());
    }

    public abstract long toRealUserId(long fakeUserId);

    public static final TypeSelector<Conversation> GSON_TYPE_SELECTOR = new TypeSelector<Conversation>() {
        @Override
        public Class<? extends Conversation> getClassForElement(JsonElement readElement) {
            String type = readElement.getAsJsonObject().get("type").getAsString();
            if (Type.PRIVATE.apiValue.equals(type)) {
                return PrivateConversation.class;
            } else if (Type.GROUP.apiValue.equals(type)) {
                return GroupConversation.class;
            } else if (Type.PUBLIC.apiValue.equals(type)) {
                return PublicConversation.class;
            }
            return null;
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Conversation that = (Conversation) o;

        if (id != that.id) return false;
        if (userId != that.userId) return false;
        if (unreadMessagesCount != that.unreadMessagesCount) return false;
        if (unreceivedMessagesCount != that.unreceivedMessagesCount) return false;
        if (messagesCount != that.messagesCount) return false;
        if (notDisturb != that.notDisturb) return false;
        if (isDisabled != that.isDisabled) return false;
        if (type != null ? !type.equals(that.type) : that.type != null) return false;
        if (createdAt != null ? !createdAt.equals(that.createdAt) : that.createdAt != null)
            return false;
        if (updatedAt != null ? !updatedAt.equals(that.updatedAt) : that.updatedAt != null)
            return false;
        return lastMessage != null ? lastMessage.equals(that.lastMessage) : that.lastMessage == null;

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (int) (userId ^ (userId >>> 32));
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        result = 31 * result + unreadMessagesCount;
        result = 31 * result + unreceivedMessagesCount;
        result = 31 * result + messagesCount;
        result = 31 * result + (notDisturb ? 1 : 0);
        result = 31 * result + (isDisabled ? 1 : 0);
        result = 31 * result + (lastMessage != null ? lastMessage.hashCode() : 0);
        return result;
    }

    public long getId() {
        return id;
    }


    /**
     * Твой ID в этой переписке. <br>
     * По нему можно определить, от тебя это сообщение или нет.
     *  ID используется для пользователей только в пределах данной беседы. Можем совпадать с
     *  реальным ID юзера, но в обсуждениях анонимок используется левое, фейковое значение.
     */
    public long getUserId() {
        return userId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public int getUnreadMessagesCount() {
        return unreadMessagesCount;
    }

    public int getUnreceivedMessagesCount() {
        return unreceivedMessagesCount;
    }

    public int getMessagesCount() {
        return messagesCount;
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public boolean isNotDisturbTurnedOn() {
        return notDisturb;
    }

    public boolean isDisabled() { return isDisabled; }
}
