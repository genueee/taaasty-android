package ru.taaasty.rest.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.List;

/**
 * Created by alexey on 22.10.14.
 */
public class PusherReadyResponse implements Parcelable {

    public List<Conversation> conversations = Collections.emptyList();

    public List<Notification> notifications = Collections.emptyList();


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(conversations);
        dest.writeTypedList(this.notifications);
    }

    public PusherReadyResponse() {
    }

    private PusherReadyResponse(Parcel in) {
        this.conversations = in.createTypedArrayList(Conversation.CREATOR);
        this.notifications = in.createTypedArrayList(Notification.CREATOR);
    }

    public static final Parcelable.Creator<PusherReadyResponse> CREATOR = new Parcelable.Creator<PusherReadyResponse>() {
        public PusherReadyResponse createFromParcel(Parcel source) {
            return new PusherReadyResponse(source);
        }

        public PusherReadyResponse[] newArray(int size) {
            return new PusherReadyResponse[size];
        }
    };
}
