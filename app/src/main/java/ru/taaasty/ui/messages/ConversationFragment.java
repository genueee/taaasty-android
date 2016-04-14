package ru.taaasty.ui.messages;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import retrofit.mime.TypedOutput;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.adapters.ConversationAdapter;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.events.UpdateMessagesReceived;
import ru.taaasty.rest.ContentTypedOutput;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Status;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.MessageList;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.PhotoSourceManager;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.ImeUtils;
import ru.taaasty.utils.ListScrollController;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.SafeOnPreDrawListener;
import ru.taaasty.widgets.DefaultUserpicDrawable;
import ru.taaasty.widgets.ExtendedImageView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.Subscriptions;

public class ConversationFragment extends Fragment implements SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationFragment";

    private static final String ARG_CONVERSATION = "ru.taaasty.ui.messages.ConversationFragment.conversation";
    private static final String ARG_FORCE_SHOW_KEYBOARD = "ru.taaasty.ui.messages.ConversationFragment.ARG_FORCE_SHOW_KEYBOARD";

    private static final int REQUEST_CODE_LOGIN = 1;

    private OnFragmentInteractionListener mListener;

    private BehaviorSubject<Conversation> mConversationSubject;

    private RecyclerView mListView;

    private ConversationAdapter mAdapter;
    private MessagesLoader mMessagesLoader;
    private MarkMessagesAsRead markMessagesAsRead;

    private EditText mSendMessageText;
    private View mSendMessageButton;
    private View mSendMessageProgress;
    private View mEmptyView;
    private MenuItem mAttachFileMenuItem;

    private ListScrollController mListScrollController;

    private Subscription mPostMessageSubscription = Subscriptions.unsubscribed();

    private Toolbar mToolbar;

    private PhotoSourceManager mPhotoSourceManager;

    public static class Builder {

        private Conversation mConversation;

        private boolean mForceShowKeyboard;

        public Builder() {
        }

        public Builder setConversation(Conversation conversation) {
            mConversation = conversation;
            return this;
        }

        public Builder setForceShowKeyboard(boolean force) {
            mForceShowKeyboard = force;
            return this;
        }

        public ConversationFragment build() {
            ConversationFragment fragment = new ConversationFragment();
            Bundle args = new Bundle();
            if (mConversation != null) args.putParcelable(ARG_CONVERSATION, mConversation);
            if (mForceShowKeyboard) args.putBoolean(ARG_FORCE_SHOW_KEYBOARD, true);
            fragment.setArguments(args);
            return fragment;
        }
    }


    public ConversationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Conversation conversation;
        if (getArguments() != null) {
            conversation = getArguments().getParcelable(ARG_CONVERSATION);
        } else {
            conversation = null;
        }

        if (conversation == null) {
            mConversationSubject = BehaviorSubject.create();
        } else {
            mConversationSubject = BehaviorSubject.create(conversation);
        }

        mMessagesLoader = new MessagesLoader();
        setHasOptionsMenu(true);
        mPhotoSourceManager = new PhotoSourceManager(this, "CreateFlowFragment", this::sendAttachment);
        mPhotoSourceManager.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_conversation, container, false);

        mSendMessageText = (EditText) v.findViewById(R.id.reply_to_comment_text);
        mSendMessageButton = v.findViewById(R.id.reply_to_comment_button);
        mSendMessageProgress = v.findViewById(R.id.reply_to_comment_progress);
        mEmptyView = v.findViewById(R.id.empty_view);
        mToolbar = (Toolbar)v.findViewById(R.id.toolbar);

        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListScrollController = new ListScrollController(mListView, mListener);
        LinearLayoutManager lm = new LinearLayoutManager(getActivity());
        lm.setStackFromEnd(true);
        mListView.setLayoutManager(lm);
        mListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView view, int dx, int dy) {
                if (mListScrollController != null) mListScrollController.checkScroll();
            }
        });

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
        ((AppCompatActivity)getActivity()).setSupportActionBar(mToolbar);
        mConversationSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Conversation>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(Conversation conversation) {
                        if (mAdapter != null) mAdapter.setFeedDesign(conversation.recipient.getDesign());
                        bindToolbar();
                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DBG) Log.v(TAG, "onActivityResult()");
        mPhotoSourceManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPhotoSourceManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter.isEmpty()) {
            getView().findViewById(R.id.progress).setVisibility(View.VISIBLE);
            mMessagesLoader.refreshMessages();
        }
        mListScrollController.checkScroll();

        if (getArguments() != null && getArguments().getBoolean(ARG_FORCE_SHOW_KEYBOARD, false)) {
            mSendMessageText.post(() -> ImeUtils.showIme(mSendMessageText));
        }
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPhotoSourceManager.onSaveInstanceState(outState);
        if (mAdapter != null) {
            // List<Conversation.Message> feed = mAdapter.getF();
            // outState.putParcelable(BUNDLE_KEY_FEED_ITEMS, feed);
        }
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        mPhotoSourceManager.startPickPhoto();
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        mPhotoSourceManager.startMakePhoto();
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
    }

    public void refresh() {
        mMessagesLoader.refreshMessages();
        bindToolbar();
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
        mToolbar = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mConversationSubject.onCompleted();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_conversation, menu);
        mAttachFileMenuItem = menu.findItem(R.id.attach_image);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        mAttachFileMenuItem = menu.findItem(R.id.attach_image);

        // TODO
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.attach_image:
                showAttachImageMenu(null);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    public void onEventMainThread(MessageChanged event) {
        Conversation conversation = mConversationSubject.getValue();
        if ((mAdapter != null)
                && (conversation != null)
                && (event.message.conversationId == conversation.id)) {
            addMessageScrollToEnd(event.message);
        }
    }

    public void onEventMainThread(UpdateMessagesReceived event) {
        Conversation conversation = mConversationSubject.getValue();
        if (mAdapter != null
                && (conversation != null)
                && (event.updateMessages.conversationId == conversation.id)) {
            mAdapter.markMessagesAsRead(event.updateMessages.messages);
        }
    }

    public void onImeKeyboardShown() {
        if (DBG) Log.v(TAG, "onImeKeyboardShown");
        smoothScrollToEnd();
    }

    public void onConversationLoaded(Conversation conversation) {
        mConversationSubject.onNext(conversation);
        mMessagesLoader.refreshMessages();
    }

    public void bindToolbar() {
        Conversation conversation = mConversationSubject.getValue();
        if (conversation == null || getActivity() == null || mListView == null) return;
        View headerGroupChat = mToolbar.findViewById(R.id.header_group_info);

        Context context = getActivity();

        // У групп без аватарки пустая рамка на зеленом экшнбаре выглядит хреново
        Drawable defaultGroupDrawable = new DefaultUserpicDrawable(context,
                ConversationHelper.getInstance().getTitleWithoutUserPrefix(conversation, context),
                0xfff37420, Color.WHITE);

        ExtendedImageView avatar = (ExtendedImageView) headerGroupChat.findViewById(R.id.avatar);
        ConversationHelper.getInstance().bindAvatarToImageView(conversation, R.dimen.avatar_in_actiobar_diameter, avatar, defaultGroupDrawable);
        ConversationHelper.getInstance().setupAvatarImageViewClickableForeground(conversation, avatar);

        TextView users = ((TextView) headerGroupChat.findViewById(R.id.users));
        TextView topic = ((TextView) headerGroupChat.findViewById(R.id.topic));
        topic.setText(ConversationHelper.getInstance().getTitle(conversation, getContext()));
        if (conversation.isGroup()) {
            users.setText(getString(R.string.user_count, conversation.getActualUsers().size()));
        } else {
            users.setVisibility(View.GONE);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) topic.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_TOP, 0);
            lp.addRule(RelativeLayout.CENTER_VERTICAL);
            topic.setPadding(0, 0, 0, 0);
        }

        headerGroupChat.setOnClickListener(v -> mListener.onSourceDetails(v));
    }

    private void initSendMessageForm() {
        mSendMessageText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == R.id.send_reply_to_comment) {
                sendMessage();
                return true;
            }
            return false;
        });
        mSendMessageText.setHint(R.string.your_message);
        mSendMessageButton.setOnClickListener(v -> sendMessage());
    }

    private void showAttachImageMenu(View from) {
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
        dialog.show(getChildFragmentManager(), "SelectPhotoSourceDialogFragment");
    }

    private void sendAttachment(Uri imageUri) {
        TypedOutput typedOutput = new ContentTypedOutput(getContext(), imageUri, null);
        Observable<Message> observable = mConversationSubject
                .take(1)
                .flatMap(conversation -> RestClient.getAPiMessenger().postMessageWithAttachments(null,
                            conversation.id, "", UUID.randomUUID().toString(), null,
                            Collections.singletonMap("files[]", typedOutput)));
        sendMessage(observable);
    }

    private void sendMessage() {
        String comment = mSendMessageText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(getActivity(), R.string.please_write_something, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }

        Observable<Message> observable = mConversationSubject
                .take(1)
                .flatMap(conversation -> RestClient.getAPiMessenger().postMessage(null,
                        conversation.id, comment, UUID.randomUUID().toString(), null));
        sendMessage(observable);
    }

    @SuppressLint("RxSubscribeOnError")
    private void sendMessage(Observable<Message> observable) {

        mPostMessageSubscription.unsubscribe();

        //mSendMessageText.setEnabled(false);
        mSendMessageProgress.setVisibility(View.VISIBLE);
        mSendMessageButton.setVisibility(View.INVISIBLE);

        mPostMessageSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(() -> {
                    mSendMessageText.setEnabled(true);
                    mSendMessageProgress.setVisibility(View.INVISIBLE);
                    mSendMessageButton.setVisibility(View.VISIBLE);
                })
                .subscribe(new Observer<Message>() {
                    @Override
                    public void onCompleted() {
                        if (mSendMessageText != null) mSendMessageText.setText("");
                    }

                    @Override
                    public void onError(Throwable e) {
                        MessageHelper.showError(ConversationFragment.this, e, R.string.error_post_comment, REQUEST_CODE_LOGIN);
                    }

                    @Override
                    public void onNext(Message message) {
                        sendAnalytics();
                        addMessageScrollToEnd(message);
                    }
                });
    }

    private void addMessageScrollToEnd(Message message) {
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

        mListView.getViewTreeObserver().addOnPreDrawListener(new SafeOnPreDrawListener(mListView, new SafeOnPreDrawListener.RunOnLaidOut() {
            @Override
            public boolean run(View root) {
                if (mListView == null) return true;
                LinearLayoutManager layoutManager = (LinearLayoutManager) mListView.getLayoutManager();
                if (smooth) {
                    mListView.smoothScrollToPosition(newPosition);
                } else {
                    layoutManager.scrollToPositionWithOffset(newPosition, 0);
                    mListScrollController.checkScrollStateOnViewPreDraw();
                }
                return false;
            }
        }));
    }

    private void addMessagesDoNotScrollList(Message[] messages) {
        Long oldTopId = null;
        int oldTopTop = 0;

        ConversationAdapter.ViewHolderMessage top = findTopVisibleMessageViewHolder();
        if (top != null) {
            oldTopId = mAdapter.getItemId(top.getLayoutPosition());
            oldTopTop = top.itemView.getTop();
        }

        mAdapter.addMessages(messages);

        if (oldTopId != null) {
            Integer newPosition = mAdapter.findPositionById(oldTopId);
            if (newPosition != null) {
                LinearLayoutManager lm = (LinearLayoutManager) mListView.getLayoutManager();
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
            if (holder instanceof ConversationAdapter.ViewHolderMessage)
                return (ConversationAdapter.ViewHolderMessage) holder;
        }
        return null;
    }

    private void sendAnalytics() {
        AnalyticsHelper.getInstance().sendUXEvent(Constants.ANALYTICS_ACTION_UX_SEND_MESSAGE);
    }

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

        private Session mSession = Session.getInstance();

        public Adapter(Context context) {
            super(context);
        }

        @Override
        public void initClickListeners(final RecyclerView.ViewHolder pHolder) {
            View.OnClickListener onClickListener = new View.OnClickListener() {

                RecyclerView.ViewHolder holder = pHolder;

                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.messages_load_more:
                            if (mMessagesLoader != null)
                                mMessagesLoader.activateCacheInBackground();
                            break;
                        case R.id.avatar:
                            int position = pHolder.getAdapterPosition();
                            Message message = getMessage(position);
                            if (message != null) {
                                TlogActivity.startTlogActivity(getActivity(), message.userId, v,
                                        R.dimen.avatar_small_diameter);
                            }
                            break;
                    }
                }
            };

            if (pHolder instanceof LoadMoreButtonHeaderHolder) {
                ((LoadMoreButtonHeaderHolder) pHolder).loadButton.setOnClickListener(onClickListener);
            } else if (pHolder instanceof ViewHolderMessage) {
                ((ViewHolderMessage) pHolder).avatar.setOnClickListener(onClickListener);
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
                    && mMessagesLoader != null) {
                Message msg = getMessage((ViewHolderMessage) holder);
                if (msg != null && !msg.isMarkedAsRead()) {
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
            if (mSession.isMe(userUuid)) {
                return mSession.getCachedCurrentUser();
            } else {
                Conversation conversation = mConversationSubject.getValue();
                if (conversation != null) {
                    if (conversation.isGroup()) {
                        return conversation.findUserById(userUuid);
                    } else {
                        return conversation.recipient;
                    }
                }
                return null;
            }
        }
    }

    /**
     * сервис, отмечающий сообщения как прочитанные
     */
    class MarkMessagesAsRead {

        private final Handler mHandler;

        private final Set<Long> mPostIds;

        private Subscription mPostMessageSubscription = Subscriptions.unsubscribed();

        /**
         * Выполняется или запланирована отправка на сервер. Устанавливается дл postDelayed, снимается когда очередь пуста.
         */
        private boolean mDoPost;

        private final int POST_DELAY_MS = 200;

        public MarkMessagesAsRead() {
            mHandler = new Handler();
            mPostIds = new HashSet<>(3);
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

                Observable<Status.MarkMessagesAsRead> observablePost =
                        mConversationSubject
                                .take(1)
                                .flatMap(conversation -> RestClient.getAPiMessenger()
                                    .markMessagesAsRead(null, conversation.id,
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

        public MessagesLoader() {
            mHandler = new Handler();
            mKeepOnAppending = new AtomicBoolean(true);
            mMessagesAppendSubscription = Subscriptions.unsubscribed();
            mMessagesRefreshSubscription = Subscriptions.unsubscribed();
            mApiMessenger = RestClient.getAPiMessenger();
        }

        protected Observable<MessageList> createObservable(Long sinceEntryId, Integer limit) {
            return mConversationSubject
                    .take(1)
                    .flatMap(conversation -> mApiMessenger.getMessages(null, conversation.id, null, sinceEntryId, limit, null));
        }

        public void refreshMessages() {
            int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
            Observable<MessageList> observableFeed = mMessagesLoader.createObservable(null, requestEntries)
                    .observeOn(AndroidSchedulers.mainThread());

            refreshFeed(observableFeed, requestEntries);
        }

        private void refreshFeed(Observable<MessageList> observable, int entriesRequested) {
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
            MessageHelper.showError(ConversationFragment.this, e, R.string.error_append_feed, REQUEST_CODE_LOGIN);
        }

        protected void onLoadNext(boolean isRefresh, int entriesRequested, MessageList messages) {
            if (DBG) Log.e(TAG, "onNext " + messages.toString());
            boolean keepOnAppending = (messages != null) && (messages.messages.length == entriesRequested);
            if (messages != null && messages.messages.length > 0) {
                if (isRefresh) {
                    mAdapter.addMessages(messages.messages);
                    scrollListToPosition(mAdapter.getLastPosition(), false);
                    mStartAppending = true;
                    mListScrollController.checkScrollStateOnViewPreDraw();
                } else {
                    addMessagesDoNotScrollList(messages.messages);
                }
            }

            if (messages != null && messages.messages.length == 0) {
                ImeUtils.showIme(mSendMessageText);
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

        public class MessagesLoadObserver implements Observer<MessageList> {

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
            public void onNext(MessageList messages) {
                MessagesLoader.this.onLoadNext(mIsRefresh, mEntriesRequested, messages);
            }
        }

    }

    public interface OnFragmentInteractionListener extends ListScrollController.OnListScrollPositionListener {
        void onSourceDetails(View fromView);
    }
}
