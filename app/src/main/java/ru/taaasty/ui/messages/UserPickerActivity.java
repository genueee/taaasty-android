package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.User;

public class UserPickerActivity extends ActivityBase implements
        InitiateConversationFragment.OnFragmentInteractionListener {

    private static final String TAG_INITIATE_CONVERSATION_DIALOG = "TAG_INITIATE_CONVERSATION_DIALOG";

    public static final String RESULT_USER = UserPickerActivity.class.getName() + ".RESULT_USER";

    public static void startPicker(Activity caller, int requestCode) {
        Intent intent = new Intent(caller, UserPickerActivity.class);
        caller.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.add_chat_user));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Fragment initiateConversationFragment = new InitiateConversationFragment();
            Bundle args = new Bundle();
            args.putBoolean(InitiateConversationFragment.ARG_USED_AS_PICKER, true);
            initiateConversationFragment.setArguments(args);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, initiateConversationFragment, TAG_INITIATE_CONVERSATION_DIALOG)
                    .commit();
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
    public void onConversationCreated(Conversation conversation) {

    }

    @Override
    public void onUserPicked(User user) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_USER, user);
        setResult(RESULT_OK, intent);
        finish();
    }


    @Override
    public void notifyError(Fragment fragment, @Nullable Throwable exception, int fallbackResId) {

    }
}
