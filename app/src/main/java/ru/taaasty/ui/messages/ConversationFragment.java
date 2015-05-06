package ru.taaasty.ui.messages;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.ConversationAdapter;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.events.UpdateMessagesReceived;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.ConversationMessages;
import ru.taaasty.model.Status;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiMessenger;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.ListScrollController;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class ConversationFragment extends Fragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationFragment";

    private static final String ARG_CONVERSATION = "ru.taaasty.ui.messages.ConversationFragment.conversation";

    private static final String ARG_CONVERSATION_ID = "ru.taaasty.ui.messages.ConversationFragment.conversation_id";

    public static final int REFRESH_DATES_DELAY_MILLIS = 20000;

    private OnFragmentInteractionListener mListener;

    private long mConversationId;

    @Nullable
    private Conversation mConversation;

    private RecyclerView mListView;

    private ConversationAdapter mAdapter;
    private MessagesLoader mMessagesLoader;
    private MarkMessagesAsRead markMessagesAsRead;

    private EditText mSendMessageText;
    private View mSendMessageButton;
    private View mSendMessageProgress;
    private View mEmptyView;

    private ListScrollController mListScrollController;

    private Subscription mPostMessageSubscription = SubscriptionHelper.empty();

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LiveFeedFragment.
     */
    public static ConversationFragment newInstance(Conversation conversation) {
        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CONVERSATION, conversation);
        fragment.setArguments(args);
        return fragment;
    }

    public static ConversationFragment newInstance(long conversationId) {
        ConversationFragment fragment = new ConversationFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_CONVERSATION_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mConversation = getArguments().getParcelable(ARG_CONVERSATION);
        if (mConversation != null) {
            mConversationId = mConversation.id;
        } else {
            mConversationId = getArguments().getLong(ARG_CONVERSATION_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_conversation, container, false);

        mSendMessageText = (EditText)v.findViewById(R.id.reply_to_comment_text);
        mSendMessageButton = v.findViewById(R.id.reply_to_comment_button);
        mSendMessageProgress = v.findViewById(R.id.reply_to_comment_progress);
        mEmptyView = v.findViewById(R.id.empty_view);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListScrollController = new ListScrollController(mListView, mListener);
        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        lm.setStackFromEnd(true);
        mListView.setLayoutManager(lm);
        mListView.getItemAnimator().setAddDuration(getResources().getInteger(R.integer.longAnimTime));
        mListView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (mListScrollController != null) mListScrollController.checkScroll();
            }
        });

        mMessagesLoader = new MessagesLoader();
        markMessagesAsRead = new MarkMessagesAsRead();
        mAdapter = new Adapter(getActivity());
        mListView.setAdapter(mAdapter);

        initSendMessageForm();

        EventBus.getDefault().register(this);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mConversation != null) mAdapter.setFeedDesign(mConversation.recipient.getDesign());
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            // List<Conversation.Message> feed = mAdapter.getF();
            // outState.putParcelable(BUNDLE_KEY_FEED_ITEMS, feed);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter.isEmpty()) {
            getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
            refreshMessages();
        }
        mListScrollController.checkScroll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
        mPostMessageSubscription.unsubscribe();
        mMessagesLoader.onDestroy();
        mMessagesLoader = null;
        markMessagesAsRead.onDestroy();
        markMessagesAsRead = null;
        mListView.setOnScrollListener(null);
        mListView = null;
        mAdapter = null;
        mListScrollController = null;
        mEmptyView = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onEventMainThread(MessageChanged event) {
        if ((mAdapter != null) && (event.message.conversationId == mConversationId)) {
            addMessageScrollToEnd(event.message);
        }
    }

    public void onEventMainThread(UpdateMessagesReceived event) {
        if (mAdapter != null && event.updateMessages.conversationId == mConversationId) {
            mAdapter.markMessagesAsRead(event.updateMessages.messages);
        }
    }

    public void onImeKeyboardShown() {
        if (DBG) Log.v(TAG, "onImeKeyboardShown");
        smoothScrollToEnd();
    }

    public void onConversationLoaded(Conversation conversation) {
        if (conversation.id != mConversationId) throw new IllegalArgumentException();
        mConversation = conversation;
        if (mAdapter != null) mAdapter.setFeedDesign(mConversation.recipient.getDesign());
    }

    private void initSendMessageForm() {
        mSendMessageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.send_reply_to_comment) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });
        mSendMessageText.setHint(R.string.your_message);
        mSendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage() {
        String comment = mSendMessageText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(getActivity(), R.string.please_write_something, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }

        mPostMessageSubscription.unsubscribe();

        ApiMessenger apiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);

        Observable<Conversation.Message> observablePost = AppObservable.bindFragment(this,
                apiMessenger.postMessage(null, mConversationId, comment,
                        UUID.randomUUID().toString(), null));

        //mSendMessageText.setEnabled(false);
        mSendMessageProgress.setVisibility(View.VISIBLE);
        mSendMessageButton.setVisibility(View.INVISIBLE);
        mPostMessageSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        mSendMessageText.setEnabled(true);
                        mSendMessageProgress.setVisibility(View.INVISIBLE);
                        mSendMessageButton.setVisibility(View.VISIBLE);
                    }
                })
                .subscribe(mPostMessageObserver);
    }

    private void refreshMessages() {
        int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
        Observable<ConversationMessages> observableFeed = mMessagesLoader.createObservable(null, requestEntries)
                .observeOn(AndroidSchedulers.mainThread());

        mMessagesLoader.refreshFeed(observableFeed, requestEntries);
    }

    private void addMessageScrollToEnd(Conversation.Message message) {
        if (mAdapter == null) return;

        mAdapter.addMessage(message);
        smoothScrollToEnd();
    }

    private void smoothScrollToEnd() {
        scrollListToPosition(mAdapter.getLastPosition(), true);
    }

    private void scrollListToPosition(final int newPosition, final boolean smooth) {
        if (DBG) Log.v(TAG, "scrollListToPosition pos: " + newPosition + " smooth: " + smooth);
        if (mListView == null) return;
        if (mAdapter == null || newPosition < 0) return;
        mListView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (mListView == null) return true;
                if (mListView.getViewTreeObserver().isAlive()) {
                    mListView.getViewTreeObserver().removeOnPreDrawListener(this);

                    LinearLayoutManager layoutManager = (LinearLayoutManager)mListView.getLayoutManager();
                    if (smooth) {
                        mListView.smoothScrollToPosition(newPosition);
                    } else {
                        layoutManager.scrollToPositionWithOffset(newPosition, 0);
                        mListScrollController.checkScrollStateOnViewPreDraw();
                    }
                    return false;
                }
                return true;
            }
        });
    }

    private void addMessagesDoNotScrollList(List<Conversation.Message> messages) {
        Long oldTopId = null;
        int oldTopTop = 0;

        ConversationAdapter.ViewHolderMessage top = findTopVisibleMessageViewHolder();
        if (top != null) {
            oldTopId = mAdapter.getItemId(top.getPosition());
            oldTopTop = top.itemView.getTop();
        }

        mAdapter.addMessages(messages);

        if (oldTopId != null) {
            Integer newPosition = mAdapter.findPositionById(oldTopId);
            if (newPosition != null) {
                LinearLayoutManager lm = (LinearLayoutManager)mListView.getLayoutManager();
                lm.scrollToPositionWithOffset(newPosition, oldTopTop);
                mListScrollController.checkScrollStateOnViewPreDraw();
            }
        }
    }

    @Nullable
    private ConversationAdapter.ViewHolderMessage findTopVisibleMessageViewHolder() {
        if (mListView == null) return null;
        int count = mListView.getChildCount();
        for (int i = 0; i < count; ++i) {
            RecyclerView.ViewHolder holder = mListView.getChildViewHolder(mListView.getChildAt(i));
            if (holder instanceof ConversationAdapter.ViewHolderMessage) return (ConversationAdapter.ViewHolderMessage) holder;
        }
        return null;
    }

    private final Observer<Conversation.Message> mPostMessageObserver = new Observer<Conversation.Message>() {
        @Override
        public void onCompleted() {
            if (mSendMessageText != null) mSendMessageText.setText("");
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_post_comment), e);
        }

        @Override
        public void onNext(Conversation.Message message) {
            addMessageScrollToEnd(message);
        }
    };

    static class LoadMoreButtonHeaderHolder extends RecyclerView.ViewHolder {

        View loadButton;

        View progress;

        public LoadMoreButtonHeaderHolder(View itemView) {
            super(itemView);
            loadButton = itemView.findViewById(R.id.messages_load_more);
            progress = itemView.findViewById(R.id.messages_load_progress);
        }
    }

    class Adapter extends ConversationAdapter {

        private UserManager mUserManager = UserManager.getInstance();

        public Adapter(Context context) {
            super(context);
        }

        @Override
        public void initClickListeners(RecyclerView.ViewHolder holder) {
            if (holder instanceof LoadMoreButtonHeaderHolder) {
                ((LoadMoreButtonHeaderHolder) holder).loadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMessagesLoader != null) mMessagesLoader.activateCacheInBackground();
                    }
                });
            }
        }

        @Override
        protected RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup parent, int viewType) {
            View child;
            switch (viewType) {
                case VIEW_TYPE_HEADER_MORE_BUTTON:
                    child = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversations_load_more_button, mListView, false);
                    return new LoadMoreButtonHeaderHolder(child);
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            super.onBindViewHolder(viewHolder, position);
            if (mMessagesLoader != null) {
                mMessagesLoader.onBindViewHolder(viewHolder, position);
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (holder instanceof ViewHolderMessage
                    && !((ViewHolderMessage) holder).isMyMessage
                    &&  mMessagesLoader != null) {
                Conversation.Message msg = getMessage((ViewHolderMessage)holder);
                if (msg != null && msg.readAt == null) {
                    markMessagesAsRead.enqueueMarkAsRead(msg.id);
                }
            }
        }

        @Override
        protected void bindHeader(RecyclerView.ViewHolder pHolder, int position) {
            if (isLoadMoreIndicatorPosition(position)) {
                mMessagesLoader.bindLoadMoreButton((LoadMoreButtonHeaderHolder) pHolder);
            }
        }

        @Nullable
        @Override
        protected User getMember(long userUuid) {
            if (mUserManager.isMe(userUuid)) {
                return mUserManager.getCachedCurrentUser();
            } else {
                return mConversation == null ? null : mConversation.recipient;
            }
        }
    }

    /**
     * сервис, отмечающий сообщения как прочитанные
     */
    class MarkMessagesAsRead {

        private final Handler mHandler;

        private final Set<Long> mPostIds;

        private final ApiMessenger mApiMessenger;

        private Subscription mPostMessageSubscription = SubscriptionHelper.empty();

        /**
         * Выполняется или запланирована отправка на сервер. Устанавливается дл postDelayed, снимается когда очередь пуста.
         */
        private boolean mDoPost;

        private final int POST_DELAY_MS = 200;

        public MarkMessagesAsRead() {
            mHandler = new Handler();
            mPostIds = new HashSet<>(3);
            mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
        }

        public void enqueueMarkAsRead(long messageId) {
            mPostIds.add(messageId);
            if (mDoPost) return;
            mDoPost = true;
            mHandler.postDelayed(mStartPostRunnable, POST_DELAY_MS);
        }

        public void onDestroy() {
            mPostMessageSubscription.unsubscribe();
            mHandler.removeCallbacksAndMessages(null);
        }

        private final Runnable mStartPostRunnable = new Runnable() {
            @Override
            public void run() {
                final Set<Long> postSet = new HashSet<>(mPostIds);
                mPostIds.clear();

                if (DBG) Log.v(TAG, "markMessagesAsRead " + TextUtils.join(",", postSet));

                Observable<Status.MarkMessagesAsRead> observablePost = AppObservable.bindFragment(ConversationFragment.this,
                        mApiMessenger.markMessagesAsRead(null, mConversationId,
                                TextUtils.join(",", postSet)));
                mPostMessageSubscription = observablePost
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<Status.MarkMessagesAsRead>() {
                            @Override
                            public void onCompleted() {

                            }

                            @Override
                            public void onError(Throwable e) {
                                Log.e(TAG, "mark as read error", e);
                                // Не нервируем юзера
                                mDoPost = false;
                            }

                            @Override
                            public void onNext(Status.MarkMessagesAsRead markMessagesAsRead) {
                                // Ставим левую дату, так как с сервера она не приходит
                                mAdapter.markMessagesAsRead(postSet, new Date());

                                mPostIds.removeAll(postSet); // Удаляем повторно накопившиеся записи
                                if (mPostIds.isEmpty()) {
                                    mDoPost = false;
                                } else {
                                    mHandler.postDelayed(mStartPostRunnable, POST_DELAY_MS);
                                }
                            }
                        });
            }
        };

    }

    /**
     * Подгрузчик сообщений
     */
    class MessagesLoader {
        public static final int ENTRIES_TO_TRIGGER_APPEND = 3;

        private final Handler mHandler;

        private boolean mStartAppending = false;

        /**
         * Лента загружена не до конца, продолжаем подгружать
         */
        private AtomicBoolean mKeepOnAppending;

        /**
         * Подгрузка сообщений
         */
        private Subscription mMessagesAppendSubscription;

        /**
         * Обновление сообщений
         */
        private Subscription mMessagesRefreshSubscription;

        private final ApiMessenger mApiMessenger;

        private boolean mLoadingMessages;

        public MessagesLoader()  {
            mHandler = new Handler();
            mKeepOnAppending = new AtomicBoolean(true);
            mMessagesAppendSubscription = SubscriptionHelper.empty();
            mMessagesRefreshSubscription = SubscriptionHelper.empty();
            mApiMessenger = NetworkUtils.getInstance().createRestAdapter().create(ApiMessenger.class);
        }

        protected Observable<ConversationMessages> createObservable(Long sinceEntryId, Integer limit) {
            return mApiMessenger.getMessages(null, mConversationId, null, sinceEntryId, limit, null);
        }

        public void refreshFeed(Observable<ConversationMessages> observable, int entriesRequested) {
            if (!mMessagesRefreshSubscription.isUnsubscribed()) {
                onFeedIsUnsubscribed(true);
                mMessagesRefreshSubscription.unsubscribe();
            }
            mMessagesRefreshSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new MessagesLoadObserver(true, entriesRequested));
        }

        public void onCreate() {

        }

        public void onDestroy() {
            mMessagesAppendSubscription.unsubscribe();
            mMessagesRefreshSubscription.unsubscribe();
            mHandler.removeCallbacksAndMessages(null);
        }

        private void setKeepOnAppending(boolean newValue) {
            mKeepOnAppending.set(newValue);
            if (mAdapter != null) mAdapter.setShowLoadMoreButton(newValue);
            setLoadingMessages(false);
        }

        private void setLoadingMessages(boolean newValue) {
            mLoadingMessages = newValue;
            mAdapter.notifyDataSetChanged();
        }

        private void activateCacheInBackground() {
            if (DBG) Log.v(TAG, "activateCacheInBackground()");

            if (!mKeepOnAppending.get()
                    || !mStartAppending
                    || mAdapter.isEmpty()
                    || mLoadingMessages) return;

            final Long lastEntryId = mAdapter.getTopMessageId();
            if (lastEntryId == null) return;
            mLoadingMessages = true; // Здесь ставим немного раньше, чтобы не наплодить runnable'ов
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    if (!mMessagesAppendSubscription.isUnsubscribed()) {
                        onFeedIsUnsubscribed(false);
                        mMessagesAppendSubscription.unsubscribe();
                    }

                    int requestEntries = Constants.LIST_FEED_APPEND_LENGTH;
                    mMessagesAppendSubscription = createObservable(lastEntryId, requestEntries)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new MessagesLoadObserver(false, requestEntries));
                }
            });
        }

        protected void onLoadCompleted(boolean isRefresh, int entriesRequested) {
            if (DBG) Log.v(TAG, "onCompleted()");
            if (isRefresh) {
                getView().findViewById(R.id.progress).setVisibility(View.GONE);
                mEmptyView.setVisibility(mAdapter.isEmpty() ? View.VISIBLE : View.GONE);
            }
        }

        protected void onLoadError(boolean isRefresh, int entriesRequested, Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            if (mListener != null) mListener.notifyError(getText(R.string.error_append_feed), e);
        }

        protected void onLoadNext(boolean isRefresh, int entriesRequested, ConversationMessages messages) {
            if (DBG) Log.e(TAG, "onNext " + messages.toString());
            boolean keepOnAppending = (messages != null) && (messages.messages.size() == entriesRequested);
            if (messages != null && !messages.messages.isEmpty()) {
                if (isRefresh) {
                    mAdapter.addMessages(messages.messages);
                    scrollListToPosition(mAdapter.getLastPosition(), false);
                    mStartAppending = true;
                    mListScrollController.checkScrollStateOnViewPreDraw();
                } else {
                    addMessagesDoNotScrollList(messages.messages);
                }
            }

            setKeepOnAppending(keepOnAppending);
            setLoadingMessages(false);
        }

        protected void onFeedIsUnsubscribed(boolean isRefresh) {
            if (DBG) Log.v(TAG, "onFeedIsUnsubscribed()");
        }

        private void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        }

        public void bindLoadMoreButton(LoadMoreButtonHeaderHolder holder) {
            if (!mKeepOnAppending.get()) {
                holder.itemView.setVisibility(View.GONE);
                return;
            }
            holder.loadButton.setVisibility(mLoadingMessages ? View.INVISIBLE : View.VISIBLE);
            holder.progress.setVisibility(mLoadingMessages ? View.VISIBLE : View.INVISIBLE);
        }

        public class MessagesLoadObserver implements Observer<ConversationMessages> {

            private final boolean mIsRefresh;
            private final int mEntriesRequested;

            public MessagesLoadObserver(boolean isRefresh, int entriesRequested) {
                mIsRefresh = isRefresh;
                mEntriesRequested = entriesRequested;
            }

            @Override
            public void onCompleted() {
                MessagesLoader.this.onLoadCompleted(mIsRefresh, mEntriesRequested);
            }

            @Override
            public void onError(Throwable e) {
                MessagesLoader.this.onLoadError(mIsRefresh, mEntriesRequested, e);
            }

            @Override
            public void onNext(ConversationMessages messages) {
                MessagesLoader.this.onLoadNext(mIsRefresh, mEntriesRequested, messages);
            }
        }

    }

    public interface OnFragmentInteractionListener extends CustomErrorView, ListScrollController.OnListScrollPositionListener  {
    }
}
