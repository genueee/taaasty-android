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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.adapters.ConversationsListAdapter;
import ru.taaasty.events.ConversationChanged;
import ru.taaasty.model.Conversation;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

public class ConversationsListFragment extends Fragment implements ServiceConnection {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationsListFragment";

    private OnFragmentInteractionListener mListener;

    private RecyclerView mListView;

    private TextView mAdapterEmpty;

    private View mProgressView;

    PusherService mPusherService;

    boolean mBound = false;

    private ConversationsListAdapter mAdapter;

    private ApiMessenger mApiMessenger;

    private Subscription mConversationsSubscription = SubscriptionHelper.empty();

    public static ConversationsListFragment newInstance() {
        return new ConversationsListFragment();
    }

    public ConversationsListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conversations, container, false);
        mListView = (RecyclerView)root.findViewById(R.id.list);
        mAdapterEmpty = (TextView)root.findViewById(R.id.empty_text);
        mProgressView = root.findViewById(R.id.progress);

        mAdapter = new ConversationsListAdapter() {
            public void initClickListeners(final ConversationsListAdapter.ViewHolder holder) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getPosition();
                        Conversation conversation = getConversation(position);
                        ConversationActivity.startConversationActivity(v.getContext(), conversation, v);
                    }
                });
                holder.avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getPosition();
                        Conversation conversation = getConversation(position);
                        new UserInfoActivity.Builder(getActivity())
                                .set(conversation.recipient, v, conversation.recipient.getDesign())
                                .setPreloadAvatarThumbnail(R.dimen.avatar_small_diameter)
                                .startActivity();
                    }
                });
            }
        };
        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.followings_list_divider));
        mListView.setAdapter(mAdapter);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));

        mAdapter.onStart();

        root.findViewById(R.id.initiate_conversation).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onInitiateConversationClicked(v);
            }
        });

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
        getActivity().bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshConversationList();
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.unregisterAdapterDataObserver(mDataObserver);
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
        mConversationsSubscription.unsubscribe();
        mAdapter.onStop();
        mListView = null;
        mListener = null;
        mProgressView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        PusherService.LocalBinder binder = (PusherService.LocalBinder) service;
        mPusherService = binder.getService();
        mBound = true;
        refreshConversationList();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }

    public void onEventMainThread(ConversationChanged event) {
        if (mAdapter != null) mAdapter.addConversation(event.conversation);
    }

    private void setNewStatus(PusherService.NotificationsStatus status) {
        switch (status.code) {
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_LOADING:
                mConversationsSubscription.unsubscribe();
                setStatusLoading();
                break;
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_READY:
                mConversationsSubscription.unsubscribe();
                setStatusReady();
                refreshConversationList();
                break;
            case PusherService.UPDATE_NOTIFICATIONS_STATUS_FAILURE:
                mConversationsSubscription.unsubscribe();
                setStatusFailure(status.errorMessage);
                break;
        }
    }

    private boolean isRefreshIndicatorShown() {
        return mProgressView.getVisibility() == View.VISIBLE;
    }

    private boolean isRefreshing() {
        return !mConversationsSubscription.isUnsubscribed();
    }

    private void setStatusLoading() {
        mAdapterEmpty.setVisibility(View.INVISIBLE);
        // Не играемся с видимостью ListView - раздражает. При обновлении и спиннер не показываем
        if (mAdapter != null && !mAdapter.isEmpty()) {
            mProgressView.setVisibility(View.INVISIBLE);
        } else {
            mProgressView.setVisibility(View.VISIBLE);
        }
    }

    private void setStatusReady() {
        if (mAdapter == null || mAdapter.isEmpty()) {
            mAdapterEmpty.setVisibility(View.VISIBLE);
        } else {
            mAdapterEmpty.setVisibility(View.INVISIBLE);
        }

        mProgressView.setVisibility(View.INVISIBLE);
    }

    private void setStatusFailure(String error) {
        mAdapterEmpty.setVisibility(View.INVISIBLE);
        mProgressView.setVisibility(View.INVISIBLE);
        if (mListener != null) mListener.notifyError(error, null);
    }

    public void refreshConversationList() {
        if (isRefreshing() || !mBound) {
            if (DBG) Log.v(TAG, "refreshConversationList failed not started. refreshing: " + isRefreshing() + " bound: " + mBound);
            return;
        }
        if (DBG) Log.v(TAG, "refreshConversationList");

        mConversationsSubscription.unsubscribe();
        mConversationsSubscription = AndroidObservable.bindFragment(this, mApiMessenger.getConversations(mPusherService.getSocketId()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mConversationListObserver);
        setStatusLoading();
    }

    private final Observer<List<Conversation>> mConversationListObserver = new  Observer<List<Conversation>>() {

        @Override
        public void onCompleted() {
            setStatusReady();
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.v(TAG, "onError");
            setStatusFailure(getString(R.string.error_loading_conversations));
        }

        @Override
        public void onNext(List<Conversation> conversations) {
            if (DBG) Log.v(TAG, "onNext");
            if (mAdapter != null) mAdapter.setConversations(conversations);
        }
    };

    private final RecyclerView.AdapterDataObserver mDataObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            if (mBound) setNewStatus(mPusherService.getNotificationsStatus());
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
        public void onInitiateConversationClicked(View view);
    }

}
