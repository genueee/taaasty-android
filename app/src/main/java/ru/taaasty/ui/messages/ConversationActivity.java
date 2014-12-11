package ru.taaasty.ui.messages;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Conversation;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.utils.TargetSetHeaderBackground;
import ru.taaasty.widgets.ErrorTextView;

public class ConversationActivity extends Activity implements ConversationFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationActivity";

    private static final String ARG_CONVERSATION = "ru.taaasty.ui.feeds.ConversationActivity.conversation";

    private static final int HIDE_ACTION_BAR_DELAY = 500;

    // Anti-picasso weak ref
    private TargetSetHeaderBackground mBackgroundTarget;

    private Handler mHideActionBarHandler;

    private boolean imeKeyboardShown;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        Conversation conversation = getIntent().getParcelableExtra(ARG_CONVERSATION);
        if (conversation == null) throw new IllegalArgumentException("no conversation");

        if (savedInstanceState == null) {
            Fragment conversationFragment = ConversationFragment.newInstance(conversation);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, conversationFragment)
                    .commit();
        }
        mHideActionBarHandler = new Handler();
        setupActionBar(conversation.recipient);
        bindDesign(conversation.recipient.getDesign());

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
    protected void onDestroy() {
        super.onDestroy();
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler = null;
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    @Override
    public void onEdgeReached(boolean atTop) {
        if (DBG) Log.v(TAG, "onBottomReached atTop: " + atTop);
        if (!atTop) return;
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        ActionBar ab = getActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onEdgeUnreached() {
        if (DBG) Log.v(TAG, "onEdgeUnreached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler.postDelayed(mHideActionBarRunnable, HIDE_ACTION_BAR_DELAY);
    }


    void onImeKeyboardShown() {
        ConversationFragment fragment = (ConversationFragment)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onImeKeyboardShown();
    }

    void onImeKeyboardHidden() {

    }

    private void setupActionBar(User recipient) {
        ActionBar ab = getActionBar();
        if (ab == null) return;
        ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark)));

        SpannableString title = new SpannableString(recipient.getName());
        ForegroundColorSpan textColor = new ForegroundColorSpan(Color.WHITE);
        title.setSpan(textColor, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ab.setTitle(title);
    }

    private void bindDesign(final TlogDesign design) {
        final View root = getWindow().getDecorView();
        if (root.getWidth() > 1) {
            bindDesignMeasured(design);
            return;
        }

        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (root.getViewTreeObserver().isAlive()) {
                    root.getViewTreeObserver().removeOnPreDrawListener(this);
                    bindDesignMeasured(design);
                }
                return true;
            }
        });
    }

    private void bindDesignMeasured(TlogDesign design) {
        View root = getWindow().getDecorView();
        mBackgroundTarget = new TargetSetHeaderBackground(root,
                design, R.color.conversation_background_overlay, 0);
        RequestCreator rq = Picasso.with(this)
                .load(design.getBackgroundUrl());
        if (root.getWidth() > 1 && root.getHeight() > 1) {
            rq.resize(root.getWidth() / 2, root.getHeight() / 2)
                    .centerCrop();
        }
        rq.into(mBackgroundTarget);
    }

    private Runnable mHideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar ab = getActionBar();
            if (ab != null) ab.hide();
        }
    };
}
