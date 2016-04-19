package ru.taaasty.ui.messages;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.ui.DividerItemDecoration;
import ru.taaasty.ui.FragmentWithWorkFragment;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.tabbar.TabbarFragment;
import ru.taaasty.utils.FabHelper;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class ConversationsListFragment extends FragmentWithWorkFragment<ConversationsListFragment.WorkRetainedFragment> implements RetainedFragmentCallbacks {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationsListFrag";

    private OnFragmentInteractionListener mListener;

    private RecyclerView mListView;

    private TextView mAdapterEmpty;

    private View mProgressView;

    private ConversationsListAdapter mAdapter;

    private WorkRetainedFragment mWorkFragment;

    public static ConversationsListFragment newInstance() {
        return new ConversationsListFragment();
    }

    public ConversationsListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_conversation_list, container, false);
        mListView = (RecyclerView)root.findViewById(R.id.list);
        mAdapterEmpty = (TextView)root.findViewById(R.id.empty_text);
        mProgressView = root.findViewById(R.id.progress);

        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(lm);
        mListView.addItemDecoration(new DividerItemDecoration(getActivity(), R.drawable.followings_list_divider));
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (mListener != null) {
                    boolean atTop = !mListView.canScrollVertically(-1);
                    mListener.onListScrolled(dy, atTop);
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                if (mListener != null) mListener.onListScrollStateChanged(newState);
            }
        });

        return root;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (DBG) Log.v(TAG, "onAttach " + this);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DBG) Log.v(TAG, this + " onActivityCreated bundle not null: " + (savedInstanceState != null)
                + " listener not null: " + (mListener != null));
        mListView.addOnScrollListener(new FabHelper.AutoHideScrollListener(mListener.getTabbar().getFab()));
    }

    @Override
    public void onWorkFragmentActivityCreatedSafe() {
        mAdapter = new ConversationsListAdapter(mWorkFragment.getConversationList()) {
            public void initClickListeners(final ConversationsListAdapter.ViewHolder holder) {
                holder.itemView.setOnClickListener(v -> {
                    int position = holder.getAdapterPosition();
                    Conversation conversation = getConversation(position);
                    ConversationActivity.startConversationActivity(v.getContext(), conversation, v);
                });
                holder.avatar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getAdapterPosition();
                        Conversation conversation = getConversation(position);
                        if (conversation.getType() == Conversation.Type.PRIVATE) {
                            TlogActivity.startTlogActivity(getActivity(),
                                    ((PrivateConversation)conversation).getRecipientId(), v, R.dimen.avatar_small_diameter);
                        } else {
                            EditCreateGroupActivity.editGroupConversation(getActivity(), conversation, 0);
                        }
                    }
                });
                holder.messageAvatar.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int position = holder.getAdapterPosition();
                        Conversation conversation = getConversation(position);
                        if (conversation == null) return;
                        if (conversation.getLastMessage() != null
                                &&  !(conversation.getType() == Conversation.Type.PUBLIC && ((PublicConversation)conversation).isAnonymous())) {
                            TlogActivity.startTlogActivity(getActivity(),
                                    conversation.getLastMessage().getRealUserId(conversation), v, R.dimen.avatar_small_diameter);
                        }
                    }
                });
            }
        };
        mListView.setAdapter(mAdapter);
        setupLoadingState();
    }

    @Override
    public void onWorkFragmentResumeSafe() {
        mWorkFragment.refreshConversationList();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DBG) Log.v(TAG, this + "onDetach");
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mWorkFragment.setTargetFragment(null, 0);
        mWorkFragment = null;
        mListView = null;
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
        notifyError(e, fallbackResId);
    }

    private void notifyError(Throwable e, int fallbackResId) {
        if (mListener != null) mListener.notifyError(ConversationsListFragment.this, e, fallbackResId);
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
                    return Conversation.SORT_BY_LAST_MESSAGE_CREATED_AT_DESC_COMPARATOR.compare(o1, o2);
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
                    .subscribe(mConversationListObserver);
            if (getTargetFragment() != null) ((ConversationsListFragment)getTargetFragment()).setStatusLoading();
        }

        public void onEventMainThread(ConversationChanged event) {
            if (event.conversation.getMessagesCount() <= 0) return;
            mConversationList.addOrUpdate(event.conversation);
        }

        @Nullable
        private RecyclerView.Adapter getTargetAdapter() {
            if (getTargetFragment() != null) {
                return ((ConversationsListFragment) getTargetFragment()).mAdapter;
            } else {
                return null;
            }
        }

        private final Observer<List<Conversation>> mConversationListObserver = new  Observer<List<Conversation>>() {

            @Override
            public void onCompleted() {
                if (getTargetFragment() != null) ((ConversationsListFragment)getTargetFragment()).setStatusReady();
            }

            @Override
            public void onError(Throwable e) {
                if (DBG) Log.v(TAG, "onError");
                if (getTargetFragment() != null) ((ConversationsListFragment)getTargetFragment())
                        .setStatusFailure(e, R.string.error_loading_conversations);
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
        TabbarFragment getTabbar();
    }

}
