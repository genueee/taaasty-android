package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.rest.model.conversations.Conversation;

/**
 * Created by arhis on 25.02.2016.
 */
public class ConversationDetailsActivity extends ActivityBase implements ConversationDetailsFragment.InteractionListener {

    public static final String EXTRA_CONVERSATION = ConversationDetailsActivity.class.getName() + ".conversation";
    public static final String ACTION_CONVERSATION_REMOVED = ConversationDetailsActivity.class.getName() + ".ACTION_CONVERSATION_REMOVED";

    private Fragment mCurrentFragment;

    public static void start(Activity caller, Conversation conversation, int requestCode) {
        Intent intent = new Intent(caller, ConversationDetailsActivity.class);
        intent.putExtra(EXTRA_CONVERSATION, conversation);
        caller.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.chat));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Conversation conversation = getConversation();
            mCurrentFragment = ConversationDetailsFragment.newInstance(conversation);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, mCurrentFragment, ConversationDetailsFragment.class.getName()).commit();
        } else {
            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(ConversationDetailsFragment.class.getName());
        }

    }

    private Conversation getConversation() {
        return getIntent().getParcelableExtra(EXTRA_CONVERSATION);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!((ConversationDetailsFragment)mCurrentFragment).isInProgress()) {
                    finish();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onConversationRemoved(Conversation conversation) {
        Intent intent = new Intent(ACTION_CONVERSATION_REMOVED);
        intent.putExtra(EXTRA_CONVERSATION, conversation);
        setResult(RESULT_OK, intent);
        finish();
    }
}
