package ru.taaasty.ui.messages;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.IntentService;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.adapters.NotificationListAdapter;
import ru.taaasty.adapters.list.NotificationsListManaged;
import ru.taaasty.events.MarkAllAsReadRequestCompleted;
import ru.taaasty.events.MessagingStatusReceived;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.NotificationList;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.feeds.TlogActivity;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class NotificationListFragment extends Fragment implements ServiceConnection, RetainedFragmentCallbacks {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationListFrgmnt";

    private OnFragmentInteractionListener mListener;

    private RecyclerView mListView;

    private TextView mAdapterEmpty;

    private View mProgressView;

    private View mMarkAsReadButton;

    PusherService mPusherService;

    boolean mBound = false;

    private NotificationListAdapter mAdapter;

    private boolean mWaitingMessagingStatus;

    private Subscription mUserInfoSubscription = Subscriptions.unsubscribed();

    private Subscription mFollowSubscription = Subscriptions.unsubscribed();

    private WorkRetainedFragment mWorkFragment;

    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }

    public NotificationListFragment() {
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

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.followings_list_divider));
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mListener == null) return;
                boolean atTop = !mListView.canScrollVertically(-1);
                mListener.onListScrolled(dy, atTop);
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mListener == null) return;
                mListener.onListScrollStateChanged(newState);
            }
        });
        mWaitingMessagingStatus = false;

        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("NotificationListWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "NotificationListWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
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

    public void onWorkFragmentActivityCreated() {
        if (DBG) Log.v(TAG, "onWorkFragmentActivityCreated");
        mAdapter = new NotificationListAdapter(getActivity(), mInteractionListener, mWorkFragment.getNotificationList()) {
            @Override
            public void onBindViewHolder(ViewHolder viewHolder, int position) {
                super.onBindViewHolder(viewHolder, position);
                if (viewHolder instanceof ViewHolderItem) mWorkFragment.onBindViewHolder(viewHolder, position);
            }
        };
        mListView.setAdapter(mAdapter);

        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                scrollShowTopPosition();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                scrollShowTopPosition();
            }
        });
    }

    public void onWorkFragmentResume() {
        if (DBG) Log.v(TAG, "onWorkFragmentResume");
        mWorkFragment.refreshFeed();
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
        mWorkFragment.setTargetFragment(null, 0);
        mAdapter = null;
        mListView.removeOnScrollListener(null);
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

    /**
     * Если верхний элемент показан частично - скроллим, чтобы он был виден полностью
     */
    private void scrollShowTopPosition() {
        if (mListView == null) return;
        View top = mListView.getChildAt(0);
        boolean listAtTop = top == null || (mListView.getChildAdapterPosition(top) == 0);
        if (listAtTop) mListView.smoothScrollToPosition(0);
    }

    public void setupLoadingState() {
        if (DBG) Log.v(TAG, "setupLoadingState");
        if (mProgressView == null || mWorkFragment == null) return;
        boolean refreshing = mWorkFragment.isRefreshing();
        boolean listEmpty = mWorkFragment.getNotificationList().isEmpty();

        boolean showNoItemsIndicator = !refreshing && listEmpty;
        mAdapterEmpty.setVisibility(showNoItemsIndicator ? View.VISIBLE : View.GONE);
        mProgressView.setVisibility(refreshing && listEmpty ? View.VISIBLE : View.INVISIBLE);
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
        ApiRelationships relApi = RestClient.getAPiRelationships();
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                relApi.follow(String.valueOf(notification.sender.getId())));
        mFollowSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FollowerObserver(notification.id));
    }

    void unfollow(Notification notification) {
        mFollowSubscription.unsubscribe();
        ApiRelationships relApi = RestClient.getAPiRelationships();
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

    private final NotificationListAdapter.InteractionListener mInteractionListener = new NotificationListAdapter.InteractionListener() {

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onNotificationClicked(View v, Notification notification) {
            markNotificationRead(notification);
            Intent intent = notification.getOpenNotificationActivityIntent(getActivity());
            if (intent != null) {
                Bundle options = ActivityOptionsCompat.makeScaleUpAnimation(
                        v, 0, 0, v.getWidth(), v.getHeight()).toBundle();
                if ((Build.VERSION.SDK_INT >= 16) && (options != null)) {
                    startActivity(intent, options);
                } else {
                    startActivity(intent);
                }
            }
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

    public static class WorkRetainedFragment extends Fragment {

        private static final String BUNDLE_KEY_NOTIFICATION_LIST = "ru.taaasty.ui.messages.NotificationListFragment.WorkRetainedFragment.BUNDLE_KEY_NOTIFICATION_LIST";

        private NotificationsListManaged mNotificationList;

        private NotificationListLoader mLoader;

        private CustomErrorView mListener;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            try {
                mListener = (CustomErrorView) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement CustomErrorView");
            }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);

            mLoader = new NotificationListLoader();
            mNotificationList = new NotificationsListManaged(new NotificationsListManaged.Callback() {
                @Override
                public void onInserted(int position, int count) {
                    RecyclerView.Adapter adapter = getTargetAdapter();
                    if (adapter != null) adapter.notifyItemRangeInserted(position, count);
                }

                @Override
                public void onRemoved(int position, int count) {
                    RecyclerView.Adapter adapter = getTargetAdapter();
                    if (adapter != null) adapter.notifyItemRangeRemoved(position, count);
                }

                @Override
                public void onMoved(int fromPosition, int toPosition) {
                    RecyclerView.Adapter adapter = getTargetAdapter();
                    if (adapter != null) adapter.notifyItemMoved(fromPosition, toPosition);
                }

                @Override
                public void onChanged(int position, int count) {
                    RecyclerView.Adapter adapter = getTargetAdapter();
                    if (adapter != null) adapter.notifyItemRangeChanged(position, count);
                }
            });

            if (savedInstanceState != null) {
                ArrayList<Notification> notifications = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_NOTIFICATION_LIST);
                if (notifications != null) mNotificationList.resetItems(notifications);
            }

            mNotificationList.onCreate();
            mLoader.onCreate();
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (!mNotificationList.isEmpty()) {
                outState.putParcelableArrayList(BUNDLE_KEY_NOTIFICATION_LIST,
                        new ArrayList<Notification>(mNotificationList.getItems()));
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            ((RetainedFragmentCallbacks)getTargetFragment()).onWorkFragmentActivityCreated();
        }

        @Override
        public void onResume() {
            super.onResume();
            ((RetainedFragmentCallbacks)getTargetFragment()).onWorkFragmentResume();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mNotificationList.onDestroy();
            mLoader.onDestroy();
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mListener = null;
        }

        void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int feedLocation) {
            mLoader.onBindViewHolder(viewHolder, feedLocation, mNotificationList.size());
        }

        public NotificationsListManaged getNotificationList() {
            return mNotificationList;
        }

        public void refreshFeed() {
            mLoader.refreshFeed();
        }

        public boolean isRefreshing() {
            return mLoader.isRefreshing();
        }

        @Nullable
        private NotificationListAdapter getTargetAdapter() {
            if (getTargetFragment() != null) {
                return ((NotificationListFragment) getTargetFragment()).mAdapter;
            } else {
                return null;
            }
        }

        void onNewListPendingIndicatorStatus(boolean isShown) {
            NotificationListAdapter adapter = getTargetAdapter();
            if (adapter != null) adapter.setShowPendingIndicator(isShown);
        }

        /**
         * Подгрузчик нотификаций
         */
        class NotificationListLoader {
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

            public NotificationListLoader()  {
                mHandler = new Handler();
                mKeepOnAppending = true;
                mAppendSubscription = Subscriptions.unsubscribed();
                mRefreshSubscription = Subscriptions.unsubscribed();
                mApiMessenger = RestClient.getAPiMessenger();
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
                if (!newValue) onNewListPendingIndicatorStatus(false);
            }

            private void activateCacheInBackground() {
                if (DBG) Log.v(TAG, "activateCacheInBackground()");
                final Notification lastEntry = mNotificationList.getLastEntry();
                if (lastEntry == null) return;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isLoading()) return;
                        mAppendSubscription.unsubscribe();
                        onNewListPendingIndicatorStatus(true);
                        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
                        mAppendSubscription = createObservable(lastEntry.id, requestEntries)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new LoadObserver(false, requestEntries));
                    }
                });
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
                    if (DBG) Log.v(TAG, "onCompleted()");
                    NotificationListFragment target = (NotificationListFragment)getTargetFragment();
                    if (target != null) {
                        target.setupMarkAsReadButtonStatus();
                        target.setupLoadingState();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    if (DBG) Log.e(TAG, "onError", e);
                    onNewListPendingIndicatorStatus(false);
                    if (mListener != null)
                        mListener.notifyError(getText(R.string.error_append_feed), e);
                }

                @Override
                public void onNext(NotificationList list) {
                    boolean keepOnAppending = (list != null) && (list.notifications.size() >= 0);

                    if (list != null) {
                        int sizeBefore = mNotificationList.size();
                        mNotificationList.addOrUpdateItems(list.notifications);
                        if (!mIsRefresh && mEntriesRequested != 0 && sizeBefore == mNotificationList.size())
                            keepOnAppending = false;
                        if (!mNotificationList.isEmpty()) {
                            StatusBarNotifications.getInstance().onNewNotificationIdSeen(mNotificationList.get(0).id);
                        }
                    }
                    setKeepOnAppending(keepOnAppending);
                    onNewListPendingIndicatorStatus(false);
                }
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
