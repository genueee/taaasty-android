package ru.taaasty;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.google.gson.Gson;
import com.pusher.client.AuthorizationFailureException;
import com.pusher.client.Authorizer;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.apache.commons.io.IOUtils;

import java.io.Reader;

import de.greenrobot.event.EventBus;
import okhttp3.ResponseBody;
import retrofit2.Call;
import ru.taaasty.events.pusher.MessagesRemoved;
import ru.taaasty.events.UiVisibleStatusChanged;
import ru.taaasty.events.pusher.UserMessagesRemoved;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.events.pusher.MessageChanged;
import ru.taaasty.events.pusher.MessagingStatusReceived;
import ru.taaasty.events.pusher.NotificationMarkedAsRead;
import ru.taaasty.events.pusher.NotificationReceived;
import ru.taaasty.events.pusher.UpdateMessagesReceived;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.MessagingStatus;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.PusherEventUpdateNotifications;
import ru.taaasty.rest.model.RemovedMessages;
import ru.taaasty.rest.model.RemovedUserMessages;
import ru.taaasty.rest.model.UpdateMessages;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.ConversationList;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.PusherMessage;
import ru.taaasty.rest.model.conversations.TypedPushMessage;
import ru.taaasty.utils.GcmUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.Objects;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

public class PusherService extends Service {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "PusherService";
    public static final int PUSHER_RECONNECT_TIMEOUT = 10000;

    private static final int BACKGROUND_MAX_WORK_TIME = (DBG ? 1 : 3) * 60 * 1000;

    /**
     *  Кол-во активных и непрочитанных переписок и непрочитанных уведомлений
     *  Тип: {@linkplain MessagingStatus}
     */
    public static final String EVENT_STATUS = "status";

    /**
     * Событие - активные переписки
     * Тип: {@linkplain ConversationList}
     */
    public static final String EVENT_ACTIVE_CONVERSATIONS = "active_conversations";

    /**
     * Событие - обновление переписки
     * Тип: {@linkplain ru.taaasty.rest.model.Notification}
     */
    public static final String EVENT_UPDATE_CONVERSATION = "update_conversation";

    /**
     * Новое сообщение
     * Тип: {@linkplain Message}
     */
    public static final String EVENT_PUSH_MESSAGE = "push_message";
    /**
     * Набирают сообщение
     * Тип: {@linkplain Message}
     */
    public static final String EVENT_TYPED = "typed";
    /**
     * Обновление статуса сообщений
     * Тип: {@linkplain ru.taaasty.rest.model.UpdateMessages}
     */
    public static final String EVENT_UPDATE_MESSAGES = "update_messages";

    /**
     * push notification - пришло уведомление.
     * Тип: {@linkplain ru.taaasty.rest.model.Notification}
     */
    public static final String EVENT_PUSH_NOTIFICATION = "push_notification";

    /**
     * push notification - удалены сообщения в чате.
     * Расылается при нажатии "удалить у себя", т.е. сообщения удаляются только у автора, у остальных
     * остаются видимыми.
     * Тип: {@linkplain ru.taaasty.rest.model.RemovedMessages}
     */
    public static final String EVENT_DELETE_MESSAGES = "delete_messages";


    /**
     * push notificaton - удалены собщения в чате.
     * Вызывается при нажатии "удалить у всех", т.е. сообщения удалены и у автора
     * и у всех пользователей
     * Тип: {@linkplain ru.taaasty.rest.model.RemovedUserMessages}.
     * В списке - новые, измененные сообщения. У новых сообщений остается тот же ID,
     * но меняется тип сообщения на System и текст.
     */
    public static final String EVENT_DELETE_USER_MESSAGES = "delete_user_messages";

    // Групповые
    /**
     * То же самое, что и {@linkplain #EVENT_STATUS}, только шлются при изменениях в групповых чатах.
     * Значения в событиях совпадатют
     */
    public static final String EVENT_GROUP_STATUS = "group_status";
    public static final String EVENT_GROUP_ACTIVE_CONVERSATIONS = "group_active_conversations";
    public static final String EVENT_GROUP_UPDATE_CONVERSATION = "group_update_conversation";

    /**
     * Новое сообщение в групповом чате
     * Тип: {@linkplain Message}
     */
    public static final String EVENT_GROUP_PUSH_MESSAGE = "group_push_message";
    public static final String EVENT_GROUP_UPDATE_MESSAGES = "group_update_messages";
    public static final String EVENT_GROUP_NOTIFICATION = "group_push_notification";
    public static final String EVENT_GROUP_UPDATE_NOTIFICATIONS = "group_update_notifications";
    public static final String EVENT_GROUP_DELETE_MESSAGES = "group_delete_messages";
    public static final String EVENT_GROUP_DELETE_USER_MESSAGES = "group_delete_user_messages";


