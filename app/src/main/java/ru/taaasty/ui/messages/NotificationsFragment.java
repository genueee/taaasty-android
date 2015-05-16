package ru.taaasty.ui.messages;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.IntentService;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.NotificationsAdapter;
import ru.taaasty.events.MarkAllAsReadRequestCompleted;
import ru.taaasty.events.MessagingStatusReceived;
import ru.taaasty.events.NotificationMarkedAsRead;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.model.Notification;
import ru.taaasty.model.NotificationList;
import ru.taaasty.model.Relationship;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class NotificationsFragment extends Fragment implements ServiceConnection {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationsFragment";

    private OnFragmentInteractionListener mListener;

    private RecyclerView mListView;

    private TextView mAdapterEmpty;

    private View mProgressView;

    private View mMarkAsReadButton;

    PusherService mPusherService;

    boolean mBound = false;

    private NotificationsAdapter mAdapter;

    private boolean mWaitingMessagingStatus;

    private Subscription mUserInfoSubscription = Subscriptions.unsubscribed();

    private Subscription mFollowSubscription = Subscriptions.unsubscribed();

    private NotificationsLoader mLoader;

    public static NotificationsFragment newInstance() {
        return new NotificationsFragment();
    }

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notifications, container, false);
        mListView = (RecyclerView)root.findViewById(R.id.list);
        mAdapterEmpty = (TextView)root.findViewById(R.id.empty_text);
        mProgressView = root.findViewById(R.id.progress);
        mMarkAsReadButton = root.findViewById(R.id.mark_all_as_read);
        mMarkAsReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                markAsReadClicked();
            }
        });

        mAdapter = new NotificationsAdapter(getActivity(), mInteractionListener) {
            @Override
            public void onBindViewHolder(ViewHolder viewHolder, int position) {
                super.onBindViewHolder(viewHolder, position);
                if (viewHolder instanceof ViewHolderItem) mLoader.onBindViewHolder(viewHolder, position, mAdapter.getItemCount());
            }
        };
        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.followings_list_divider));
        mListView.setAdapter(mAdapter);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                boolean atTop = !mListView.canScrollVertically(-1);
                mListener.onListScrolled(dy, atTop);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                mListener.onListScrollStateChanged(newState);
            }
        });
        mWaitingMessagingStatus = false;
        mLoader = new NotificationsLoader();
        mLoader.onCreate();
        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        Intent intent = new Intent(getActivity(), PusherService.class);
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoader.refreshFeed();
        setupLoadingState();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        if (mBound) {
            getActivity().unbindService(this);
            mBound = false;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mLoader != null) {
            mLoader.onDestroy();
            mLoader = null;
        }
        mListView.setOnScrollListener(null);
        mListView = null;
        mListener = null;
        mProgressView = null;
        mMarkAsReadButton = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFollowSubscription.unsubscribe();
        mUserInfoSubscription.unsubscribe();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PusherService.LocalBinder binder = (PusherService.LocalBinder) service;
        mPusherService = binder.getService();
        mBound = true;
        if (mAdapter != null) setupMarkAsReadButtonStatus();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }

    public void onEventMainThread(MessagingStatusReceived status) {
        mWaitingMessagingStatus = false;
        if (DBG) Log.v(TAG, "MessagingStatusReceived " + status);
        setupMarkAsReadButtonStatus();
    }

    public void onEventMainThread(MarkAllAsReadRequestCompleted status) {
        if (DBG) Log.v(TAG, "MarkAllAsReadRequestCompleted " + status);
        setupMarkAsReadButtonStatus();
    }

    public void onEventMainThread(NotificationReceived event) {
        if (mAdapter == null ) return;
        if (DBG) Log.v(TAG, "NotificationReceived " + event);
        mAdapter.addNotification(event.notification);
        scrollShowTopPosition();
    }

    /**
     * При отписке или отписке юзера на тлог в списке нотификаций нужно изменить кнопку "подтвердить/уже в друзьях"
     * Через пушер новая нотификация не приходит.
     * @param relationshipChanged
     */
    // Это лютый пиздец, что оно здесь, хотя да и похуй
    public void onEventMainThread(RelationshipChanged relationshipChanged) {
        Relationship newRelationship = relationshipChanged.relationship;
        long me = UserManager.getInstance().getCurrentUserId();
        long him;
        boolean changed = false;

        if (!newRelationship.isMyRelationToHim(me)) return; // Не интересно
        him = newRelationship.getToId();

        // Меняем relation
        synchronized (this) {
            for (Notification notification : mAdapter.getNotifications().getItems()) {
                if (!notification.isTypeRelationship()) return;
                if (notification.sender.getId() == him) {
                    Notification newNotification = Notification.changeSenderRelation(notification, newRelationship);
                    mAdapter.addNotification(newNotification);
                    changed = true;
                }
            }
        }
        if (changed) scrollShowTopPosition();
    }

    public void onEventMainThread(NotificationMarkedAsRead event) {
        NotificationsAdapter.NotificationsList notifications = mAdapter.getNotifications();

        boolean changed = false;
        int notificationsSize = notifications.size();
        for (int responseIdx = 0; responseIdx < event.id.length; ++responseIdx) {
            for (int i = 0; i < notificationsSize; ++i) {
                Notification notification = notifications.get(i);
                if (notification.id == event.id[responseIdx]) {
                    if (!notification.isMarkedAsRead()) {
                        Notification newNotification = Notification.markAsRead(notification, event.readAt[responseIdx]);
                        mAdapter.addNotification(newNotification);
                        changed = true;
                    }
                    break;
                }
            }
        }
        if (changed) scrollShowTopPosition();
    }

    /**
     * Если верхний элемент показан частично - скроллим, чтобы он был виден полностью
     */
    private void scrollShowTopPosition() {
        View top = mListView.getChildAt(0);
        boolean listAtTop = top == null || (mListView.getChildAdapterPosition(top) == 0);
        if (listAtTop) mListView.smoothScrollToPosition(0);
    }

    public void setupLoadingState() {
        if (DBG) Log.v(TAG, "setupLoadingState");
        if (mProgressView == null || mLoader == null) return;
        boolean showNoItemsIndicator = !mLoader.isRefreshing() && mAdapter.isEmpty();
        mAdapterEmpty.setVisibility(showNoItemsIndicator ? View.VISIBLE : View.GONE);
        mProgressView.setVisibility(mLoader.isRefreshing() && mAdapter.isEmpty() ? View.VISIBLE : View.INVISIBLE);
    }

    /**
     * Показ/скрытие кнопки "пометить все как прочитанные"
     */
    void setupMarkAsReadButtonStatus() {
        if (mMarkAsReadButton == null) return;
        boolean isVisible = !mWaitingMessagingStatus && mBound && mPusherService.hasUnreadNotifications();
        mMarkAsReadButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    void follow(Notification notification) {
        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                relApi.follow(String.valueOf(notification.sender.getId())));
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FollowerObserver(notification.id));
    }

    void unfollow(Notification notification) {
        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                relApi.unfollow(String.valueOf(notification.sender.getId())));
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FollowerObserver(notification.id));
    }

    void markAsReadClicked() {
        if (mMarkAsReadButton == null) return;
        IntentService.markAllNotificationsAsRead(getActivity());
        // MessagingStatus с pusher обычно приходит позже, чем завершается запрос.
        // Ждем его, чтобы не мелькать кнопкой.
        mWaitingMessagingStatus = true;
        setupMarkAsReadButtonStatus();
    }

    public class FollowerObserver implements Observer<Relationship> {

        private long notificationId;

        public FollowerObserver(long notificationId) {
            this.notificationId = notificationId;
        }

        @Override
        public void onCompleted() {
            if (mAdapter != null) mAdapter.onNotificationFollowUnfollowStopped(notificationId);
        }

        @Override
        public void onError(Throwable e) {
            if (mListener != null) mListener.notifyError(getString(R.string.error_follow), e);
            if (mAdapter != null) mAdapter.onNotificationFollowUnfollowStopped(notificationId);
        }

        @Override
        public void onNext(Relationship relationship) {
            // Здесь сервис должен поймать это событие и сгенерить новое NotificationChanged,
            // которое должен поймать этот объект и передать адаптеру
            EventBus.getDefault().post(new RelationshipChanged(relationship));
        }
    }

    private void markNotificationRead(Notification notification) {
        if (notification.isMarkedAsRead()) return;
        IntentService.markNotificationAsRead(getActivity(), notification.id);
    }

    private final NotificationsAdapter.InteractionListener mInteractionListener = new NotificationsAdapter.InteractionListener() {

        @Override
        public void onNotificationClicked(View v, Notification notification) {
            markNotificationRead(notification);
            notification.startOpenPostActivity(getActivity());
        }

        @Override
        public void onAvatarClicked(View v, Notification notification) {
            TlogActivity.startTlogActivity(getActivity(), notification.sender.getId(), v, R.dimen.avatar_small_diameter);
        }

        @Override
        public void onAddButtonClicked(View v, Notification notification) {
            follow(notification);
        }

        @Override
        public void onAddedButtonClicked(View v, Notification notification) {
            unfollow(notification);
        }
    };

    /**
     * Подгрузчик нотификаций
     */
    class NotificationsLoader {
        public static final int ENTRIES_TO_TRIGGER_APPEND = 3;

        private final Handler mHandler;

        /**
         * Лента загружена не до конца, продолжаем подгружать
         */
        private boolean mKeepOnAppending;

        /**
         * Подгрузка нотификаций
         */
        private Subscription mAppendSubscription;

        /**
         * Обновление нотификаций
         */
        private Subscription mRefreshSubscription;

        private final ApiMessenger mApiMessenger;

        public NotificationsLoader()  {
            mHandler = new Handler();
            mKeepOnAppending = true;
            mAppendSubscription = Subscriptions.unsubscribed();
            mRefreshSubscription = Subscriptions.unsubscribed();
            mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
        }

        protected Observable<NotificationList> createObservable(Long sinceEntryId, Integer limit) {
            return mApiMessenger.getNotifications(null, null, sinceEntryId, limit, null);
        }

        public void refreshFeed() {
            if (!mRefreshSubscription.isUnsubscribed()) {
                return;
            }
            int entriesRequested = Constants.LIST_FEED_INITIAL_LENGTH;
            Observable<NotificationList> observable = createObservable(null, entriesRequested);
            mRefreshSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LoadObserver(true, entriesRequested));
        }

        public boolean isLoading() {
            return !mAppendSubscription.isUnsubscribed() || !mRefreshSubscription.isUnsubscribed();
        }

        public boolean isRefreshing() {
            return !mRefreshSubscription.isUnsubscribed();
        }

        public void onCreate() {

        }

        public void onDestroy() {
            mAppendSubscription.unsubscribe();
            mRefreshSubscription.unsubscribe();
        }

        private void setKeepOnAppending(boolean newValue) {
            mKeepOnAppending = newValue;
            if (!newValue) mAdapter.setShowPendingIndicator(false);
        }

        private void activateCacheInBackground() {
            if (DBG) Log.v(TAG, "activateCacheInBackground()");
            final Notification lastEntry = mAdapter.getNotifications().getLastEntry();
            if (lastEntry == null) return;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (isLoading()) return;
                    mAppendSubscription.unsubscribe();
                    mAdapter.setShowPendingIndicator(true);
                    int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
                    mAppendSubscription = createObservable(lastEntry.id, requestEntries)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new LoadObserver(false, requestEntries));
                }
            });
        }

        protected void onLoadCompleted(boolean isRefresh, int entriesRequested) {
            if (DBG) Log.v(TAG, "onCompleted()");
            setupMarkAsReadButtonStatus();
            setupLoadingState();
        }

        protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            mAdapter.setShowPendingIndicator(false);
            if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
        }

        protected void onLoadNext(boolean isRefresh, int entriesRequested, NotificationList list) {
            boolean keepOnAppending = (list != null) && (list.notifications.size() >= 0);

            if (list != null) {
                int sizeBefore = mAdapter.getNotifications().size();
                mAdapter.getNotifications().insertItems(list.notifications);
                if (!isRefresh && entriesRequested != 0 && sizeBefore == mAdapter.getNotifications().size())
                    keepOnAppending = false;
            }
            setKeepOnAppending(keepOnAppending);
            mAdapter.setShowPendingIndicator(false);
        }

        private void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position, int feedSize) {
            if (!mKeepOnAppending
                    || feedSize == 0
                    || isLoading()) return;
            if (position >= feedSize - ENTRIES_TO_TRIGGER_APPEND) activateCacheInBackground();
        }

        public class LoadObserver implements Observer<NotificationList> {

            private final boolean mIsRefresh;
            private final int mEntriesRequested;

            public LoadObserver(boolean isRefresh, int entriesRequested) {
                mIsRefresh = isRefresh;
                mEntriesRequested = entriesRequested;
            }

            @Override
            public void onCompleted() {
                NotificationsLoader.this.onLoadCompleted(mIsRefresh, mEntriesRequested);
            }

            @Override
            public void onError(Throwable e) {
                NotificationsLoader.this.onLoadError(mIsRefresh, mEntriesRequested, e);
            }

            @Override
            public void onNext(NotificationList notifications) {
                NotificationsLoader.this.onLoadNext(mIsRefresh, mEntriesRequested, notifications);
            }
        }

    }


        /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onListScrolled(int dy, boolean atTop);
        void onListScrollStateChanged(int state);
    }

}
