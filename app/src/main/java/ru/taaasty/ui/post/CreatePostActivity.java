package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UploadService;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.model.PostEntry;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.widgets.CreatePostButtons;
import ru.taaasty.widgets.ErrorTextView;

public class CreatePostActivity extends ActivityBase implements OnCreatePostInteractionListener, SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreatePostActivity";

    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED = Activity.RESULT_FIRST_USER;
    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN = Activity.RESULT_FIRST_USER + 1;

    private static final int REQUEST_PICK_PHOTO = Activity.RESULT_FIRST_USER + 2;
    private static final int REQUEST_MAKE_PHOTO = Activity.RESULT_FIRST_USER + 3;

    private static final String SHARED_PREFS_NAME = "CreatePostActivity";
    private static final String SHARED_PREFS_KEY_PRIVATE_POST_STATUS = "provate_post_status";
    private static final String SHARED_PREFS_KEY_INITIAL_SECTION = "initial_section";


    private static final String KEY_CURRENT_PHOTO_URI = "ru.taaasty.ui.post.KEY_CURRENT_PHOTO_URI";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private CreatePostButtons mCreatePostButtons;
    private ImageView mCreatePostButton;

    private Uri mCurrentPhotoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_post);

        Page currentItem = Page.TEXT_POST;
        boolean postPrivate = false;

        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getFragmentManager());

        if (savedInstanceState != null) {
            mCurrentPhotoUri = savedInstanceState.getParcelable(KEY_CURRENT_PHOTO_URI);
        } else {
            // Восстанавливаем значения последнего поста
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
            postPrivate = prefs.getBoolean(SHARED_PREFS_KEY_PRIVATE_POST_STATUS, false);
            String currentItemString = prefs.getString(SHARED_PREFS_KEY_INITIAL_SECTION, null);
            if (currentItemString != null) {
                currentItem = Page.valueOfPrefsName(currentItemString);
            }
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOnPageChangeListener(mOnPageChangedListener);
        mViewPager.setPageTransformer(true, new FadePageTransformer());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mCreatePostButtons = (CreatePostButtons)findViewById(R.id.buttons);
        mCreatePostButtons.setOnItemClickListener(mCreatePostButtonsListener);
        mCreatePostButtons.findViewById(R.id.private_post_indicator).setActivated(postPrivate);

        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.create_post_ab_custom_view);

            mCreatePostButton = (ImageView)ab.getCustomView().findViewById(R.id.create_post_button);
            mCreatePostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCreatePostClicked();
                }
            });
            mCreatePostButton.setEnabled(false);
        }
        mViewPager.setCurrentItem(currentItem.ordinal(), false);

        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri imageUri = null;

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_PHOTO:
                    Uri selectedImageUri = data.getData();
                    if (DBG) Log.v(TAG,"image uri: " + selectedImageUri);
                    imageUri = selectedImageUri;
                    break;
                case REQUEST_MAKE_PHOTO:
                    if (DBG) Log.v(TAG,"image uri: " + mCurrentPhotoUri);
                    ImageUtils.galleryAddPic(this, mCurrentPhotoUri);
                    imageUri = mCurrentPhotoUri;
                    mCurrentPhotoUri = null;
            }
        }
        if (imageUri != null && mSectionsPagerAdapter != null) {
            Fragment fragment = mSectionsPagerAdapter.getCurrentPrimaryItem();
            if (fragment instanceof  CreateImagePostFragment) {
                ((CreateImagePostFragment)fragment).onImageSelected(imageUri);
            }
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveState();
        EventBus.getDefault().unregister(this);
    }

    void onCreatePostClicked() {
        PostEntry post;
        CreatePostFragmentBase fragment;

        fragment = mSectionsPagerAdapter.getCurrentPrimaryItem();
        if (!fragment.isFormValid()) {
            // XXX: предупреждать юзера?
            return;
        }
        post = fragment.getForm();
        post.setIsPrivate(isPostPrivate());
        UploadService.startPostEntry(this, post);
        setUploadingStatus(true);
    }

    public boolean isPostPrivate() {
        return findViewById(R.id.private_post_indicator).isActivated();
    }

    public void onEventMainThread(PostUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully) {
            // Переходим на страницу, в зависимости от статуса блокировки
            Toast.makeText(this, R.string.post_created, Toast.LENGTH_LONG).show();
            if (status.entry.isPrivate()) {
                // Пост приватный. Переход к скрытым записям
                setResult(CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN);
            } else {
                // Пост публичный, переход в мой дневник
                setResult(CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED);
            }
            finish();
        } else {
            // Сообщаем об ошибке
            setUploadingStatus(false);
            notifyError(status.error, status.exception);
        }
    }

    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    private void setUploadingStatus(boolean uploading) {
        View progress = findViewById(R.id.progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mViewPager.setVisibility(uploading ? View.INVISIBLE : View.VISIBLE);
        mCreatePostButtons.setVisibility(uploading ? View.INVISIBLE : View.VISIBLE);
        mCreatePostButton.setEnabled(!uploading);
    }

    private final CreatePostButtons.onCreatePostButtonsListener mCreatePostButtonsListener = new CreatePostButtons.onCreatePostButtonsListener() {
        @Override
        public void onCreatePostButtonClicked(View v) {
           switch (v.getId()) {
               case R.id.private_post_indicator:
                   v.setActivated(!v.isActivated());
                   break;
               default:
                   if (v.isActivated()) return;
                   mViewPager.setCurrentItem(Page.valueOfButtonViewId(v.getId()).ordinal(), true);
           }
        }
    };

    private final ViewPager.OnPageChangeListener mOnPageChangedListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int i, float v, int i2) {

        }

        @Override
        public void onPageSelected(int i) {
            Page selected = Page.values()[i];
            mCreatePostButtons.setActivated(selected.buttonViewId);
            getActionBar().setTitle(selected.titleViewId);
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    @Override
    public void onValidationStatusChanged(boolean postValid) {
        // Изменилась валидность данных формы. Обновляем статус кнопки создания поста
        mCreatePostButton.setEnabled(postValid);
    }

    @Override
    public void onChoosePhotoButtonClicked(boolean hasPicture) {
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(hasPicture);
        dialog.show(getFragmentManager(), "SelectPhotoSourceDialogFragment");
    }

    @Override
    public void onPickPhotoSelected() {
        if (DBG) Log.v(TAG, "onPickPhotoSelected");
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent
                , REQUEST_PICK_PHOTO);
    }

    @Override
    public void onMakePhotoSelected() {
        File storageDir;
        Intent takePictureIntent;
        String imageFileName;
        Date currentDate;
        File image;

        if (DBG) Log.v(TAG, "onMakePhotoSelected");
        takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, R.string.error_camera_not_available, Toast.LENGTH_LONG).show();
            return;
        }
        storageDir = ImageUtils.getPicturesDirectory(this);
        if (storageDir == null) {
            Toast.makeText(this, R.string.error_no_place_to_save, Toast.LENGTH_LONG).show();
            return;
        }
        currentDate = new Date();
        imageFileName = ImageUtils.getOutputMediaFileName(currentDate);
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            mCurrentPhotoUri = Uri.fromFile(image);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);

            startActivityForResult(takePictureIntent, REQUEST_MAKE_PHOTO);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_can_not_create_file_for_photo, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDeletePhotoSelected() {
        if (mSectionsPagerAdapter != null) {
            Fragment fragment = mSectionsPagerAdapter.getCurrentPrimaryItem();
            if (fragment instanceof  CreateImagePostFragment) {
                ((CreateImagePostFragment)fragment).onDeleteImageClicked();
            }
        }
    }

    private void saveState() {
        getSharedPreferences(SHARED_PREFS_NAME, 0)
                .edit()
                .putBoolean(SHARED_PREFS_KEY_PRIVATE_POST_STATUS, isPostPrivate())
                .putString(SHARED_PREFS_KEY_INITIAL_SECTION,
                        Page.values()[mViewPager.getCurrentItem()].namePrefs)
                .commit();
    }
}