    //Обсуждения:
    /**
     * То же самое, что и {@linkplain #EVENT_STATUS}, только шлются при изменениях в обсуждениях записи.
     * Значения в событиях совпадатют
     */
    public static final String EVENT_PUBLIC_STATUS = "public_status";

    public static final String EVENT_PUBLIC_ACTIVE_CONVERSATIONS = "public_active_conversations";
    public static final String EVENT_PUBLIC_UPDATE_CONVERSATION = "public_update_conversation";

    /**
     * Новое сообщение в обсуждениях записи
     * Тип: {@linkplain Message}
     */
    public static final String EVENT_PUBLIC_PUSH_MESSAGE = "public_push_message";

    public static final String EVENT_PUBLIC_UPDATE_MESSAGES = "public_update_messages";
    public static final String EVENT_PUBLIC_NOTIFICATION = "public_push_notification";
    public static final String EVENT_PUBLIC_UPDATE_NOTIFICATIONS = "public_update_notifications";
    public static final String EVENT_PUBLIC_DELETE_MESSAGES = "public_delete_messages";
    public static final String EVENT_PUBLIC_DELETE_USER_MESSAGES = "public_delete_user_messages";

    /**
     * Уведомления отмечены как прочитвнные.
     * Тип: notifications: [{ id: notification.id, read_at: notification.read_at }]
     */
    public static final String EVENT_UPDATE_NOTIFICATIONS = "update_notifications";

    private static final String ACTION_START = "ru.taaasty.PusherService.action.START";

    public static final String ACTION_HANDLE_GCM_PUSH = "ru.taaasty.PusherService.action.ACTION_HANDLE_GCM_PUSH";

    private final IBinder mBinder = new LocalBinder();

    private Pusher mPusher;

    private volatile Handler mHandler;

    private Subscription mSendAuthReadySubscription = Subscriptions.unsubscribed();

    private Gson mGson;

    private MessagingStatus mLastMessagingStatus;

