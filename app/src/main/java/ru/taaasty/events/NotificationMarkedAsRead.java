package ru.taaasty.events;

import android.support.v4.util.LongSparseArray;

import java.util.Collection;
import java.util.Date;

import ru.taaasty.rest.model.MarkNotificationsAsReadResponse;

public class NotificationMarkedAsRead {

    /**
     * id => readAt date
     */
    public final LongSparseArray<Date> itemMap;

    public NotificationMarkedAsRead(Collection<MarkNotificationsAsReadResponse> responses) {
        itemMap = new LongSparseArray<>(responses.size());
        for (MarkNotificationsAsReadResponse response: responses) itemMap.append(response.id, response.readAt);
    }

}
