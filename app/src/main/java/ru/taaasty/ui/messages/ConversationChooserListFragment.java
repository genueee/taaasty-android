package ru.taaasty.ui.messages;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.RetainedFragmentCallbacks;
import ru.taaasty.SortedList;
import ru.taaasty.adapters.ConversationsListAdapter;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.FragmentWithWorkFragment;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.Subscriptions;

public class ConversationChooserListFragment extends FragmentWithWorkFragment<ConversationChooserListFragment.WorkRetainedFragment> implements RetainedFragmentCallbacks {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationsListFrag";
    private Subject<Long,Long> resultConversationIdSubject = PublishSubject.create();


    private RecyclerView mRecyclerView;

    private TextView mAdapterEmpty;

    private View mProgressView;

    private ConversationsListAdapter mAdapter;

    private WorkRetainedFragment mWorkFragment;

    public static ConversationsListFragment newInstance() {
        return new ConversationsListFragment();
    }

    public ConversationChooserListFragment() {
    }

    public Observable<Long> getResultConversationIdObservable() {
        return resultConversationIdSubject;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conversation_list, container, false);
        mRecyclerView = (RecyclerView)root.findViewById(R.id.list);
        mAdapterEmpty = (TextView)root.findViewById(R.id.empty_text);
        mProgressView = root.findViewById(R.id.progress);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.conversation_list_divider));
        mRecyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(mRecyclerView,conversationId->{
            resultConversationIdSubject.onNext(conversationId);
        }));


        return root;
    }

    public static class RecyclerViewItemClickListener implements RecyclerView.OnItemTouchListener{
        private OnClickListener onClickListener;
        private RecyclerView recyclerView;
        private GestureDetector gestureDetector;

        public interface OnClickListener{
            void onClick(long itemId);
        }

        public RecyclerViewItemClickListener(RecyclerView recyclerView, OnClickListener onClickListener) {
            this.onClickListener = onClickListener;
            this.recyclerView = recyclerView;
            gestureDetector = new GestureDetector(recyclerView.getContext(), new GestureDetectorListener());

        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            return gestureDetector.onTouchEvent(e);
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
        private class GestureDetectorListener extends GestureDetector.SimpleOnGestureListener {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                if (childView==null) return false;
                RecyclerView.ViewHolder childViewHolder = recyclerView.getChildViewHolder(childView);
                onClickListener.onClick(childViewHolder.getItemId());
                return true;
            }
        }

    }




    @Nullable
    @Override
    public WorkRetainedFragment getWorkFragment() {
        return mWorkFragment;
    }

    @Override
    public void initWorkFragment() {
        FragmentManager fm = getFragmentManager();
        mWorkFragment = (WorkRetainedFragment) fm.findFragmentByTag("ConversationListWorkFragment");
        if (mWorkFragment == null) {
            mWorkFragment = new WorkRetainedFragment();
            mWorkFragment.setTargetFragment(this, 0);
            fm.beginTransaction().add(mWorkFragment, "ConversationListWorkFragment").commit();
        } else {
            mWorkFragment.setTargetFragment(this, 0);
        }
    }


    @Override
    public void onWorkFragmentActivityCreatedSafe() {
        mAdapter = new ConversationsListAdapter(mWorkFragment.getConversationList());
        mRecyclerView.setAdapter(mAdapter);
        setupLoadingState();
    }

    @Override
    public void onWorkFragmentResumeSafe() {
        mWorkFragment.refreshConversationList();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWorkFragment.setTargetFragment(null, 0);
        mWorkFragment = null;
        mRecyclerView = null;
        mProgressView = null;
        mAdapter = null;
    }

    private void setupLoadingState() {
        if (mWorkFragment.isRefreshing()) {
            setStatusLoading();
        } else {
            setStatusReady();
        }
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

    private void setStatusFailure(Throwable e, int fallbackResId) {
        mAdapterEmpty.setVisibility(View.INVISIBLE);
        mProgressView.setVisibility(View.INVISIBLE);
    }



    public static class WorkRetainedFragment extends Fragment {

        private static final String BUNDLE_KEY_SUBSCRIPTION_LIST = "ru.taaasty.ui.messages.ConversationsListFragment.WorkRetainedFragment.BUNDLE_KEY_SUBSCRIPTION_LIST";

        private ApiMessenger mApiMessenger;

        private Subscription mConversationsSubscription = Subscriptions.unsubscribed();

        private SortedList<Conversation> mConversationList;

        private boolean mIsInitialized;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
            mApiMessenger = RestClient.getAPiMessenger();
            mConversationList = new SortedList<>(Conversation.class, new android.support.v7.util.SortedList.Callback<Conversation>() {
                @Override
                public int compare(Conversation o1, Conversation o2) {
                    return Conversation.SORT_BY_LAST_MESSAGE_UPDATED_AT_DESC_COMPARATOR.compare(o1, o2);
                }

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

                @Override
                public boolean areContentsTheSame(Conversation oldItem, Conversation newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areItemsTheSame(Conversation item1, Conversation item2) {
                    return item1.getId() == item2.getId();
                }

            });
            if (savedInstanceState != null) {
                ArrayList<Conversation> list = savedInstanceState.getParcelableArrayList(BUNDLE_KEY_SUBSCRIPTION_LIST);
                if (list != null) mConversationList.resetItems(list);
            }

            EventBus.getDefault().register(this);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            if (!mConversationList.isEmpty()) {
                outState.putParcelableArrayList(BUNDLE_KEY_SUBSCRIPTION_LIST,
                        new ArrayList<Parcelable>(mConversationList.getItems()));
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (getTargetFragment() != null) {
                ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentActivityCreated();
            } else {
                if (DBG) Log.i(TAG, "onActivityCreated target fragment is null");
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            if (getTargetFragment() != null) {
                ((RetainedFragmentCallbacks) getTargetFragment()).onWorkFragmentResume();
            } else {
                if (DBG) Log.i(TAG, "onResume target fragment is null");
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mConversationsSubscription.unsubscribe();
            EventBus.getDefault().unregister(this);
        }

        public SortedList<Conversation> getConversationList() {
            return mConversationList;
        }

        public boolean isRefreshing() {
            return !mConversationsSubscription.isUnsubscribed();
        }

        public void refreshConversationList() {
            if (isRefreshing()) {
                if (DBG) Log.v(TAG, "refreshConversationList failed not started. refreshing: " + isRefreshing());
                return;
            }
            if (mApiMessenger == null || mConversationsSubscription == null) {
                // Бывает при вызове из onServiceConnected, непонятно как
                return;
            }
            if (DBG) Log.v(TAG, "refreshConversationList");

            mConversationsSubscription.unsubscribe();
            mConversationsSubscription = mApiMessenger.getConversations(null)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(RestSchedulerHelper.getScheduler())
                    .subscribe(mConversationListObserver);
            if (getTargetFragment() != null) ((ConversationChooserListFragment)getTargetFragment()).setStatusLoading();
        }

        public void onEventMainThread(ConversationChanged event) {
            if (event.conversation.getMessagesCount() <= 0) return;
            if (event.conversation.isDisabled()) {
                mConversationList.remove(event.conversation);
            } else {
                mConversationList.addOrUpdate(event.conversation);
            }
        }

        @Nullable
        private RecyclerView.Adapter getTargetAdapter() {
            if (getTargetFragment() != null) {
                return ((ConversationChooserListFragment) getTargetFragment()).mAdapter;
            } else {
                return null;
            }
        }

        private final Observer<List<Conversation>> mConversationListObserver = new  Observer<List<Conversation>>() {

            @Override
            public void onCompleted() {
                if (getTargetFragment() != null) ((ConversationChooserListFragment)getTargetFragment()).setStatusReady();
            }

            @Override
            public void onError(Throwable e) {
                if (DBG) Log.v(TAG, "onError");

            }

            @Override
            public void onNext(List<Conversation> conversations) {
                if (DBG) Log.v(TAG, "onNext");
                List<Conversation> ge0 = new ArrayList<>(conversations.size());
                for (Conversation c : conversations) if (c.getMessagesCount() > 0) ge0.add(c);
                mConversationList.resetItems(ge0);
            }
        };

    }
}