    private boolean mIsStarted;

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
        context.stopService(intent);
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
        mGson = NetworkUtils.getGson();
        EventBus.getDefault().register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                if (DBG) Log.v(TAG, "ACTION_START");
                mIsStarted = true;
                pusherConnect();
                resetBackgroundTimer();
            } else if (ACTION_HANDLE_GCM_PUSH.equals(action)) {
                onGcmPushReceived(intent);
            }
        }

        // Не восстанавливаемся после завершения не через ACTION_STOP
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        EventBus.getDefault().unregister(this);
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
        mSendAuthReadySubscription.unsubscribe();
        destroyPusher();
        mIsStarted = false;
    }

    public void onEventMainThread(UiVisibleStatusChanged status) {
        if (!mIsStarted) return;
        if (status.activeActivitiesCount == 0) {
            resetBackgroundTimer();
        } else {
            stopBackgroundTimer();
        }
    }

    private final com.pusher.client.channel.PrivateChannelEventListener mPrivateChannelEventListener = new  com.pusher.client.channel.PrivateChannelEventListener() {

        @WorkerThread
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
                case EVENT_GROUP_STATUS:
                case EVENT_PUBLIC_STATUS:
                    MessagingStatus status = mGson.fromJson(data, MessagingStatus.class);
                    if (!Objects.equals(status, mLastMessagingStatus)) {
                        mLastMessagingStatus = status;
                        EventBus.getDefault().post(new MessagingStatusReceived(status));
                    }
                    break;
                case EVENT_ACTIVE_CONVERSATIONS:
                case EVENT_GROUP_ACTIVE_CONVERSATIONS:
                case EVENT_PUBLIC_ACTIVE_CONVERSATIONS:
                    break;
                case EVENT_GROUP_PUSH_MESSAGE:
                case EVENT_PUSH_MESSAGE:
                case EVENT_PUBLIC_PUSH_MESSAGE:
                    PusherMessage pusherMessage =  mGson.fromJson(data, PusherMessage.class);
                    Message message = mGson.fromJson(data, Message.class);
                    EventBus.getDefault().post(new MessageChanged(pusherMessage.conversation, message));
                    break;
                case EVENT_UPDATE_CONVERSATION:
                case EVENT_GROUP_UPDATE_CONVERSATION:
                case EVENT_PUBLIC_UPDATE_CONVERSATION:
                    Conversation conversation =  mGson.fromJson(data, Conversation.class);
                    EventBus.getDefault().post(new ConversationChanged(conversation));
                    break;
                case EVENT_UPDATE_MESSAGES:
                case EVENT_GROUP_UPDATE_MESSAGES:
                case EVENT_PUBLIC_UPDATE_MESSAGES:
                    UpdateMessages updateMessages =  mGson.fromJson(data, UpdateMessages.class);
                    EventBus.getDefault().post(new UpdateMessagesReceived(updateMessages));
                    break;
                case EVENT_PUSH_NOTIFICATION:
                case EVENT_GROUP_NOTIFICATION:
                case EVENT_PUBLIC_NOTIFICATION:
                    Notification notification = mGson.fromJson(data, Notification.class);
                    EventBus.getDefault().post(new NotificationReceived(notification));
                    break;
                case EVENT_UPDATE_NOTIFICATIONS:
                case EVENT_GROUP_UPDATE_NOTIFICATIONS:
                case EVENT_PUBLIC_UPDATE_NOTIFICATIONS:
                    PusherEventUpdateNotifications event = mGson.fromJson(data, PusherEventUpdateNotifications.class);
                    if (!event.notifications.isEmpty()) {
                        EventBus.getDefault().post(new NotificationMarkedAsRead(event.notifications));
                    }
                    break;
                case EVENT_DELETE_MESSAGES:
                case EVENT_GROUP_DELETE_MESSAGES:
                case EVENT_PUBLIC_DELETE_MESSAGES:
                    RemovedMessages removedMessages = mGson.fromJson(data, RemovedMessages.class);
                    if (removedMessages.messages.length > 0) {
                        EventBus.getDefault().post(new MessagesRemoved(removedMessages));
                    }
                    break;
                case EVENT_DELETE_USER_MESSAGES:
                case EVENT_GROUP_DELETE_USER_MESSAGES:
                case EVENT_PUBLIC_DELETE_USER_MESSAGES:
                    RemovedUserMessages removedUserMessages = mGson.fromJson(data, RemovedUserMessages.class);
                    if (removedUserMessages.messages.length > 0) {
                        EventBus.getDefault().post(new UserMessagesRemoved(removedUserMessages));
                    }
                    break;
                case EVENT_TYPED:
                    TypedPushMessage typedPushMessage = mGson.fromJson(data, TypedPushMessage.class);
                    EventBus.getDefault().post(typedPushMessage);
                default:
                    break;
            }
        }

        @WorkerThread
        @Override
        public void onAuthenticationFailure(String message, Exception e) {
            if (DBG) Log.v(TAG, "onAuthenticationFailure() msg: " + message, e);
        }

        @WorkerThread
        @Override
        public synchronized void onSubscriptionSucceeded(String channelName) {
            if (DBG) Log.v(TAG, "onSubscriptionSucceeded() channel: " + channelName);
            if (mHandler == null) return;
            mHandler.removeCallbacks(mSendAuthReadyRunnable);
            mHandler.post(mSendAuthReadyRunnable);
        }

        private final Runnable mSendAuthReadyRunnable = () -> sentAuthReady();
    };

    @Nullable
    public MessagingStatus getLastMessagingStatus() {
        return mLastMessagingStatus;
    }

    public boolean hasUnreadNotifications() {
        return mLastMessagingStatus != null && mLastMessagingStatus.unreadNotificationsCount > 0;
    }

    private void createPusher() {
        destroyPusher();
        PusherOptions options = new PusherOptions()
                .setEncrypted(false)
                .setAuthorizer(mAuthorizer);

        mPusher = new Pusher(BuildConfig.PUSHER_KEY, options);
        mPusher.subscribePrivate(getMessagingChannelName(), mPrivateChannelEventListener,
                EVENT_STATUS,
                EVENT_ACTIVE_CONVERSATIONS,
                EVENT_PUSH_MESSAGE,
                EVENT_UPDATE_CONVERSATION,
                EVENT_UPDATE_MESSAGES,
                EVENT_UPDATE_NOTIFICATIONS,
                EVENT_PUSH_NOTIFICATION,
                // TODO delete messages

                EVENT_GROUP_STATUS,
                EVENT_GROUP_ACTIVE_CONVERSATIONS,
                EVENT_GROUP_PUSH_MESSAGE,
                EVENT_GROUP_UPDATE_MESSAGES,
                EVENT_GROUP_NOTIFICATION,
                EVENT_GROUP_UPDATE_NOTIFICATIONS,
                EVENT_GROUP_UPDATE_CONVERSATION,
                EVENT_GROUP_DELETE_MESSAGES,
                EVENT_GROUP_DELETE_USER_MESSAGES,

                EVENT_PUBLIC_STATUS,
                EVENT_PUBLIC_ACTIVE_CONVERSATIONS,
                EVENT_PUBLIC_UPDATE_CONVERSATION,
                EVENT_PUBLIC_PUSH_MESSAGE,
                EVENT_PUBLIC_UPDATE_MESSAGES,
                EVENT_PUBLIC_NOTIFICATION,
                EVENT_PUBLIC_UPDATE_NOTIFICATIONS,
                EVENT_PUBLIC_DELETE_MESSAGES,
                EVENT_PUBLIC_DELETE_USER_MESSAGES,
                EVENT_TYPED
                );
    }

    private void pusherConnect() {
        if (mPusher != null && mPusher.getConnection().getState() != ConnectionState.DISCONNECTED) {
            return;
        }
        if (mPusher == null) createPusher();
        mPusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                if (DBG) Log.v(TAG, "onConnectionStateChange() change: " + change.getPreviousState() + " -> " + change.getCurrentState());
                if (mPusher == null) return;
                if (change.getCurrentState() == ConnectionState.DISCONNECTED) {
                    reconnectPusherLater();
                } else if (change.getCurrentState() == ConnectionState.CONNECTED) {
                    mHandler.removeCallbacks(mReconnectPusherRunnable);
                }
            }

            @Override
            public void onError(String message, String code, Exception e) {
                if (DBG) Log.v(TAG, "onError() message: " + message + " code: " + code, e);
            }
        });
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

    private final Runnable mReconnectPusherRunnable = () -> {
        if (DBG) Log.v(TAG, "reconnect pusher");
        pusherConnect();
    };

    private String getMessagingChannelName() {
        return "private-" + Session.getInstance().getCurrentUserId() + "-messaging";
    }

    public class LocalBinder extends Binder {
        public PusherService getService() {
            return PusherService.this;
        }
    }

    private final Authorizer mAuthorizer = (channelName, socketId) -> {
        try {
            Call<ResponseBody> responseBodyCall = RestClient.getAPiMessenger().authPusher(channelName, socketId);
            Reader responseBodyReader = responseBodyCall.execute().body().charStream();
            return IOUtils.toString(responseBodyReader);
        } catch (Exception e) {
            throw new AuthorizationFailureException(e);
        }
    };

    @WorkerThread
    private void sentAuthReady() {
        if (DBG) Log.d(TAG, "sentAuthReady()");
        if (mPusher == null || mPusher.getConnection().getState() != ConnectionState.CONNECTED) {
            if (DBG) Log.v(TAG, "sentAuthReady pusher disconnected");
            return;
        }
        mSendAuthReadySubscription.unsubscribe();

        // Игнорируем все ошибки и результаты
        mSendAuthReadySubscription =  RestClient.getAPiMessenger().authReady2(mPusher.getConnection().getSocketId())
                //.subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(new Observer<Void>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(Void aVoid) {}
                });

    }

    private void onGcmPushReceived(Intent intent) {
        if (mPusher != null) {
            if (DBG) Log.v(TAG, "skip GCM sync: pusher is connected");
            GcmBroadcastReceiver.completeWakefulIntent(intent);
        } else if (!Session.getInstance().isAuthorized()) {
            if (DBG) Log.v(TAG, "skip GCM sync: not authorized");
            GcmBroadcastReceiver.completeWakefulIntent(intent);
        } else {
            String messageType = GcmUtils.getGcmNotificationType(intent.getExtras());
            if (GcmUtils.GCM_NOTIFICATION_TYPE_PUSH_NOTIFICATION.equals(messageType)) {
                StatusBarNotifications ssb = StatusBarNotifications.getInstance();
                ssb.onGcmPushNotificationReceived(intent);
                stopSelf();
                //pusherConnect();
            } else if (GcmUtils.GCM_NOTIFICATION_TYPE_PUSH_MESSAGE.equals(messageType)) {
                StatusBarNotifications ssb = StatusBarNotifications.getInstance();
                ssb.onGcmPushConversationReceived(intent);
                pusherConnect();
                resetBackgroundTimer();
            } else {
                GcmBroadcastReceiver.completeWakefulIntent(intent);
                throw new IllegalStateException();
            }
        }
    }

    private void resetBackgroundTimer() {
        mHandler.removeCallbacks(mCheckStopRunnable);
        if (((TaaastyApplication)getApplication()).isUiActive()) return;
        if (DBG) Log.d(TAG, "background timeout timer set to " + BACKGROUND_MAX_WORK_TIME / 1000 + " seconds");
        mHandler.postDelayed(mCheckStopRunnable, BACKGROUND_MAX_WORK_TIME);
    }

    private void stopBackgroundTimer() {
        if (DBG) Log.d(TAG, "background timeout timer stopped");
        if (mHandler != null) mHandler.removeCallbacks(mCheckStopRunnable);
    }

    private final Runnable mCheckStopRunnable = () -> {
        if (!((TaaastyApplication)getApplication()).isUiActive()) {
            if (DBG) Log.d(TAG, "stop self on timeout");
            stopSelf();
        }
    };

}
