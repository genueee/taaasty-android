package ru.taaasty.adapters.list;

import android.util.Log;

import java.util.Date;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.SortedList;
import ru.taaasty.UserManager;
import ru.taaasty.events.NotificationMarkedAsRead;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.Relationship;

public final class NotificationsListManaged extends SortedList<Notification> {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationsListMgd";

    public NotificationsListManaged(Callback callback) {
        super(Notification.class, callback);
    }

    public void onCreate() {
        EventBus.getDefault().register(this);
    }

    public void onDestroy() {
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(NotificationReceived event) {
        if (DBG) Log.v(TAG, "NotificationReceived " + event);
        addOrUpdate(event.notification);
    }

    /**
     * При отписке или отписке юзера на тлог в списке нотификаций нужно изменить кнопку "подтвердить/уже в друзьях"
     * Через пушер новая нотификация не приходит.
     * @param relationshipChanged
     */
    public void onEventMainThread(RelationshipChanged relationshipChanged) {
        Relationship newRelationship = relationshipChanged.relationship;
        long me = UserManager.getInstance().getCurrentUserId();
        long him;

        if (!newRelationship.isMyRelationToHim(me)) return; // Не интересно
        him = newRelationship.getToId();

        // Меняем relation
        synchronized (this) {
            for (Notification notification : getItems()) {
                if (!notification.isTypeRelationship()) return;
                if (notification.sender.getId() == him) {
                    Notification newNotification = Notification.changeSenderRelation(notification, newRelationship);
                    addOrUpdate(newNotification);
                }
            }
        }
    }

    public void onEventMainThread(NotificationMarkedAsRead event) {
        int notificationsSize = size();
        beginBatchedUpdates();
        try {
            for (int i = 0; i < notificationsSize; ++i) {
                Notification notification = get(i);
                if (!notification.isMarkedAsRead()) {
                    Date readDate = event.itemMap.get(notification.id);
                    if (readDate != null) {
                        Notification newNotification = Notification.markAsRead(notification, readDate);
                        addOrUpdate(newNotification);
                    }
                }
            }
        } finally {
            endBatchedUpdates();
        }
    }

    public static abstract class Callback extends android.support.v7.util.SortedList.Callback<Notification> {

        @Override
        public int compare(Notification o1, Notification o2) {
            return Notification.SORT_BY_CREATED_AT_DESC_COMPARATOR.compare(o1, o2);
        }

        @Override
        public boolean areContentsTheSame(Notification oldItem, Notification newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areItemsTheSame(Notification item1, Notification item2) {
            return item1.id == item2.id;
        }
    }
}
