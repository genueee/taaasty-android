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

import com.aviary.android.feather.library.Constants;

import java.io.File;

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
    private static final int REQUEST_FEATHER_BACKGROUND_PHOTO = Activity.RESULT_FIRST_USER + 4;
    private static final int REQUEST_PICK_AVATAR_PHOTO = Activity.RESULT_FIRST_USER + 5;
    private static final int REQUEST_MAKE_AVATAR_PHOTO = Activity.RESULT_FIRST_USER + 6;
    private static final int REQUEST_FEATHER_AVATAR_PHOTO = Activity.RESULT_FIRST_USER + 7;

    public static final String ARG_USER = "ru.taaasty.ui.UserInfoActivity.author";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.UserInfoActivity.tlog_design";

    private User mUser;
    private TlogDesign mDesign;
    private Uri mMakePhotoDstUri;


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
            mDesign = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);

            Fragment userInfoFragment = UserInfoFragment.newInstance(mUser);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        } else {
            mMakePhotoDstUri = savedInstanceState.getParcelable(KEY_CURRENT_PHOTO_URI);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMakePhotoDstUri != null) {
            outState.putParcelable(KEY_CURRENT_PHOTO_URI, mMakePhotoDstUri);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri imageUri = null;
        boolean imageUriIsBackground = false;

        if ((requestCode == REQUEST_PICK_BACKGROUND_PHOTO)
                || (requestCode == REQUEST_MAKE_BACKGROUND_PHOTO)
                || (requestCode == REQUEST_FEATHER_BACKGROUND_PHOTO))
            imageUriIsBackground = true;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_BACKGROUND_PHOTO:
                case REQUEST_PICK_AVATAR_PHOTO:
                    Uri selectedImageUri = data.getData();
                    if (DBG) Log.v(TAG,"image uri: " + selectedImageUri);
                    imageUri = selectedImageUri;
                    if (imageUri != null) startFeatherPhoto(imageUriIsBackground, imageUri);
                    break;
                case REQUEST_MAKE_BACKGROUND_PHOTO:
                case REQUEST_MAKE_AVATAR_PHOTO:
                    if (DBG) Log.v(TAG,"image uri: " + mMakePhotoDstUri);
                    ImageUtils.galleryAddPic(this, mMakePhotoDstUri);
                    imageUri = mMakePhotoDstUri;
                    if (imageUri != null) startFeatherPhoto(imageUriIsBackground, mMakePhotoDstUri);
                    break;
                case REQUEST_FEATHER_AVATAR_PHOTO:
                case REQUEST_FEATHER_BACKGROUND_PHOTO:
                    imageUri = data.getData();
                    if (imageUri.toString().startsWith("/")) {
                        imageUri = Uri.fromFile(new File(imageUri.toString())); // Мозгоблядство от aviary
                    }
                    ImageUtils.galleryAddPic(this, imageUri);
                    // XXX: удалять старый файл, если он aviary ?
                    // Редактирование завершено. Сохраняемся.
                    if (imageUriIsBackground) {
                        updateBackground(imageUri);
                    } else {
                        updateAvatar(imageUri);
                    }
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            switch (requestCode) {
                case REQUEST_MAKE_BACKGROUND_PHOTO:
                case REQUEST_MAKE_AVATAR_PHOTO:
                    mMakePhotoDstUri = null;
                    break;
                case REQUEST_FEATHER_AVATAR_PHOTO:
                case REQUEST_FEATHER_BACKGROUND_PHOTO:
                    // Редактирование отменено, удаляем файл, если фотографировали
                    if (mMakePhotoDstUri != null) {
                        new File(mMakePhotoDstUri.getPath()).delete();
                        mMakePhotoDstUri = null;
                    }
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
            mMakePhotoDstUri = takePictureIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            startActivityForResult(takePictureIntent, requestCode);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(this, e.errorResourceId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        throw new IllegalStateException("ничего не удаляем");
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        boolean isBackground;
        Uri photoUri;
        if (DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag())) {
            isBackground = true;
            // XXX: background может быть null
            photoUri = Uri.parse(mDesign.getBackgroundUrl());
        } else {
            isBackground = false;
            // XXX: userpic может быть null
            photoUri = Uri.parse(mUser.getUserpic().originalUrl);
        }

        startFeatherPhoto(isBackground, photoUri);
    }

    private void startFeatherPhoto(boolean isBackground, Uri photoUri) {
        Intent featherPhotoIntent;
        int requestCode = isBackground ? REQUEST_FEATHER_BACKGROUND_PHOTO : REQUEST_FEATHER_AVATAR_PHOTO;

        try {
            featherPhotoIntent = ImageUtils.createFeatherPhotoIntent(this, photoUri);
            mMakePhotoDstUri = featherPhotoIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            featherPhotoIntent.putExtra(Constants.EXTRA_IN_SAVE_ON_NO_CHANGES, true);
            startActivityForResult(featherPhotoIntent, requestCode);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(this, e.errorResourceId, Toast.LENGTH_LONG).show();
        }
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
