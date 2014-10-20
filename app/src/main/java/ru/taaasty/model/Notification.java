package ru.taaasty.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.Comparator;
import java.util.Date;

/**
 * Created by alexey on 22.10.14.
 */
public class Notification implements Parcelable {

    public static final String ENTITY_TYPE_ENTRY = "Entry";

    public static final String ENTITY_TYPE_RELATIONSHIP = "Relationship";

    public static final String ENTITY_TYPE_COMMENT = "Comment";

    public static Comparator<Notification> SORT_BY_CREATED_AT_COMPARATOR = new Comparator<Notification>() {

        @Override
        public int compare(Notification lhs, Notification rhs) {
            return lhs.createdAt.compareTo(rhs.createdAt);
        }
    };

    public long id;


    /**
     * Дата события
     */
    public Date createdAt;

    /**
     * Какой-то user_id. Иногда отличается от sender.id (XXX: какого хера?)
     */
    public long userId;

    // ACHTUNG! Здесь какой-то кастрированный юзер
    public User sender;

    @Nullable
    // ACHTUNG! Relationship кастрированный тоже
    public Relationship senderRelation;

    /**
     * Дата отметки сообщения как прочитанного
     */
    @Nullable
    public Date readAt;

    /**
     * Изображение, если есть
     */
    @Nullable
    public ImageInfo.Image2 image = ImageInfo.Image2.DUMMY;

    /**
     * Действие. "following", "favorite", ещё какие-то.
     */
    public String action;

    /**
     * Комментарий к действию. Напр. "поместил Вашу запись в избранное"
     */
    public String actionText;

    /**
     * Текст комментария или статьи для действия. напр. "Тест фоторедактора"
     */
    public String text;

    /**
     * ID статьи. Либо комментария. Либо, блядь, ещё какой-нибудь хуйни. Тип - в {#entryType}
     */
    public long entityId;

    /**
     * Тип хуйни.
     * {@linkplain #ENTITY_TYPE_ENTRY},
     * {@linkplain #ENTITY_TYPE_RELATIONSHIP},
     * {@linkplain #ENTITY_TYPE_COMMENT},
     * ещё какие-то
     */
    public String entityType;

    /**
     * ID статьи, когда entityType - Сomment
     */
    @Nullable
    public Long parentId;

    @Nullable
    public Long parentType;

    /**
     * URL хуйни.
     */
    public String entityUrl;

    @Override
    public int describeContents() {
        return 0;
    }

    public boolean isMarkedAsRead() {
        return readAt != null;
    }

    public boolean isMeSubscribed() {
        // XXX: здесь по апи возвращается какая-то хрень. Я не понимаю, как этим пользоваться
        if (senderRelation == null) return false;
        return Relationship.isMeSubscribed(senderRelation.getState());
    }

    public boolean isTypeRelationship() {
        return ENTITY_TYPE_RELATIONSHIP.equals(entityType);
    }

    public boolean isTypeComment() {
        return ENTITY_TYPE_COMMENT.equals(entityType);
    }

    public boolean isTypeEntry() {
        return ENTITY_TYPE_ENTRY.equals(entityType);
    }

    public boolean hasImage() {
        return image != null && image != ImageInfo.Image2.DUMMY;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
        dest.writeLong(this.userId);
        dest.writeParcelable(this.sender, 0);
        dest.writeParcelable(this.senderRelation, 0);
        dest.writeLong(readAt != null ? readAt.getTime() : -1);
        dest.writeParcelable(this.image, 0);
        dest.writeString(this.action);
        dest.writeString(this.actionText);
        dest.writeString(this.text);
        dest.writeLong(this.entityId);
        dest.writeString(this.entityType);
        dest.writeValue(this.parentId);
        dest.writeValue(this.parentType);
        dest.writeString(this.entityUrl);
    }

    public Notification() {
    }

    private Notification(Parcel in) {
        this.id = in.readLong();
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        this.userId = in.readLong();
        this.sender = in.readParcelable(User.class.getClassLoader());
        this.senderRelation = in.readParcelable(Relationship.class.getClassLoader());
        long tmpReadAt = in.readLong();
        this.readAt = tmpReadAt == -1 ? null : new Date(tmpReadAt);
        this.image = in.readParcelable(ImageInfo.Image2.class.getClassLoader());
        this.action = in.readString();
        this.actionText = in.readString();
        this.text = in.readString();
        this.entityId = in.readLong();
        this.entityType = in.readString();
        this.parentId = (Long) in.readValue(Long.class.getClassLoader());
        this.parentType = (Long) in.readValue(Long.class.getClassLoader());
        this.entityUrl = in.readString();
    }

    public static final Parcelable.Creator<Notification> CREATOR = new Parcelable.Creator<Notification>() {
        public Notification createFromParcel(Parcel source) {
            return new Notification(source);
        }

        public Notification[] newArray(int size) {
            return new Notification[size];
        }
    };
}