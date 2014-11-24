package ru.taaasty.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by alexey on 24.11.14.
 */
public class MessagingStatus {

    @SerializedName("activeConversationsCount")
    public long activeConversationsCount;

    @SerializedName("unreadConversationsCount")
    public long unreadConversationsCount;

    @SerializedName("unreadConversationsCount")
    public long unreadNotificationsCount;

}
