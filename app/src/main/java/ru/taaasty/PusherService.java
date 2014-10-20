package ru.taaasty;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;

import com.pusher.client.AuthorizationFailureException;
import com.pusher.client.Authorizer;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.apache.commons.io.IOUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.event.EventBus;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.NotificationsCountChanged;
import ru.taaasty.model.Notification;
import ru.taaasty.model.PusherReadyResponse;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class PusherService extends Service implements PrivateChannelEventListener {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "PusherService";
    public static final String EVENT_PUSH_NOTIFICATION = "push_notification";

    @Retention(RetentionPolicy.CLASS)
    @IntDef({UPDATE_NOTIFICATIONS_STATUS_NONE, UPDATE_NOTIFICATIONS_STATUS_LOADING, UPDATE_NOTIFICATIONS_STATUS_READY, UPDATE_NOTIFICATIONS_STATUS_FAILURE})
    public @interface UpdateNotificationsStatus {}

    /**
     * Уведомления ещё даже и не на чали загружаться. Не должен использоваться
     */
    public static final int UPDATE_NOTIFICATIONS_STATUS_NONE = 0;

    /**
     * Уведомления в процессе загрузки/обновления
     */
    public static final int UPDATE_NOTIFICATIONS_STATUS_LOADING = 1;

    /**
     * Уведомления загружены
     */
    public static final int  UPDATE_NOTIFICATIONS_STATUS_READY = 2;

    /**
     * Список уведомлений не загружен из-за ошибки
     */
    public static final int  UPDATE_NOTIFICATIONS_STATUS_FAILURE = 3;


    private static final String ACTION_START = "ru.taaasty.PusherService.action.START";
    private static final String ACTION_STOP = "ru.taaasty.PusherService.action.STOP";
    private static final String ACTION_MARK_AS_READ = "ru.taaasty.PusherService.action.MARK_AS_READ";

    private static final String EXTRA_NOTIFICATION_ID = "ru.taaasty.PusherService.action.EXTRA_NOTIFICATION_ID";

    private final IBinder mBinder = new LocalBinder();

    private ApiMessenger mApiMessenger;
    private Pusher mPusher;

    @UpdateNotificationsStatus
    private volatile int mUpdateNotificationsStatus;

    private volatile boolean mNotificationsListInitialized;

    private volatile LinkedList<Notification> mCurrentNotifications;

    /**
     * Сообщение об ошибки при {#mUpdateNotificationsStatus} = {#UPDATE_NOTIFICATIONS_STATUS_FAILURE}.
     * Пригодное, для показа юзеру на экране
     */
    private String mUpdateNotificationsError = "";

    public static final class NotificationsStatus {
        @UpdateNotificationsStatus
        public final int code;

        public final String errorMessage;

        NotificationsStatus(int status, String errorMessage) {
            this.code = status;
            this.errorMessage = errorMessage;
        }
    }

    public static void startPusher(Context context) {
        Intent intent = new Intent(context, PusherService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stopPusher(Context context) {
        Intent intent = new Intent(context, PusherService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    public static void markNotificationAsRead(Context context, long notificationId) {
        Intent intent = new Intent(context, PusherService.class);
        intent.setAction(ACTION_MARK_AS_READ);
        intent.putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        context.startService(intent);
    }

    public PusherService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
        mUpdateNotificationsStatus = UPDATE_NOTIFICATIONS_STATUS_NONE;
        mCurrentNotifications = new LinkedList<>();
        mNotificationsListInitialized = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                if (DBG) Log.v(TAG, "ACTION_START");
                pusherConnect();
            } else if (ACTION_STOP.equals(action)) {
                if (DBG) Log.v(TAG, "ACTION_STOP");
                stopSelf();
            } else if (ACTION_MARK_AS_READ.equals(action)) {
                handleMarkAsRead(intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1));
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        destroyPusher();
        mApiMessenger = null;
    }

    @Override
    public void onEvent(String channelName, String eventName, String data) {
        if (getMessagingChannelName().equals(channelName) && EVENT_PUSH_NOTIFICATION.equals(eventName)) {
            Notification n = NetworkUtils.getInstance().getGson().fromJson(data, Notification.class);
            addNotification(n);
        } else {
            if (DBG) Log.v(TAG, "onEvent() channel: " + channelName + " event: " + eventName + " data: " + data);
        }
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
        if (DBG) Log.v(TAG, "onAuthenticationFailure() msg: " + message, e);
        setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_FAILURE, message, e);
    }

    @Override
    public synchronized void onSubscriptionSucceeded(String channelName) {
        if (DBG) Log.v(TAG, "onSubscriptionSucceeded() channel: " + channelName);
        try {
            PusherReadyResponse response = mApiMessenger.authReady(mPusher.getConnection().getSocketId());
            mCurrentNotifications.clear();
            mCurrentNotifications.addAll(response.notifications);
            setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_READY);
            broadcastNotificationsCountChanged();
        } catch (Exception e) {
            Log.e(TAG, "authReady failed", e);
            //Попробуем подключиться как-нибудь в другой раз
            destroyPusher();
            setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_FAILURE, getString(R.string.error_loading_notifications), e);
        }
    }

    /**
     * @return Кол-во непрочитанных уведомлений.
     * В процессе обновления или при ошибке - последнее известное значение.
     * -1, если они ещё ни разу не были загружены.
     */
    public synchronized int getMessagesCount() {
       return  mNotificationsListInitialized ? mCurrentNotifications.size() : -1;
    }

    public synchronized boolean isNotificationListEmpty() {
        return mCurrentNotifications.isEmpty();
    }

    public synchronized NotificationsStatus getNotificationsStatus() {
        return new NotificationsStatus(mUpdateNotificationsStatus, mUpdateNotificationsError);
    }

    /**
     * @return Кол-во непрочитанных уведомлений.
     * В процессе обновления или при ошибке - последнее известное значение.
     * -1, если они ещё ни разу не были загружены.
     */
    public synchronized int getUnreadMessagesCount() {
        if (!mNotificationsListInitialized) return -1;
        int cnt = 0;
        for (Notification notification: mCurrentNotifications) {
            if (!notification.isMarkedAsRead()) cnt += 1;
        }
        return cnt;
    }

    /**
     * @return Копия списка уведомлений
     */
    public synchronized List<Notification> getNotifications() {
        // XXX считаем, что Notification - immutable, копируем ссылки
        return new ArrayList<>(mCurrentNotifications);
    }

    private void createPusher() {
        destroyPusher();
        PusherOptions options = new PusherOptions()
                .setEncrypted(false)
                .setAuthorizer(mAuthorizer)
                ;

        mPusher = new Pusher(BuildConfig.PUSHER_KEY, options);
        mPusher.subscribePrivate(getMessagingChannelName(), this, EVENT_PUSH_NOTIFICATION);
    }

    private void pusherConnect() {
        if (mPusher != null && mPusher.getConnection().getState() != ConnectionState.DISCONNECTED) {
            return;
        }
        setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_LOADING);
        if (mPusher == null) createPusher();
        mPusher.connect(mConnectionEventListener);
    }

    private void destroyPusher() {
        if (mPusher == null) return;
        mPusher.disconnect();
        mPusher = null;
    }

    private String getMessagingChannelName() {
        return "private-" + UserManager.getInstance().getCurrentUserId() + "-messaging";
    }

    private void setUpdateNotificationsStatus(@UpdateNotificationsStatus int status) {
        setUpdateNotificationsStatus(status, null, null);
    }

    private synchronized void addNotification(Notification notification) {
        mCurrentNotifications.addFirst(notification);
        EventBus.getDefault().post(new NotificationReceived(notification));
        broadcastNotificationsCountChanged();
    }

    private synchronized void setUpdateNotificationsStatus(@UpdateNotificationsStatus int status, String error, Exception e) {
        if (DBG) Log.v(TAG, "setUpdateNotificationsStatus status: " + status + " error: "  + error);
        mUpdateNotificationsStatus = status;
        mUpdateNotificationsError = error;
        if (status == UPDATE_NOTIFICATIONS_STATUS_READY) mNotificationsListInitialized = true;

        ru.taaasty.events.NotificationsStatus event = new ru.taaasty.events.NotificationsStatus(new NotificationsStatus(status, error));
        EventBus.getDefault().post(event);
    }

    private synchronized void broadcastNotificationsCountChanged() {
        EventBus.getDefault().post(new NotificationsCountChanged(mCurrentNotifications));
    }

    public class LocalBinder extends Binder {
        public PusherService getService() {
            return PusherService.this;
        }
    }

    private final Authorizer mAuthorizer = new Authorizer() {
        @Override
        public String authorize(String channelName, String socketId) throws AuthorizationFailureException {
            try {
                Response response = mApiMessenger.authPusher(channelName, socketId);
                TypedInput ti = response.getBody();
                if (ti == null) throw  new NullPointerException("response is null");
                return IOUtils.toString(ti.in());
            } catch (Exception e) {
                throw new AuthorizationFailureException(e);
            }
        }
    };

    private void handleMarkAsRead(long notificationId) {
        if (mPusher == null || mPusher.getConnection().getState() != ConnectionState.CONNECTED) {
            if (DBG) Log.v(TAG, "handleMarkAsRead pusher disconnected");
            return;
        }
        Observable<Notification> observableNotification = mApiMessenger.markNotificationAsRead(
                mPusher.getConnection().getSocketId(), notificationId);
        observableNotification
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mMarkAsReadObserver);
    }

    private final Observer<Notification> mMarkAsReadObserver = new Observer<Notification>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            // XXX
        }

        @Override
        public void onNext(Notification notification) {

            synchronized (this) {
                boolean found = false;
                ListIterator<Notification> i = mCurrentNotifications.listIterator();
                while (i.hasNext()) {
                    Notification old = i.next();
                    if (old.id == notification.id) {
                        i.set(notification);
                        found = true;
                        break;
                    }
                }
                if (!found) mCurrentNotifications.addFirst(notification);
                EventBus.getDefault().post(new NotificationReceived(notification));
                broadcastNotificationsCountChanged();
            }

        }
    };

    private final ConnectionEventListener mConnectionEventListener = new ConnectionEventListener() {
        @Override
        public void onConnectionStateChange(ConnectionStateChange change) {
            if (DBG) Log.v(TAG, "onConnectionStateChange() change: " + change.getPreviousState() + " -> " + change.getCurrentState());
        }

        @Override
        public void onError(String message, String code, Exception e) {
            if (DBG) Log.v(TAG, "onError() message: " + message + " code: " + code, e);
        }
    };
}
