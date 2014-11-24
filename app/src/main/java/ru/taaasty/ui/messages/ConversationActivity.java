package ru.taaasty.ui.messages;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.View;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Conversation;
import ru.taaasty.widgets.ErrorTextView;

public class ConversationActivity extends Activity implements ConversationFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ConversationActivity";

    private static final String ARG_CONVERSATION = "ru.taaasty.ui.feeds.ConversationActivity.conversation";

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
}
