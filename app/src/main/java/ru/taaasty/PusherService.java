package ru.taaasty;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
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
import ru.taaasty.events.ConversationChanged;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.events.MessagingStatusReceived;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.events.UpdateMessagesReceived;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.MessagingStatus;
import ru.taaasty.model.Notification;
import ru.taaasty.model.PusherReadyResponse;
import ru.taaasty.model.Relationship;
import ru.taaasty.model.UpdateMessages;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.Objects;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public class PusherService extends Service implements PrivateChannelEventListener {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "PusherService";
    public static final int PUSHER_RECONNECT_TIMEOUT = 10000;

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


    /**
     *  Кол-во активных и непрочитанных переписок и непрочитанных уведомлений
     *  Тип: {@linkplain ru.taaasty.model.MessagingStatus}
     */
    public static final String EVENT_STATUS = "status";


    /**
     * Событие - активные переписки
     * Тип: {@linkplain ru.taaasty.model.Conversations}
     */
    public static final String EVENT_ACTIVE_CONVERSATIONS = "active_conversations";

    /**
     * Событие - обновление переписки
     * Тип: {@linkplain ru.taaasty.model.Notification}
     */
    public static final String EVENT_UPDATE_CONVERSATION = "update_conversation";

    /**
     * Новое сообщение
     * Тип: {@linkplain ru.taaasty.model.Conversation.Message}
     */
    public static final String EVENT_PUSH_MESSAGE = "push_message";

    /**
     * Обновление статуса сообщений
     * Тип: {@linkplain ru.taaasty.model.UpdateMessages}
     */
    public static final String EVENT_UPDATE_MESSAGES = "update_messages";

    /**
     * push notification - пришло уведомление.
     * Тип: {@linkplain ru.taaasty.model.Notification}
     */
    public static final String EVENT_PUSH_NOTIFICATION = "push_notification";


    /**
     * Уведомления отмечеты как прочитвнные.
     * Тип: notifications: [{ id: notification.id, read_at: notification.read_at }]
     */
    public static final String EVENT_UPDATE_NOTIFICATIONS = "update_notifications";


    private static final String ACTION_START = "ru.taaasty.PusherService.action.START";
    private static final String ACTION_STOP = "ru.taaasty.PusherService.action.STOP";
    private static final String ACTION_SET_STATUS_BAR_NOTIFICATIONS = "ru.taaasty.PusherService.action.ACTION_SET_STATUS_BAR_NOTIFICATIONS";
    private static final String ACTION_MARK_AS_READ = "ru.taaasty.PusherService.action.MARK_AS_READ";
    private static final String ACTION_REFRESH_NOTIFICATIONS = "ru.taaasty.PusherService.action.ACTION_REFRESH_NOTIFICATIONS";

    private static final String EXTRA_NOTIFICATION_ID = "ru.taaasty.PusherService.action.EXTRA_NOTIFICATION_ID";

    private static final String EXTRA_SET_STATUS_BAR_NOTIFICATIONS = "ru.taaasty.PusherService.action.EXTRA_SET_STATUS_BAR_NOTIFICATIONS";

    private final IBinder mBinder = new LocalBinder();

    private ApiMessenger mApiMessenger;

    private Pusher mPusher;

    private Handler mHandler;

    private boolean mPusherMustBeActive = false;

    private Subscription mSendAuthReadySubscription = SubscriptionHelper.empty();

    @UpdateNotificationsStatus
    private volatile int mUpdateNotificationsStatus;

    private volatile LinkedList<Notification> mNotifications;

    private StatusBarNotification mStatusBarNotification;

    private Gson mGson;

    private EventBus mEventBus;

    private MessagingStatus mLastMessagingStatus;

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

    /**
     * Проверяем, что pusher запущен. Пересоединяемся, если нет
     * @param context
     */
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

    /**
     * Обновление списка уведомлений с сервера
     * @param context
     */
    public static void refreshNotifications(Context context) {
        Intent intent = new Intent(context, PusherService.class);
        intent.setAction(ACTION_REFRESH_NOTIFICATIONS);
        context.startService(intent);
    }

    public static void enableStatusBarNotifications(Context context) {
        setStatusBarNotifications(context, true);
    }

    public static void disableStatusBarNotifications(Context context) {
        setStatusBarNotifications(context, false);
    }

    /**
     * Включение или выключение получения нотификаций в статусбаре
     * @param context
     * @param enable
     */
    private static void setStatusBarNotifications(Context context, boolean enable) {
        Intent intent = new Intent(context, PusherService.class);
        intent.setAction(ACTION_SET_STATUS_BAR_NOTIFICATIONS);
        intent.putExtra(EXTRA_SET_STATUS_BAR_NOTIFICATIONS, enable);
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
        mHandler = new Handler();
        mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
        mUpdateNotificationsStatus = UPDATE_NOTIFICATIONS_STATUS_NONE;
        mNotifications = new LinkedList<>();
        mStatusBarNotification = new StatusBarNotification(this);
        mGson = NetworkUtils.getInstance().getGson();
        mEventBus = EventBus.getDefault();
        mEventBus.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                if (DBG) Log.v(TAG, "ACTION_START");
                mPusherMustBeActive = true;
                pusherConnect();
            } else if (ACTION_STOP.equals(action)) {
                mPusherMustBeActive = false;
                if (DBG) Log.v(TAG, "ACTION_STOP");
                stopSelf();
            } else if (ACTION_MARK_AS_READ.equals(action)) {
                handleMarkAsRead(intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1));
            } else if (ACTION_REFRESH_NOTIFICATIONS.equals(action)) {
                if (mUpdateNotificationsStatus == UPDATE_NOTIFICATIONS_STATUS_READY) sentAuthReady();
            } else if (ACTION_SET_STATUS_BAR_NOTIFICATIONS.equals(action)) {
                mStatusBarNotification.addEnableDisableNotifications(intent.getBooleanExtra(EXTRA_SET_STATUS_BAR_NOTIFICATIONS, true));
            }
        }

        // Не восстанавливаемся после завершения не через ACTION_STOP
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        mHandler.removeCallbacks(mReconnectPusherRunnable);
        mHandler = null;
        mEventBus.unregister(this);
        mStatusBarNotification.onDestroy();
        mStatusBarNotification = null;
        mSendAuthReadySubscription.unsubscribe();
        destroyPusher();
        mApiMessenger = null;
    }

    @Override
    public void onEvent(String channelName, String eventName, String data) {
        if (!getMessagingChannelName().equals(channelName)) {
            if (DBG) Log.v(TAG, "onEvent() unknown channel: " + channelName + " event: " + eventName + " data: " + data);
            return;
        }

        if (DBG) Log.v(TAG, "onEvent() channel: " + channelName + " event: " + eventName + " data: " + data);

        if (eventName == null) eventName = "";
        switch (eventName) {
            case EVENT_STATUS:
                MessagingStatus status = mGson.fromJson(data, MessagingStatus.class);
                if (!Objects.equals(status, mLastMessagingStatus)) {
                    mLastMessagingStatus = status;
                    mEventBus.post(new MessagingStatusReceived(status));
                }
                break;
            case EVENT_ACTIVE_CONVERSATIONS:
                break;
            case EVENT_PUSH_MESSAGE:
                Conversation.Message message =  mGson.fromJson(data, Conversation.Message.class);
                mEventBus.post(new MessageChanged(message));
                mStatusBarNotification.append(message);
                break;
            case EVENT_UPDATE_CONVERSATION:
                Conversation conversation =  mGson.fromJson(data, Conversation.class);
                mEventBus.post(new ConversationChanged(conversation));
                break;
            case EVENT_UPDATE_MESSAGES:
                UpdateMessages updateMessages =  mGson.fromJson(data, UpdateMessages.class);
                mEventBus.post(new UpdateMessagesReceived(updateMessages));
                break;
            case EVENT_PUSH_NOTIFICATION:
                Notification n = mGson.fromJson(data, Notification.class);
                addNotification(n);
                mStatusBarNotification.append(n);
                break;
            case EVENT_UPDATE_NOTIFICATIONS:
                break;
            default:
                break;
        }
    }

    @Nullable
    public MessagingStatus getLastMessagingStatus() {
        return mLastMessagingStatus;
    }

    public void onEventMainThread(RelationshipChanged relationshipChanged) {
        Relationship newRelationship = relationshipChanged.relationship;
        long me = UserManager.getInstance().getCurrentUserId();
        long him;

        if (!newRelationship.isMyRelationToHim(me)) return; // Не интересно
        him = newRelationship.getToId();

        // Меняем relation
        synchronized (this) {
            ListIterator<Notification> l = mNotifications.listIterator();
            while (l.hasNext()) {
                Notification notification = l.next();
                if (!notification.isTypeRelationship()) return;
                if (notification.sender.getId() == him) {
                    Notification newNotification = Notification.changeSenderRelation(notification, newRelationship);
                    l.set(newNotification);
                    mEventBus.post(new NotificationReceived(newNotification));
                }
            }
        }
    }

    public void onEventMainThread(ConversationVisibilityChanged event) {
        if (mStatusBarNotification != null) mStatusBarNotification.onConversationVisibilityChanged(event);
    }

    @Override
    public void onAuthenticationFailure(String message, Exception e) {
        if (DBG) Log.v(TAG, "onAuthenticationFailure() msg: " + message, e);
        setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_FAILURE, message, e);
    }

    @Override
    public synchronized void onSubscriptionSucceeded(String channelName) {
        if (DBG) Log.v(TAG, "onSubscriptionSucceeded() channel: " + channelName);
        sentAuthReady();
    }

    public synchronized boolean isNotificationListEmpty() {
        return mNotifications.isEmpty();
    }

    public synchronized NotificationsStatus getNotificationsStatus() {
        return new NotificationsStatus(mUpdateNotificationsStatus, mUpdateNotificationsError);
    }

    /**
     * @return Копия списка уведомлений
     */
    public synchronized List<Notification> getNotifications() {
        // XXX считаем, что Notification - immutable, копируем ссылки
        return new ArrayList<>(mNotifications);
    }

    public String getSocketId() {
        return mPusher.getConnection().getSocketId();
    }

    private void createPusher() {
        destroyPusher();
        PusherOptions options = new PusherOptions()
                .setEncrypted(false)
                .setAuthorizer(mAuthorizer)
                ;

        mPusher = new Pusher(BuildConfig.PUSHER_KEY, options);
        mPusher.subscribePrivate(getMessagingChannelName(), this,
                EVENT_STATUS,
                EVENT_ACTIVE_CONVERSATIONS,
                EVENT_PUSH_MESSAGE,
                EVENT_UPDATE_CONVERSATION,
                EVENT_UPDATE_MESSAGES,
                EVENT_UPDATE_NOTIFICATIONS,
                EVENT_PUSH_NOTIFICATION);
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

    private void reconnectPusherLater() {
        if (mHandler == null) return;
        mHandler.removeCallbacks(mReconnectPusherRunnable);
        mHandler.postDelayed(mReconnectPusherRunnable, PUSHER_RECONNECT_TIMEOUT);
    }

    private final Runnable mReconnectPusherRunnable = new Runnable() {
        @Override
        public void run() {
            if (DBG) Log.v(TAG, "reconnect pusher");
            pusherConnect();
        }
    };

    private String getMessagingChannelName() {
        return "private-" + UserManager.getInstance().getCurrentUserId() + "-messaging";
    }

    private void setUpdateNotificationsStatus(@UpdateNotificationsStatus int status) {
        setUpdateNotificationsStatus(status, null, null);
    }

    private synchronized void addNotification(Notification notification) {
        mNotifications.addFirst(notification);
        mEventBus.post(new NotificationReceived(notification));
    }

    private synchronized void setUpdateNotificationsStatus(@UpdateNotificationsStatus int status, String error, Exception e) {
        if (DBG) Log.v(TAG, "setUpdateNotificationsStatus status: " + updateNotificationStatusName(status) + " error: "  + error);
        mUpdateNotificationsStatus = status;
        mUpdateNotificationsError = error;

        ru.taaasty.events.NotificationsStatus event = new ru.taaasty.events.NotificationsStatus(new NotificationsStatus(status, error));
        mEventBus.post(event);
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

    private void sentAuthReady() {
        if (mPusher == null || mPusher.getConnection().getState() != ConnectionState.CONNECTED) {
            if (DBG) Log.v(TAG, "sentAuthReady pusher disconnected");
            return;
        }
        setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_LOADING);
        mSendAuthReadySubscription.unsubscribe();

        mSendAuthReadySubscription = mApiMessenger.authReady(mPusher.getConnection().getSocketId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mPusherReadyResponseObserver);

    }

    private final Observer<PusherReadyResponse> mPusherReadyResponseObserver = new Observer<PusherReadyResponse>() {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            Log.e(TAG, "authReady failed", e);
            destroyPusher();
            setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_FAILURE, getString(R.string.error_loading_notifications), new RuntimeException(e));
            reconnectPusherLater();
        }

        @Override
        public void onNext(PusherReadyResponse pusherReadyResponse) {
            try {
                synchronized (PusherService.this) {
                    mNotifications.clear();
                    mNotifications.addAll(pusherReadyResponse.notifications);
                    setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_READY);
                }
            } catch (Exception e) {
                Log.e(TAG, "authReady failed", e);
                destroyPusher();
                setUpdateNotificationsStatus(UPDATE_NOTIFICATIONS_STATUS_FAILURE, getString(R.string.error_loading_notifications), e);
                reconnectPusherLater();
            }
        }
    };

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
                ListIterator<Notification> i = mNotifications.listIterator();
                while (i.hasNext()) {
                    Notification old = i.next();
                    if (old.id == notification.id) {
                        i.set(notification);
                        found = true;
                        break;
                    }
                }
                if (!found) mNotifications.addFirst(notification);
                mEventBus.post(new NotificationReceived(notification));
            }

        }
    };

    private final ConnectionEventListener mConnectionEventListener = new ConnectionEventListener() {
        @Override
        public void onConnectionStateChange(ConnectionStateChange change) {
            if (DBG) Log.v(TAG, "onConnectionStateChange() change: " + change.getPreviousState() + " -> " + change.getCurrentState());
            if (mPusher == null) return;
            if (mPusherMustBeActive && change.getCurrentState() == ConnectionState.DISCONNECTED) {
                reconnectPusherLater();
            } else if (change.getCurrentState() == ConnectionState.CONNECTED) {
                mHandler.removeCallbacks(mReconnectPusherRunnable);
            }
        }

        @Override
        public void onError(String message, String code, Exception e) {
            if (DBG) Log.v(TAG, "onError() message: " + message + " code: " + code, e);
        }
    };

    private String updateNotificationStatusName(@UpdateNotificationsStatus int status) {
        switch (status) {
            case UPDATE_NOTIFICATIONS_STATUS_FAILURE:
                return "failure";
            case UPDATE_NOTIFICATIONS_STATUS_LOADING:
                return "loading";
            case UPDATE_NOTIFICATIONS_STATUS_NONE:
                return "none";
            case UPDATE_NOTIFICATIONS_STATUS_READY:
                return "ready";
        }
        return "unknown";
    }
}
