package ru.taaasty.events;

import java.util.Collection;
import java.util.Date;

import ru.taaasty.model.MarkNotificationsAsReadResponse;

/**
 * Created by alexey on 16.05.15.
 */
public class NotificationMarkedAsRead {

    public final long id[];

    public final Date readAt[];

    public NotificationMarkedAsRead(long id, Date readAt) {
        this.id = new long[]{id};
        this.readAt = new Date[]{readAt};
    }

    public NotificationMarkedAsRead(Collection<MarkNotificationsAsReadResponse> responses) {
        int size = responses.size();
        this.id = new long[size];
        this.readAt = new Date[size];

        int i = 0;
        for (MarkNotificationsAsReadResponse response: responses) {
            id[i] = response.id;
            readAt[i] = response.readAt;
            i += 1;
        }
    }

}
