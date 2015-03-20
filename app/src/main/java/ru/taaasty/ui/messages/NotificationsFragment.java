package ru.taaasty.ui.messages;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.adapters.NotificationsAdapter;
import ru.taaasty.events.MarkAsReadRequestCompleted;
import ru.taaasty.events.MessagingStatusReceived;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.RelationshipChanged;
import ru.taaasty.model.Notification;
import ru.taaasty.model.Relationship;
import ru.taaasty.service.ApiRelationships;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;

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

    private Subscription mUserInfoSubscription = SubscriptionHelper.empty();

    private Subscription mFollowSubscribtion = SubscriptionHelper.empty();

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

        mAdapter = new NotificationsAdapter(getActivity(), mInteractionListener);
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
        mAdapter.registerAdapterDataObserver(mDataObserver);
        mAdapter.onStart();
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.unregisterAdapterDataObserver(mDataObserver);
        mAdapter.onStop();
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
        mListView.setOnScrollListener(null);
        mListView = null;
        mListener = null;
        mProgressView = null;
        mMarkAsReadButton = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFollowSubscribtion.unsubscribe();
        mUserInfoSubscription.unsubscribe();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PusherService.LocalBinder binder = (PusherService.LocalBinder) service;
        mPusherService = binder.getService();
        mBound = true;
        if (mAdapter != null) {
            mAdapter.setNotifications(mPusherService.getNotifications());
            setupMarkAsReadButtonStatus();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }

    public void onEventMainThread(ru.taaasty.events.NotificationsStatus status) {
        if (status.newStatus.code == PusherService.UPDATE_NOTIFICATIONS_STATUS_READY && (mAdapter != null) && mBound) {
            mAdapter.setNotifications(mPusherService.getNotifications());
        }
        setupPusherStatus(status.newStatus);
        setupMarkAsReadButtonStatus();
    }

    public void onEventMainThread(MessagingStatusReceived status) {
        mWaitingMessagingStatus = false;
        setupMarkAsReadButtonStatus();
    }

    public void onEventMainThread(MarkAsReadRequestCompleted status) {
        setupMarkAsReadButtonStatus();
    }

    public void onEventMainThread(NotificationReceived event) {
        if (mAdapter == null ) return;
        View top = mListView.getChildAt(0);
        boolean listAtTop = top == null || (mListView.getChildPosition(top) == 0);
        mAdapter.addNotification(event.notification);
        if (listAtTop) mListView.smoothScrollToPosition(0);
    }

    private void setupPusherStatus(PusherService.NotificationsStatus status) {
        switch (status.code) {
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_LOADING:
                mAdapterEmpty.setVisibility(View.INVISIBLE);
                // Не играемся с видимостью ListView - раздражает. При обновлении и спиннер не показываем
                if (mAdapter != null && !mAdapter.isEmpty()) {
                    mProgressView.setVisibility(View.INVISIBLE);
                } else {
                    mProgressView.setVisibility(View.VISIBLE);
                }
                break;
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_READY:
                if (mAdapter == null || mAdapter.isEmpty()) {
                    mAdapterEmpty.setVisibility(View.VISIBLE);
                } else {
                    mAdapterEmpty.setVisibility(View.INVISIBLE);
                }
                mProgressView.setVisibility(View.INVISIBLE);
                break;
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_FAILURE:
                mAdapterEmpty.setVisibility(View.INVISIBLE);
                mProgressView.setVisibility(View.INVISIBLE);
                if (mListener != null) mListener.notifyError(status.errorMessage, null);
                break;
        }
    }

    void setupMarkAsReadButtonStatus() {
        if (mMarkAsReadButton == null) return;

        boolean isVisible = !mWaitingMessagingStatus && mBound && mPusherService.hasUnreadMessages();
        mMarkAsReadButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    void follow(Notification notification) {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                relApi.follow(String.valueOf(notification.sender.getId())));
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FollowerObserver(notification.id));
    }

    void unfollow(Notification notification) {
        mFollowSubscribtion.unsubscribe();
        ApiRelationships relApi = NetworkUtils.getInstance().createRestAdapter().create(ApiRelationships.class);
        Observable<Relationship> observable = AppObservable.bindFragment(this,
                relApi.unfollow(String.valueOf(notification.sender.getId())));
        mFollowSubscribtion = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new FollowerObserver(notification.id));
    }

    void markAsReadClicked() {
        if (mMarkAsReadButton == null) return;
        PusherService.markAllNotificationsAsRead(getActivity());
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

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (mBound) setupPusherStatus(mPusherService.getNotificationsStatus());
        }
    };

    private void markNotificationRead(Notification notification) {
        if (notification.isMarkedAsRead()) return;
        PusherService.markNotificationAsRead(getActivity(), notification.id);
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
        public void onListScrolled(int dy, boolean atTop);
        public void onListScrollStateChanged(int state);
    }

}
