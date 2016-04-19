package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.pollexor.ThumborUrlBuilder;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.conversations.PrivateConversation;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SafeOnPreDrawListener;
import ru.taaasty.utils.TargetSetHeaderBackground;
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

        mConversationId = getIntent().getLongExtra(ARG_CONVERSATION_ID, -1);
        mEntryId = getIntent().getLongExtra(ARG_ENTRY_ID, -1);
        mConversation = getIntent().getParcelableExtra(ARG_CONVERSATION);
        boolean forceShowKeyboard = getIntent().getBooleanExtra(ARG_FORCE_SHOW_KEYBOARD, false);

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
    }

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
    protected void onStop() {
        super.onStop();
        mIsStarted = false;
        if (mConversation != null) {
            EventBus.getDefault().post(new ConversationVisibilityChanged(mConversation.getId(), false));
        }
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
    }

    void onImeKeyboardShown() {
        ConversationFragment fragment = (ConversationFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onImeKeyboardShown();
    }

    void onImeKeyboardHidden() {

    }

    private void setupImeWatcher() {
        final View activityRootView = findViewById(R.id.activityRoot);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
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
                        }
                    }
                });
    }

    private void bindDesign(final TlogDesign design) {
        final View root = getWindow().getDecorView();
        SafeOnPreDrawListener.runWhenLaidOut(root, root1 -> {
            bindDesignMeasured(design);
            return true;
        });
    }

    private void bindDesignMeasured(TlogDesign design) {
        View root = getWindow().getDecorView();
        mBackgroundTarget = new TargetSetHeaderBackground(root,
                design, R.color.conversation_background_overlay);
        String originalUrl = design.getBackgroundUrl();
        ThumborUrlBuilder thumborUrl = NetworkUtils.createThumborUrl(originalUrl);
        if (root.getWidth() > 1 && root.getHeight() > 1) {
            thumborUrl
                    .resize(root.getWidth() / 2, root.getHeight() / 2)
                    .filter(ThumborUrlBuilder.noUpscale());
        }

        RequestCreator rq = Picasso.with(this)
                .load(thumborUrl.toUrlUnsafe())
                .config(Bitmap.Config.RGB_565);
        if (root.getWidth() > 1 && root.getHeight() > 1) {
            rq.resize(root.getWidth() / 2, root.getHeight() / 2)
                    .centerCrop();
        }
        rq.into(mBackgroundTarget);
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
}
