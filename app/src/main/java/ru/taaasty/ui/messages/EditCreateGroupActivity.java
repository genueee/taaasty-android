package ru.taaasty.ui.messages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener;
import ru.taaasty.utils.ImageUtils;

/**
 * Created by arhis on 25.02.2016.
 */
public class EditCreateGroupActivity extends ActivityBase implements SelectPhotoSourceDialogListener, EditGroupFragment.InteractionListener {

    public static final String RESULT_CONVERSATION = EditCreateGroupActivity.class.getName() + ".conversation";
    private static final String EXTRA_CONVERSATION = EditCreateGroupActivity.class.getName() + ".conversation";
    public static final String ACTION_LEAVE_CONVERSATION = EditCreateGroupActivity.class.getName() + ".ACTION_LEAVE_CONVERSATION";
    public static final String ACTION_SAVE_CONVERSATION = EditCreateGroupActivity.class.getName() + ".ACTION_SAVE_CONVERSATION";

    private static final int REQUEST_PICK_PHOTO = 1;
    private static final int REQUEST_MAKE_PHOTO = 2;
    public static final int REQUEST_PICK_USER = 3;

    private Uri mMakePhotoDstUri;
    private Fragment mCurrentFragment;

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
        toolbar.setTitle(getString(R.string.group_chat));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Conversation conversation = getConversation();
            mCurrentFragment = EditGroupFragment.newInstance(conversation);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, mCurrentFragment, EditGroupFragment.class.getName()).commit();
        } else {
            mCurrentFragment = getSupportFragmentManager().findFragmentByTag(EditGroupFragment.class.getName());
        }

    }

    private Conversation getConversation() {
        return getIntent().getParcelableExtra(EXTRA_CONVERSATION);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!((EditGroupFragment)mCurrentFragment).isInProgress()) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(mCurrentFragment.getView().getWindowToken(), 0);
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
        Intent photoPickerIntent = ImageUtils.createPickImageActivityIntent();
        startActivityForResult(photoPickerIntent, REQUEST_PICK_PHOTO);
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        Intent takePictureIntent;
        try {
            takePictureIntent = ImageUtils.createMakePhotoIntent(this, true);
            mMakePhotoDstUri = takePictureIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            startActivityForResult(takePictureIntent, REQUEST_MAKE_PHOTO);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(this, e.errorResourceId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {

    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_PHOTO:
                    Uri selectedImageUri = data.getData();
                    ((EditGroupFragment) mCurrentFragment).onImagePicked(selectedImageUri);
                    break;
                case REQUEST_MAKE_PHOTO:
                    ((EditGroupFragment) mCurrentFragment).onImagePicked(mMakePhotoDstUri);
                    break;
                case REQUEST_PICK_USER:
                    ((EditGroupFragment) mCurrentFragment).onUserPicked((User) data.getParcelableExtra(UserPickerActivity.RESULT_USER));
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
}
