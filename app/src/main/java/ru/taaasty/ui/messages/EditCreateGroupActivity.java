package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.events.pusher.ConversationChanged;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.ui.post.PhotoSourceManager;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener;
import ru.taaasty.utils.ConversationHelper;
import ru.taaasty.utils.ImeUtils;

/**
 * Created by arhis on 25.02.2016.
 */
public class EditCreateGroupActivity extends ActivityBase implements SelectPhotoSourceDialogListener, EditGroupFragment.InteractionListener {

    public static final String RESULT_CONVERSATION = EditCreateGroupActivity.class.getName() + ".conversation";
    private static final String EXTRA_CONVERSATION = EditCreateGroupActivity.class.getName() + ".conversation";
    public static final String ACTION_LEAVE_CONVERSATION = EditCreateGroupActivity.class.getName() + ".ACTION_LEAVE_CONVERSATION";
    public static final String ACTION_SAVE_CONVERSATION = EditCreateGroupActivity.class.getName() + ".ACTION_SAVE_CONVERSATION";

    public static final int REQUEST_PICK_USER = 3;

    private static final int REQUEST_PICK_PHOTO_BASE = 5;

    private PhotoSourceManager mPhotoManager;

    public static void newGroupConversation(Activity caller, Fragment receiver, int requestCode) {
        Intent intent = new Intent(caller, EditCreateGroupActivity.class);
        receiver.startActivityForResult(intent, requestCode);
    }

    public static void editGroupConversation(Activity caller, Conversation conversation, int requestCode) {
        Intent intent = new Intent(caller, EditCreateGroupActivity.class);
        intent.putExtra(EXTRA_CONVERSATION, conversation);
        caller.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_group);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Conversation conversation = getConversation();
            Fragment fragment = EditGroupFragment.newInstance(conversation);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment, EditGroupFragment.class.getName()).commit();
        }

        mPhotoManager = new PhotoSourceManager(this, "GroupPhoto", REQUEST_PICK_PHOTO_BASE, 3345,
                findViewById(R.id.coordinator_layout_container),
                (uri) -> getEditFragment().onImagePicked(uri));
        mPhotoManager.onCreate(savedInstanceState);

        setTitle(guessActivityTitle());
    }

    @Nullable
    private Conversation getConversation() {
        return getIntent().getParcelableExtra(EXTRA_CONVERSATION);
    }

    private EditGroupFragment getEditFragment() {
        return (EditGroupFragment)getSupportFragmentManager().findFragmentByTag(EditGroupFragment.class.getName());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPhotoManager.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mPhotoManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!getEditFragment().isInProgress()) {
                    ImeUtils.hideIme(getEditFragment().getView());
                    finish();
                }
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        mPhotoManager.startPickPhoto();
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        mPhotoManager.startMakePhoto();
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        // UNUSED
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        // UNSUED
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mPhotoManager.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_USER:
                    getEditFragment().onUserPicked(data.getParcelableExtra(UserPickerActivity.RESULT_USER));
                    break;
            }
        }
    }

    @Override
    public void onConversationLeaved(Conversation conversation) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_CONVERSATION, conversation);
        intent.setAction(ACTION_LEAVE_CONVERSATION);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onConversationSaved(Conversation conversation) {
        Intent intent = new Intent();
        intent.putExtra(RESULT_CONVERSATION, conversation);
        intent.setAction(ACTION_SAVE_CONVERSATION);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onDoNotDisturbStatusChanged(Conversation conversation) {
        if (isFinishing()) return;
        Intent intent = new Intent();
        intent.putExtra(RESULT_CONVERSATION, conversation);
        setResult(Activity.RESULT_OK, intent);
        EventBus.getDefault().post(new ConversationChanged(conversation));
    }

    private CharSequence guessActivityTitle() {
        Conversation conversation = getConversation();
        if (conversation == null) {
            // Скорее всего, это создание нового обсуждения
            return getText(R.string.edit_create_conversation_group_chat);
        }

        switch (conversation.getType()) {
            case PRIVATE:
                return getText(R.string.edit_create_conversation_title_conversation);
            case GROUP:
                return getText(R.string.edit_create_conversation_group_chat);
            case PUBLIC:
                return getText(R.string.edit_create_conversation_thread);
            case OTHER:
            default:
                return ConversationHelper.getInstance().getTitle(conversation, this);
        }
    }
}
