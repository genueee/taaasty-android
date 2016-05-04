package ru.taaasty.ui.messages;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import rx.Subscription;

public class CreateMessageActivity extends ActivityBase {

    public static final String CONVERSATION_ID_EXTRA = "CONVERSATION_ID_EXTRA";
    public static final int EMPTY_CONVERSATION_ID_EXTRA = -1;
    private long conversationId;
    private ConversationChooserListFragment conversationChooserListFragment;
    private Subscription conversationChooserSubscription;

    public static void startActivity(Context context, long conversationId) {
        Intent intent = new Intent(context, CreateMessageActivity.class);
        intent.putExtra(CONVERSATION_ID_EXTRA, conversationId);
        context.startActivity(intent);
    }

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, CreateMessageActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
        conversationId = getIntent().getLongExtra(CONVERSATION_ID_EXTRA, EMPTY_CONVERSATION_ID_EXTRA);
        initContentFragment();
    }


    private void initContentFragment() {
        if (conversationId == EMPTY_CONVERSATION_ID_EXTRA) {
            addConversationChooserListFragment();

        } else {
            addCreateMessageFragment();
        }

    }

    private void addConversationChooserListFragment() {
        conversationChooserListFragment = (ConversationChooserListFragment) getSupportFragmentManager().findFragmentByTag("conversationChooserListFragment");
        if (conversationChooserListFragment == null) {
            conversationChooserListFragment = new ConversationChooserListFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, conversationChooserListFragment, null)
                    .commit();
        }
    }

    private void replaceConversationChooserListFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .remove(conversationChooserListFragment)
                .commit();
        if (conversationChooserSubscription != null) {
            conversationChooserSubscription.unsubscribe();
            conversationChooserSubscription = null;
        }
        conversationChooserListFragment = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationChooserListFragment != null) {
            conversationChooserSubscription = conversationChooserListFragment.getResultConversationIdObservable()
                    .subscribe(result -> {
                                getIntent().putExtra(CONVERSATION_ID_EXTRA, result);
                                conversationId = result;
                                replaceConversationChooserListFragment();
                                addCreateMessageFragment();

                            },
                            error -> {

                            }
                    );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (conversationChooserSubscription != null) {
            conversationChooserSubscription.unsubscribe();
        }

    }


    private void addCreateMessageFragment() {
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.container, new CreateMessageFragment())
                .commit();


    }
}
