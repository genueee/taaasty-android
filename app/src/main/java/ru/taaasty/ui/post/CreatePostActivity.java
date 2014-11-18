package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UploadService;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.model.PostForm;
import ru.taaasty.widgets.CreatePostButtons;
import ru.taaasty.widgets.ErrorTextView;

public class CreatePostActivity extends ActivityBase implements OnCreatePostInteractionListener, SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreatePostActivity";

    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED = Activity.RESULT_FIRST_USER;
    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN = Activity.RESULT_FIRST_USER + 1;

    private static final String SHARED_PREFS_NAME = "CreatePostActivity";
    private static final String SHARED_PREFS_KEY_PRIVATE_POST_STATUS = "provate_post_status";
    private static final String SHARED_PREFS_KEY_INITIAL_SECTION = "initial_section";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private CreatePostButtons mCreatePostButtons;
    private ImageView mCreatePostButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DBG) Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_create_post);

        Page currentItem = Page.TEXT_POST;
        boolean postPrivate = false;

        mSectionsPagerAdapter = new SectionsPagerAdapter(this, getFragmentManager());

        if (savedInstanceState == null) {
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
        mViewPager.setOffscreenPageLimit(3);
        mCreatePostButtons = (CreatePostButtons)findViewById(R.id.buttons);
        mCreatePostButtons.setOnItemClickListener(mCreatePostButtonsListener);
        mCreatePostButtons.findViewById(R.id.private_post_indicator).setActivated(postPrivate);

        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_create_post);
            ab.setTitle(currentItem.titleViewId);

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
        mCreatePostButtons.setActivated(currentItem.buttonViewId);

        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        saveState();
        EventBus.getDefault().unregister(this);
    }

    void onCreatePostClicked() {
        PostForm post;
        CreatePostFragmentBase fragment;

        fragment = (CreatePostFragmentBase)mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
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

    public void onEventMainThread(EntryUploadStatus status) {
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
    public void onPickPhotoSelected(Fragment fragment) {
        if (DBG) Log.v(TAG, "onPickPhotoSelected");
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onPickPhotoSelected();
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        if (DBG) Log.v(TAG, "onMakePhotoSelected");
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onMakePhotoSelected();
    }

    @Override
    public void onDeletePhotoSelected(Fragment sourceFragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onDeleteImageClicked();
    }

    @Override
    public void onFeatherPhotoSelected(Fragment sourceFragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onFeatherPhotoClicked();
    }

    private CreateImagePostFragment getCurrentImagePostFragment() {
        if (mSectionsPagerAdapter != null) {
            Fragment fragment = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
            if (fragment instanceof  CreateImagePostFragment) {
                return (CreateImagePostFragment)fragment;
            } else {
                if (DBG) throw new IllegalStateException();
            }
        }
        return null;
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
