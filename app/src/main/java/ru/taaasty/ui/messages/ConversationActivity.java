package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.pollexor.ThumborUrlBuilder;
import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.ConversationVisibilityChanged;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.TlogDesign;
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

    private static final String ARG_RECIPIENT_ID = "ru.taaasty.ui.feeds.ConversationActivity.recipient_id";

    private static final String BUNDLE_ARG_CONVERSATION = "ru.taaasty.ui.feeds.ConversationActivity.BUNDLE_ARG_CONVERSATION";

    private static final int REQUEST_CODE_EDIT_CONVERSATION = 1;
    private static final int REQUEST_CODE_CONVERSATION_DETAILS = 2;

    private static final int HIDE_ACTION_BAR_DELAY = 500;

    // Anti-picasso weak ref
    private TargetSetHeaderBackground mBackgroundTarget;

    private Handler mHideActionBarHandler;

    private boolean imeKeyboardShown;

    private Subscription mConversationSubscription = Subscriptions.unsubscribed();

    private long mRecipientId;

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

    public static Intent createIntent(Context source, long conversationId, long recipientId) {
        Intent intent = new Intent(source, ConversationActivity.class);
        intent.putExtra(ARG_RECIPIENT_ID, recipientId);
        intent.putExtra(ARG_CONVERSATION_ID, conversationId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        long conversationId;

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));

        if (getIntent().hasExtra(ARG_CONVERSATION)) {
            mConversation = getIntent().getParcelableExtra(ARG_CONVERSATION);
            mRecipientId = mConversation.recipientId;
            conversationId = mConversation.id;
        } else {
            conversationId = getIntent().getLongExtra(ARG_CONVERSATION_ID, -1);
            mRecipientId = getIntent().getLongExtra(ARG_RECIPIENT_ID, -1);
            if (savedInstanceState != null) {
                mConversation = savedInstanceState.getParcelable(BUNDLE_ARG_CONVERSATION);
            }
        }

        if (savedInstanceState == null) {
            Fragment conversationFragment;
            if (mConversation != null) {
                conversationFragment = ConversationFragment.newInstance(mConversation);
            } else {
                conversationFragment = ConversationFragment.newInstance(conversationId);
            }
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, conversationFragment)
                    .commit();
        }

        mHideActionBarHandler = new Handler();

        if (mConversation != null) {
            onConversationLoaded(mConversation);
        } else {
            loadConversation(mRecipientId);
        }

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

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().post(new ConversationVisibilityChanged(mRecipientId, true));
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().post(new ConversationVisibilityChanged(mRecipientId, false));
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
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler = null;
    }

    @Override
    public void onEdgeReached(boolean atTop) {
        /*
        if (DBG) Log.v(TAG, "onBottomReached atTop: " + atTop);
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        if (!atTop) {
            mHideActionBarHandler.postDelayed(mHideActionBarRunnable, HIDE_ACTION_BAR_DELAY);
        } else {
            mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
            ActionBar ab = getSupportActionBar();
            if (ab != null) ab.show();
        }
        */
    }

    @Override
    public void onEdgeUnreached() {
        /*
        if (DBG) Log.v(TAG, "onEdgeUnreached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler.postDelayed(mHideActionBarRunnable, HIDE_ACTION_BAR_DELAY);
        */
    }

    public void onConversationLoaded(Conversation conversation) {
        mConversation = conversation;
        setTitle(conversation.recipient.getName());
        if (!conversation.isGroup()) {
            bindDesign(conversation.recipient.getDesign());
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

    private void loadConversation(long recipientId) {
        mConversationSubscription.unsubscribe();
        ApiMessenger apiMessenger = RestClient.getAPiMessenger();

        Observable<Conversation> observable = apiMessenger.createConversation(null, recipientId);

        mConversationSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mLoadConversationObservable);
    }

    private void bindDesign(final TlogDesign design) {
        final View root = getWindow().getDecorView();
        SafeOnPreDrawListener.runWhenLaidOut(root, new SafeOnPreDrawListener.RunOnLaidOut() {
            @Override
            public boolean run(View root) {
                bindDesignMeasured(design);
                return true;
            }
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

    private Runnable mHideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar ab = getSupportActionBar();
            if (ab != null) ab.hide();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_EDIT_CONVERSATION:
                    if (EditCreateGroupActivity.ACTION_SAVE_CONVERSATION.equals(data.getAction())) {
                        onConversationLoaded((Conversation) data.getParcelableExtra(EditCreateGroupActivity.RESULT_CONVERSATION));
                    }
                    if (EditCreateGroupActivity.ACTION_LEAVE_CONVERSATION.equals(data.getAction())) {
                        finish();
                    }
                    break;
                case REQUEST_CODE_CONVERSATION_DETAILS:
                    if (data.getAction().equals(ConversationDetailsActivity.ACTION_CONVERSATION_REMOVED)) {
                        finish();
                    }
                    break;
            }
        }
    }

    private final Observer<Conversation> mLoadConversationObservable = new Observer<Conversation>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.v(TAG, getString(R.string.error_create_conversation), e);
            MessageHelper.showError(ConversationActivity.this, R.id.activityRoot, 1, e, R.string.error_create_conversation);
            // TODO здесь неавторизованным по хорошему надо возвращать ошибку
            finish();
        }

        @Override
        public void onNext(Conversation conversation) {
            onConversationLoaded(conversation);
        }
    };

    public void onEditGroupConversation(Conversation conversation) {
        EditCreateGroupActivity.editGroupConversation(this, conversation, REQUEST_CODE_EDIT_CONVERSATION);
    }

    public void onViewChatDetails(Conversation conversation) {
        ConversationDetailsActivity.start(this, conversation, REQUEST_CODE_CONVERSATION_DETAILS);
    }

    @Override
    public void onSourceDetails(Conversation conversation, View fromView) {
        if (!conversation.isGroup()) {
            onViewChatDetails(conversation);
        } else {
            onEditGroupConversation(conversation);
        }
    }
}
