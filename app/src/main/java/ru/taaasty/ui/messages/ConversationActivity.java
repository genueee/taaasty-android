package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.events.pusher.MessageChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.UserStatusInfo;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.conversations.GroupConversation;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.model.conversations.PublicConversation;
import ru.taaasty.rest.model.conversations.TypedPushMessage;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SafeOnPreDrawListener;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.ExtendedImageView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class ConversationActivity extends ActivityBase implements ConversationFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationActivity";

    private static final String ARG_CONVERSATION = "ru.taaasty.ui.feeds.ConversationActivity.conversation";

    private static final String ARG_CONVERSATION_ID = "ru.taaasty.ui.feeds.ConversationActivity.conversation_id";

    private static final String ARG_FORCE_SHOW_KEYBOARD = "ru.taaasty.ui.feeds.ConversationActivity.ARG_FORCE_SHOW_KEYBOARD";

    private static final String ARG_ENTRY_ID = "ru.taaasty.ui.feeds.ConversationActivity.entry_id";

    private static final String BUNDLE_ARG_CONVERSATION = "ru.taaasty.ui.feeds.ConversationActivity.BUNDLE_ARG_CONVERSATION";

    private static final int REQUEST_CODE_EDIT_CONVERSATION = 1;
    private static final int REQUEST_CODE_CONVERSATION_DETAILS = 2;

    private static final int HIDE_ACTION_BAR_DELAY = 500;

    // Anti-picasso weak ref
    private TargetSetHeaderBackground mBackgroundTarget;
    private boolean imeKeyboardShown;
    private Subscription mConversationSubscription = Subscriptions.unsubscribed();
    private long mConversationId;
    private long mEntryId;
    private boolean mIsStarted;
    private Toolbar mToolbar;
    private TextView tvTyping;
    private TextView tvStatus;
    private StatusPresenter statusPresenter;
    private final ConversationHelper mChatHelper = ConversationHelper.getInstance();
    @Nullable
    private Conversation mConversation;

    public static void startConversationActivity(Context source, Conversation conversation, View animateFrom) {
        Intent intent = new Intent(source, ConversationActivity.class);
        intent.putExtra(ARG_CONVERSATION, conversation);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity) source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    public static void startEntryConversationActivity(Context source, long entryId, View animateFrom) {
        Intent intent = new Intent(source, ConversationActivity.class);
        intent.putExtra(ARG_ENTRY_ID, entryId);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity) source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    // Интент, который будет открываться при тыке на уведомление
    public static Intent createNotificationIntent(Context source, long conversationId) {
        Intent intent = new Intent(source, ConversationActivity.class);
        intent.putExtra(ARG_CONVERSATION_ID, conversationId);
        intent.putExtra(ARG_FORCE_SHOW_KEYBOARD, true);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        tvTyping = (TextView) mToolbar.findViewById(R.id.typing);
        tvStatus = (TextView) mToolbar.findViewById(R.id.status);
        statusPresenter = new StatusPresenter(this,tvStatus,mChatHelper);

        mConversationId = getIntent().getLongExtra(ARG_CONVERSATION_ID, -1);
        mEntryId = getIntent().getLongExtra(ARG_ENTRY_ID, -1);
        boolean forceShowKeyboard = getIntent().getBooleanExtra(ARG_FORCE_SHOW_KEYBOARD, false);

        mConversation = getIntent().getParcelableExtra(ARG_CONVERSATION);
        if (savedInstanceState != null) {
            mConversation = savedInstanceState.getParcelable(BUNDLE_ARG_CONVERSATION);
        }


        if (savedInstanceState == null) {
            Fragment conversationFragment = new ConversationFragment.Builder()
                    .setConversation(mConversation)
                    .setForceShowKeyboard(forceShowKeyboard)
                    .build();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, conversationFragment)
                    .commit();
        }

        setupImeWatcher();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mIsStarted = true;
        if (mConversation != null) {
            onConversationLoaded(mConversation);
            EventBus.getDefault().post(new ConversationVisibilityChanged(mConversation.getId(), true));
        } else {
            loadConversation();
        }
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        statusPresenter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        userTypingTextCountDownTimer.onFinish();
        userTypingTextCountDownTimer.cancel();
        statusPresenter.stop();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsStarted = false;
        if (mConversation != null) {
            EventBus.getDefault().post(new ConversationVisibilityChanged(mConversation.getId(), false));
        }
        EventBus.getDefault().unregister(this);
    }
    public void bindToolbar() {
        View headerGroupChat = mToolbar.findViewById(R.id.header_group_info);
        ExtendedImageView avatar = (ExtendedImageView) headerGroupChat.findViewById(R.id.avatar);
        mChatHelper.bindConversationIconToImageView(mConversation, R.dimen.avatar_in_actiobar_diameter, avatar);
        mChatHelper.setupAvatarImageViewClickableForeground(mConversation, avatar);
        TextView topic = ((TextView) headerGroupChat.findViewById(R.id.topic));
        topic.setText(mChatHelper.getTitle(mConversation, this));
        headerGroupChat.setOnClickListener(v -> onSourceDetails(v));
    }

    public void onEventMainThread(TypedPushMessage typedPushMessage) {
        if (mConversation==null) return;
        if (typedPushMessage.conversationId != mConversation.getId()) return;
        userTypingTextCountDownTimer.cancel();
        String typingViewText = getResources().getString(R.string.typing);
        if (mConversation instanceof GroupConversation) {
            List<User> userList = ((GroupConversation) mConversation).getUsers();
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

    public void onEventMainThread(MessageChanged event) {
        if ((mConversation != null) && (event.message.conversationId == mConversation.getId())) {
            userTypingTextCountDownTimer.onFinish();
            userTypingTextCountDownTimer.cancel();
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_EDIT_CONVERSATION:
                    if (EditCreateGroupActivity.ACTION_SAVE_CONVERSATION.equals(data.getAction())) {
                    }
                    if (EditCreateGroupActivity.ACTION_LEAVE_CONVERSATION.equals(data.getAction())) {
                        finish();
                        return;
                    }
                    break;
                case REQUEST_CODE_CONVERSATION_DETAILS:
                    if (data.getAction().equals(ConversationDetailsActivity.ACTION_CONVERSATION_REMOVED)) {
                        finish();
                        return;
                    }
                    break;
            }
            if (data != null) {
                Conversation newConversation = (Conversation) data.getParcelableExtra(EditCreateGroupActivity.RESULT_CONVERSATION);
                if (newConversation != null
                        && (!newConversation.equals(mConversation)))
                    onConversationLoaded(newConversation);
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == REQUEST_CODE_EDIT_CONVERSATION) {
                // XXX здесь при смене doNotDisturb может оказаться новый conversation
                if (data != null) {
                    Conversation newConversation = (Conversation) data.getParcelableExtra(EditCreateGroupActivity.RESULT_CONVERSATION);
                    if ((newConversation != null)
                            && (!newConversation.equals(mConversation)))
                        onConversationLoaded(newConversation);
                }
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(BUNDLE_ARG_CONVERSATION, mConversation);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConversationSubscription.unsubscribe();
    }

    void onEventMainThread(ConversationChanged event) {
        if (mConversation != null
                && mConversation.getId() == event.conversation.getId()
                && !mConversation.equals(event.conversation)) {
            onConversationLoaded(event.conversation);
        }
    }

    @Override
    public void onEdgeReached(boolean atTop) {
    }

    @Override
    public void onEdgeUnreached() {
    }



    public void onConversationLoaded(Conversation conversation) {
        mConversation = conversation;
        if (mConversation.getType() == Conversation.Type.PRIVATE) {
            PrivateConversation chat = (PrivateConversation) conversation;
            setTitle(chat.getRecipient().getName());
            // Фон пока не ставим. TODO: удалить bindDesign, когда примут
            //bindDesign(chat.getRecipient().getDesign());
        }

        ConversationFragment fragment = (ConversationFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onConversationLoaded(conversation);
        bindToolbar();
    }

    void onImeKeyboardShown() {
        ConversationFragment fragment = (ConversationFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onImeKeyboardShown();
    }

    void onImeKeyboardHidden() {

    }

    private void setupImeWatcher() {
        final View activityRootView = findViewById(R.id.activityRoot);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
            if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                if (!imeKeyboardShown) {
                    imeKeyboardShown = true;
                    onImeKeyboardShown();
                }
            } else {
                if (imeKeyboardShown) {
                    imeKeyboardShown = false;
                    onImeKeyboardHidden();
                }
            }
        });
    }

    private void loadConversation() {
        mConversationSubscription.unsubscribe();
        ApiMessenger apiMessenger = RestClient.getAPiMessenger();

        Observable<Conversation> observable;
        if (mConversationId > 0) {
            observable = apiMessenger.getConversation(mConversationId);
        } else if (mEntryId > 0) {
            observable = apiMessenger.createGroupConversationByEntry(null, mEntryId);
        } else {
            // оп-па
            if (DBG) throw new IllegalStateException();
            return;
        }

        mConversationSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
                .subscribe(new Observer<Conversation>() {

                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        if (DBG) Log.v(TAG, getString(R.string.error_create_conversation), e);
                        setResult(Constants.ACTIVITY_RESULT_CODE_SHOW_ERROR);
                        MessageHelper.createErrorToast(ConversationActivity.this, getText(R.string.error_create_conversation)).show();
                        finish();
                    }

                    @Override
                    public void onNext(Conversation conversation) {
                        onConversationLoaded(conversation);
                        if (mIsStarted && mConversation == null) {
                            EventBus.getDefault().post(new ConversationVisibilityChanged(conversation.getId(), false));
                            mConversation = conversation;
                        }
                    }
                });
    }



    public void onEditGroupConversation(Conversation conversation) {
        EditCreateGroupActivity.editGroupConversation(this, conversation, REQUEST_CODE_EDIT_CONVERSATION);
    }

    public void onViewChatDetails(Conversation conversation) {
        ConversationDetailsActivity.start(this, conversation, REQUEST_CODE_CONVERSATION_DETAILS);
    }

    @Override
    public void onSourceDetails(View fromView) {
        if (mConversation == null) {
            if (DBG) throw new IllegalStateException();
            return;
        }
        if (mConversation.getType() == Conversation.Type.PRIVATE) {
            onViewChatDetails(mConversation);
        } else {
            onEditGroupConversation(mConversation);
        }
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
            if (mConversation==null) return;

            if (mConversation instanceof GroupConversation || mConversation instanceof PublicConversation) {

                RestClient
                        .getAPiMessenger()
                        .getConversation(mConversation.getId())
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
            }else if (mConversation instanceof PrivateConversation) {
                PrivateConversation privateConversation = (PrivateConversation) mConversation;
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



}
