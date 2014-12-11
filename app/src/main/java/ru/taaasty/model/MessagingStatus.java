package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alexey on 24.11.14.
 */
public class MessagingStatus {

    @SerializedName("activeConversationsCount")
    public int activeConversationsCount;

    @SerializedName("unreadConversationsCount")
    public int unreadConversationsCount;

    @SerializedName("unreadNotificationsCount")
    public int unreadNotificationsCount;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MessagingStatus that = (MessagingStatus) o;

        if (activeConversationsCount != that.activeConversationsCount) return false;
        if (unreadConversationsCount != that.unreadConversationsCount) return false;
        if (unreadNotificationsCount != that.unreadNotificationsCount) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = activeConversationsCount;
        result = 31 * result + unreadConversationsCount;
        result = 31 * result + unreadNotificationsCount;
        return result;
    }

    @Override
    public String toString() {
        return "MessagingStatus{" +
                "activeConversationsCount=" + activeConversationsCount +
                ", unreadConversationsCount=" + unreadConversationsCount +
                ", unreadNotificationsCount=" + unreadNotificationsCount +
                '}';
    }
}
