package ru.taaasty.ui.messages;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import junit.framework.Assert;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import de.greenrobot.event.EventBus;
import okhttp3.RequestBody;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.adapters.ConversationAdapter;
import ru.taaasty.events.pusher.MessageChanged;
import ru.taaasty.events.pusher.MessagesRemoved;
import ru.taaasty.events.pusher.UpdateMessagesReceived;
import ru.taaasty.events.pusher.UserMessagesRemoved;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.UriRequestBody;
import ru.taaasty.rest.model.Status;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.UserStatusInfo;
import ru.taaasty.rest.model.conversations.Attachment;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.conversations.GroupConversation;
import ru.taaasty.rest.model.conversations.HasManyUsers;
import ru.taaasty.rest.model.conversations.Message;
import ru.taaasty.rest.model.conversations.MessageList;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.rest.model.conversations.TypedPushMessage;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.PhotoSourceManager;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.ImeUtils;
import ru.taaasty.utils.ListScrollController;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.SafeOnPreDrawListener;
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

    private final ConversationHelper mChatHelper = ConversationHelper.getInstance();

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
    private TextView tvTyping;
    private TextView tvStatus;
    private StatusPresenter statusPresenter;


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


        Conversation conversation = null;
        if (getArguments() != null) {
            conversation = getArguments().getParcelable(ARG_CONVERSATION);
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
    private TextWatcher typingTextWatcher = new TextWatcher() {
        public static final int MIN_SENDING_INTERVAL = 5000;
        private long oldDate = 0;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            long nowDate = System.currentTimeMillis();
            long timeInterval = nowDate - oldDate;
            if (timeInterval > MIN_SENDING_INTERVAL && mConversationSubject.hasValue()) {
                Observable<Object> sendTyped = RestClient.getAPiMessenger().sendTyped(null, mConversationSubject.getValue().getId());
                sendTyped
                        .subscribeOn(RestSchedulerHelper.getScheduler())
                        .subscribe();
                oldDate = nowDate;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = getActivity().getLayoutInflater().inflate(R.layout.fragment_conversation, container, false);

        mSendMessageText = (EditText) v.findViewById(R.id.reply_to_comment_text);
        mSendMessageButton = v.findViewById(R.id.reply_to_comment_button);
        mSendMessageProgress = v.findViewById(R.id.reply_to_comment_progress);
        mEmptyView = v.findViewById(R.id.empty_view);
        mToolbar = (Toolbar)v.findViewById(R.id.toolbar);
        tvTyping = (TextView) mToolbar.findViewById(R.id.typing);
        tvStatus = (TextView) mToolbar.findViewById(R.id.status);
        statusPresenter = new StatusPresenter(getActivity(),tvStatus,mChatHelper);
        mListView = (RecyclerView) v.findViewById(R.id.recycler_list_view);
        mListView.setItemAnimator(null);

        mSendMessageText.addTextChangedListener(typingTextWatcher);
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
                .distinctUntilChanged()
                .subscribe(new Observer<Conversation>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(Conversation conversation) {
                        if (DBG) Assert.assertSame(Looper.getMainLooper(), Looper.myLooper());
                        if (mAdapter != null) {
                            mAdapter.setConversation(conversation);
                        }
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
        statusPresenter.start();
    }

    @Override
    public void onPause() {
        userTypingTextCountDownTimer.onFinish();
        userTypingTextCountDownTimer.cancel();
        statusPresenter.stop();
        super.onPause();
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
    public void onStop() {
        super.onStop();

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

    public void onEventMainThread(TypedPushMessage typedPushMessage) {
        if (!mConversationSubject.hasValue()) return;
        if (typedPushMessage.conversationId != mConversationSubject.getValue().getId()) return;
        userTypingTextCountDownTimer.cancel();
        String typingViewText = getResources().getString(R.string.typing);
        if (mConversationSubject.getValue() instanceof GroupConversation) {
            List<User> userList = ((GroupConversation) mConversationSubject.getValue()).getUsers();
            User typingUser = mChatHelper.findUserById(userList, typedPushMessage.userId);
            if (typingUser != null) {
                typingViewText = typingUser.getName() + " " + getResources().getString(R.string.typing);
            }
        }
        tvTyping.setText(typingViewText);
        tvTyping.setVisibility(View.VISIBLE);
        tvStatus.setVisibility(View.GONE);
        userTypingTextCountDownTimer.start();
    }

    private CountDownTimer userTypingTextCountDownTimer = new CountDownTimer(6000, 6000) {
        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            tvTyping.setVisibility(View.GONE);
            tvStatus.setVisibility(View.VISIBLE);
        }
    };


    public void onEventMainThread(MessageChanged event) {
        Conversation conversation = mConversationSubject.getValue();
        if ((mAdapter != null)
                && (conversation != null)
                && (event.message.conversationId == conversation.getId())) {
            addMessageScrollToEnd(event.message);
            userTypingTextCountDownTimer.onFinish();
            userTypingTextCountDownTimer.cancel();
        }
    }

    public void onEventMainThread(UpdateMessagesReceived event) {
        Conversation conversation = mConversationSubject.getValue();
        if (mAdapter != null
                && (conversation != null)
                && (event.updateMessages.conversationId == conversation.getId())) {
            mAdapter.markMessagesAsRead(event.updateMessages.messages);
        }
    }

    public void onEventMainThread(UserMessagesRemoved event) {
        Conversation conversation = mConversationSubject.getValue();
        if (mAdapter == null
                || conversation == null
                || event.messages.conversationId != conversation.getId()) return;
        mAdapter.changeMessages(event.messages.messages);
    }

    public void onEventMainThread(MessagesRemoved event) {
        Conversation conversation = mConversationSubject.getValue();
        if (mAdapter == null
                || conversation == null
                || event.messages.conversationId != conversation.getId()) return;
        mAdapter.hideMyMessages(event.messages.messages);
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

        ExtendedImageView avatar = (ExtendedImageView) headerGroupChat.findViewById(R.id.avatar);
        mChatHelper.bindConversationIconToImageView(conversation, R.dimen.avatar_in_actiobar_diameter, avatar);
        mChatHelper.setupAvatarImageViewClickableForeground(conversation, avatar);

        TextView topic = ((TextView) headerGroupChat.findViewById(R.id.topic));
        topic.setText(mChatHelper.getTitle(conversation, getContext()));
        headerGroupChat.setOnClickListener(v -> mListener.onSourceDetails(v));
    }

    private  class StatusPresenter {
        private TextView tvStatus;
        private ConversationHelper conversationHelper;
        private Context context;
        private Timer timer = new Timer();
        private TimerTask timerTask;

        public StatusPresenter(Context context, TextView tvStatus, ConversationHelper conversationHelper) {
            this.tvStatus = tvStatus;
            this.conversationHelper = conversationHelper;
            this.context = context;
        }

        public void start() {
            if (timerTask != null) {
                timerTask.cancel();
                timer.purge();
            }
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    tvStatus.post(()->updateToolbarStatus());
                }
            };
            updateToolbarStatus();
            timer.schedule(timerTask, 0, 5000);
        }

        public void stop() {
            if (timerTask != null) {
                timerTask.cancel();
                timer.purge();
            }
        }

        private void updateToolbarStatus() {
            if (!mConversationSubject.hasValue()) return;
            Conversation conversation = mConversationSubject.getValue();

            if (conversation instanceof GroupConversation || conversation instanceof PublicConversation) {

                RestClient
                        .getAPiMessenger()
                        .getConversation(conversation.getId())
                        .subscribeOn(RestSchedulerHelper.getScheduler())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                freshConversation -> {
                                    String statusString = context.getString(R.string.user_count, conversationHelper.countActiveUsers(freshConversation));
                                    tvStatus.setText(statusString);
                                },
                                error -> {

                                }
                        );
            }else if (conversation instanceof PrivateConversation) {
                PrivateConversation privateConversation = (PrivateConversation) conversation;
                RestClient
                        .getAPiOnlineStatuses().getUserInfo(""+privateConversation.getRecipient().getId())
                        .subscribeOn(RestSchedulerHelper.getScheduler())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(userList -> {
                                    if (userList.size() == 1) {
                                        UserStatusInfo userStatusInfo = userList.get(0);
                                        if (userStatusInfo.isOnline){
                                            tvStatus.setText(R.string.online);
                                        }else {
                                            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM yyyy");
                                            tvStatus.setText(context.getString(R.string.last_seen) +" "+ simpleDateFormat.format(userStatusInfo.lastSeenAt));
                                        }
                                    }
                                },
                                error -> {

                                }

                        );
            }
        }

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

        String filename = imageUri.getLastPathSegment();
        if (!filename.matches(".+\\..{2,6}$")) {
            filename = filename + ".jpg";
        }

        HashMap<String, RequestBody> imagesMap = new HashMap<>();
        imagesMap.put("files[]\"; filename=\"" + filename, new UriRequestBody(getActivity(), imageUri));
        String uuid = UUID.randomUUID().toString();

        mConversationSubject
                .take(1)
                .map(conversation -> {
                    mListView.post(() -> {

                        Attachment attachment = new Attachment();
                        //todo make real content type
                        attachment.contentType = "image/jpeg";
                        attachment.url = imageUri.toString();
                        Attachment[] attachments = new Attachment[]{attachment};

                        Message newMessage = Message.newBuilder()
                                .uuid(uuid)
                                .createdAt(new Date())
                                .contentHtml("")
                                .id(0)
                                .author(null)
                                .conversationId(conversation.getId())
                                .userId(Session.getInstance().getCurrentUserId())
                                .type("Message")
                                .attachments(attachments)

                                .build();
                        newMessage.isOnServer = false;
                        addMessageScrollToEnd(newMessage);
                        UnsentMessageRepository.addMessage(newMessage);
                    });
                    return conversation;
                })
                .flatMap(
                        conversation -> RestClient.getAPiMessenger().postMessageWithAttachments(
                                null,
                                conversation.getId(),
                                "",
                                uuid,
                                null,
                                imagesMap
                        )
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(
                        message -> sendAnalytics(),
                        error -> MessageHelper.showError(ConversationFragment.this, error, R.string.error_post_comment, REQUEST_CODE_LOGIN)
                );

    }

    private void sendMessage() {
        String comment = mSendMessageText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(getActivity(), R.string.please_write_something, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }
        mSendMessageText.setText("");

        String uuid = UUID.randomUUID().toString();
        mConversationSubject
                .take(1)
                .map(conversation -> {
                    mListView.post(() -> {

                        Message newMessage = Message.newBuilder()
                                .uuid(uuid)
                                .createdAt(new Date())
                                .contentHtml(comment)
                                .id(0)
                                .author(null)
                                .conversationId(conversation.getId())
                                .userId(Session.getInstance().getCurrentUserId())
                                .type("Message")
                                .build();

                        newMessage.isOnServer = false;
                        addMessageScrollToEnd(newMessage);
                        UnsentMessageRepository.addMessage(newMessage);
                    });

                    return conversation;
                })
                .flatMap(conversation ->
                        RestClient.getAPiMessenger().postMessage(
                                null,
                                conversation.getId(),
                                comment,
                                uuid,
                                null
                        )
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(
                        message -> sendAnalytics(),
                        error -> MessageHelper.showError(ConversationFragment.this, error, R.string.error_post_comment, REQUEST_CODE_LOGIN)
                );
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

            View.OnClickListener onClickListener = v -> {
                switch (v.getId()) {
                    case R.id.messages_load_more:
                        if (mMessagesLoader != null)
                            mMessagesLoader.activateCacheInBackground();
                        break;
                    case R.id.avatar:
                        int position = pHolder.getAdapterPosition();
                        Message message = getMessage(position);
                        if (mChatHelper.isNullOrAnonymousConversation(mConversationSubject.getValue())) {
                            return;
                        }
                        if (message != null) {
                            TlogActivity.startTlogActivity(getActivity(),
                                    message.getRealUserId(mConversationSubject.getValue()),
                                    v,
                                    R.dimen.avatar_small_diameter);
                        }
                        break;
                }
            };

            if (pHolder instanceof LoadMoreButtonHeaderHolder) {
                ((LoadMoreButtonHeaderHolder) pHolder).loadButton.setOnClickListener(onClickListener);
            } else if (pHolder instanceof ViewHolderMessage && ((ViewHolderMessage) pHolder).avatar != null) {
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
            Conversation conversation = mConversationSubject.getValue();
            if (conversation == null) {
                // Биндимся немного раньше загрузки данных по конверсейшну
                return null;
            }
            switch (conversation.getType()) {
                case PRIVATE:
                    if (mSession.isMe(userUuid)) {
                        return mSession.getCachedCurrentUser();
                    } else {
                        return ((PrivateConversation)conversation).getRecipient();
                    }
                case GROUP:
                case PUBLIC:
                    return mChatHelper.findUserById(((HasManyUsers)conversation).getUsers(), userUuid);                 
                case OTHER:
                default:
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
                                    .markMessagesAsRead(null, conversation.getId(),
                                            TextUtils.join(",", postSet))
                                    .subscribeOn(RestSchedulerHelper.getScheduler())
                                );

                mPostMessageSubscription = observablePost
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(RestSchedulerHelper.getScheduler())
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
        public static final int ENTRIES_TO_TRIGGER_APPEND = 7;

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

        private boolean mLoadingMessages;

        private long mLastAppendMessageId;

        public MessagesLoader() {
            mHandler = new Handler();
            mKeepOnAppending = new AtomicBoolean(true);
            mMessagesAppendSubscription = Subscriptions.unsubscribed();
            mMessagesRefreshSubscription = Subscriptions.unsubscribed();
        }
        private Message[] connectArrays(Message[] array1,Message[] array2) {
            Message[] resultArray = Arrays.copyOf(array1, array1.length + array2.length);
            System.arraycopy(array2,0,resultArray,array1.length,array2.length);
            return resultArray;
        }
        protected Observable<MessageList> createObservable(Long sinceEntryId, Integer limit) {


            Observable<MessageList> messageListObservable = mConversationSubject
                    .take(1)
                    .flatMap(conversation -> RestClient.getAPiMessenger()
                            .getMessages(conversation.getId(), null, null, sinceEntryId, limit, null)
                            .subscribeOn(RestSchedulerHelper.getScheduler())
                            .map(messageList -> {
                                Message[] unsentMessagesArray = UnsentMessageRepository.getUnsentMessagesArray(conversation.getId());

                                MessageList newMessageList = new MessageList();
                                newMessageList.totalCount = messageList.totalCount+unsentMessagesArray.length;
                                newMessageList.scopeCount = messageList.scopeCount;
                                newMessageList.messages = connectArrays(messageList.messages, unsentMessagesArray);
                                return newMessageList;
                            })
                    );

            return messageListObservable;
        }

        public void refreshMessages() {
            int requestEntries = Constants.LIST_FEED_INITIAL_LENGTH;
            Observable<MessageList> observableFeed = mMessagesLoader.createObservable(null, requestEntries);
            refreshFeed(observableFeed, requestEntries);
        }

        private void refreshFeed(Observable<MessageList> observable, int entriesRequested) {
            if (!mMessagesRefreshSubscription.isUnsubscribed()) {
                onFeedIsUnsubscribed(true);
                mMessagesRefreshSubscription.unsubscribe();
            }
            mMessagesRefreshSubscription = observable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(RestSchedulerHelper.getScheduler())
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
            // notifyDataSetChanged() нельзя вызывать в onBindViewHolder, поэтому откладываем
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                    if (!mMessagesAppendSubscription.isUnsubscribed()) {
                        onFeedIsUnsubscribed(false);
                        mMessagesAppendSubscription.unsubscribe();
                    }

                    int requestEntries = Constants.LIST_FEED_APPEND_LENGTH;
                    mLastAppendMessageId = lastEntryId;
                    mMessagesAppendSubscription = createObservable(lastEntryId, requestEntries)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(RestSchedulerHelper.getScheduler())
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
            if (position >= ENTRIES_TO_TRIGGER_APPEND) {
                return;
            }

            //if (DBG) Log.d(TAG, "onBindViewHolder() called with: position = [" + position + "]");

            if (!mKeepOnAppending.get()
                    || !mStartAppending
                    || mAdapter.isEmpty()
                    || mLoadingMessages) return;

            final Long lastEntryId = mAdapter.getTopMessageId();
            if (lastEntryId == null) return;

            if (mLastAppendMessageId == lastEntryId) return; // загружали уже

            activateCacheInBackground();
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
