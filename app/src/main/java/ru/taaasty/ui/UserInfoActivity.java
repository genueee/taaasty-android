package ru.taaasty.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UploadService;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;
import ru.taaasty.ui.relationships.FollowingFollowersActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.ErrorTextView;

public class UserInfoActivity extends ActivityBase implements UserInfoFragment.OnFragmentInteractionListener,
        SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final String TAG = "UserInfoActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String KEY_CURRENT_PHOTO_URI = "ru.taaasty.ui.UserInfoActivity";

    private static final String DIALOG_TAG_SELECT_BACKGROUND = "DIALOG_SELECT_BACKGROUND";
    private static final String DIALOG_TAG_SELECT_AVATAR = "DIALOG_SELECT_AVATAR";

    private static final int REQUEST_PICK_BACKGROUND_PHOTO = Activity.RESULT_FIRST_USER + 2;
    private static final int REQUEST_MAKE_BACKGROUND_PHOTO = Activity.RESULT_FIRST_USER + 3;
    private static final int REQUEST_PICK_AVATAR_PHOTO = Activity.RESULT_FIRST_USER + 4;
    private static final int REQUEST_MAKE_AVATAR_PHOTO = Activity.RESULT_FIRST_USER + 5;

    public static final String ARG_USER = "ru.taaasty.ui.UserInfoActivity.author";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.UserInfoActivity.tlog_design";

    private User mUser;
    private Uri mCurrentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mUser = getIntent().getParcelableExtra(ARG_USER);
        if (mUser == null) throw new IllegalArgumentException("no User");

        if (savedInstanceState == null) {
            TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);

            Fragment userInfoFragment = UserInfoFragment.newInstance(mUser);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        } else {
            mCurrentPhotoUri = savedInstanceState.getParcelable(KEY_CURRENT_PHOTO_URI);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mCurrentPhotoUri != null) {
            outState.putParcelable(KEY_CURRENT_PHOTO_URI, mCurrentPhotoUri);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri imageUri = null;
        boolean imageUriIsAvatar = false;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_BACKGROUND_PHOTO:
                case REQUEST_PICK_AVATAR_PHOTO:
                    Uri selectedImageUri = data.getData();
                    if (DBG) Log.v(TAG,"image uri: " + selectedImageUri);
                    imageUri = selectedImageUri;
                    imageUriIsAvatar = requestCode == REQUEST_PICK_AVATAR_PHOTO;
                    break;
                case REQUEST_MAKE_BACKGROUND_PHOTO:
                case REQUEST_MAKE_AVATAR_PHOTO:
                    if (DBG) Log.v(TAG,"image uri: " + mCurrentPhotoUri);
                    ImageUtils.galleryAddPic(this, mCurrentPhotoUri);
                    imageUri = mCurrentPhotoUri;
                    imageUriIsAvatar = requestCode == REQUEST_PICK_AVATAR_PHOTO;
                    mCurrentPhotoUri = null;
            }
        }

        if (imageUri != null) {
            if (imageUriIsAvatar) {
                updateAvatar(imageUri);
            } else {
                updateBackground(imageUri);
            }
        }
    }

    @Override
    public void onEntriesCountClicked() {
        if (DBG) Log.v(TAG, "onEntriesCountClicked");
        Intent i = new Intent(this, TlogActivity.class);
        i.putExtra(TlogActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSubscribtionsCountClicked() {
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSubscribersCountClicked() {
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSelectBackgroundClicked() {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(DIALOG_TAG_SELECT_BACKGROUND) != null
                | fm.findFragmentByTag(DIALOG_TAG_SELECT_AVATAR) != null) {
            return;
        }
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
        dialog.show(getFragmentManager(), DIALOG_TAG_SELECT_BACKGROUND);
    }

    @Override
    public void onUserAvatarClicked() {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(DIALOG_TAG_SELECT_BACKGROUND) != null
                | fm.findFragmentByTag(DIALOG_TAG_SELECT_AVATAR) != null) {
            return;
        }
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
        dialog.show(getFragmentManager(), DIALOG_TAG_SELECT_AVATAR);
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
    public void onPickPhotoSelected(Fragment fragment) {
        int requestCode;
        if (DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag())) {
            requestCode = REQUEST_PICK_BACKGROUND_PHOTO;
        } else {
            requestCode = REQUEST_PICK_AVATAR_PHOTO;
        }

        Intent photoPickerIntent = ImageUtils.createPickImageActivityIntent();
        startActivityForResult(photoPickerIntent, requestCode);
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        Intent takePictureIntent;
        int requestCode;
        if (DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag())) {
            requestCode = REQUEST_MAKE_BACKGROUND_PHOTO;
        } else {
            requestCode = REQUEST_MAKE_AVATAR_PHOTO;
        }

        try {
            takePictureIntent = ImageUtils.createMakePhotoIntent(this, requestCode == REQUEST_MAKE_AVATAR_PHOTO);
            mCurrentPhotoUri = takePictureIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            startActivityForResult(takePictureIntent, requestCode);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(this, e.errorResourceId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        throw new IllegalStateException("ничего не удаляем");
    }

    void updateBackground(Uri imageUri) {
        UploadService.startUploadBackground(this, mUser.getId(), imageUri);
        EventBus.getDefault().post(TlogBackgroundUploadStatus.createUploadStarted(mUser.getId(), imageUri));
    }

    void updateAvatar(Uri imageUri) {
        UploadService.startUploadUserpic(this, mUser.getId(), imageUri);
        EventBus.getDefault().post(UserpicUploadStatus.createUploadStarted(mUser.getId(), imageUri));
    }
}
