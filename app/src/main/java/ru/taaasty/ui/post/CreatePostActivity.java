package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.widgets.CreatePostButtons;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class CreatePostActivity extends ActivityBase implements OnCreatePostInteractionListener,
        SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener,
        CreateEmbeddPostFragment.InteractionListener,
        EmbeddMenuDialogFragment.OnDialogInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreatePostActivity";

    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED = Activity.RESULT_FIRST_USER;
    public static final int CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN = Activity.RESULT_FIRST_USER + 1;

    private static final String ARG_TLOG_ID = "ru.taaasty.ui.post.CreatePostActivity.ARG_TLOG_ID";
    private static final String ARG_PAGE = "ru.taaasty.ui.post.CreatePostActivity.ARG_PAGE";
    private static final String KEY_TLOG = "TLOG";
    private static final String KEY_CURRENT_PAGE = "ru.taaasty.ui.post.CreatePostActivity.CURRENT_PAGE";

    private static final String SHARED_PREFS_NAME = "CreatePostActivity";
    private static final String SHARED_PREFS_KEY_POST_PRIVACY = "post_privacy";
    private static final String SHARED_PREFS_KEY_INITIAL_SECTION = "initial_section";

    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private CreatePostButtons mCreatePostButtons;
    private ImageView mCreatePostButton;

    private Long mTlogId;

    private Subscription mTlogInfoSubscription = Subscriptions.empty();

    @Nullable
    private TlogInfo mTlog;

    private Set<CreatePostFragmentBase> mCreatePostFragments = new HashSet<>(4);


    public static void startCreatePostActivityForResult(Context context,
                                                        Object activityOrFragment,
                                                        int requestCode) {
        startCreatePostActivityForResult(context, activityOrFragment, null, null, requestCode);
    }

    public static void startCreatePostActivityForResult(Context context, Object activityOrFragment,
                                                        Long tlogId,
                                                        Page page,
                                                        int requestCode) {
        Intent intent = new Intent(context, CreatePostActivity.class);
        if (tlogId != null) {
            intent.putExtra(ARG_TLOG_ID, tlogId.longValue());
        }
        if (page != null) {
            intent.putExtra(ARG_PAGE, page.ordinal());
        }

        if (activityOrFragment instanceof Fragment) {
            ((Fragment)activityOrFragment).startActivityForResult(intent, requestCode);
        } else if (activityOrFragment instanceof Activity) {
            ((Activity)activityOrFragment).startActivityForResult(intent, requestCode);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DBG) Log.v(TAG, "onCreate()");
        setContentView(R.layout.activity_create_post);

        Page currentItem;
        @Entry.EntryPrivacy
        String postPrivacy = Entry.PRIVACY_PUBLIC;

        if (getIntent().hasExtra(ARG_TLOG_ID)) {
            mTlogId = getIntent().getLongExtra(ARG_TLOG_ID, 0);
        } else {
            mTlogId = null;
        }

        if (getIntent().hasExtra(ARG_PAGE)) {
            currentItem = Page.values()[getIntent().getIntExtra(ARG_PAGE, 0)];
        } else {
            currentItem = Page.TEXT_POST;
        }

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), mTlogId);

        if (savedInstanceState == null) {
            // Восстанавливаем значения последнего поста
            SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
            //noinspection ResourceType
            postPrivacy = prefs.getString(SHARED_PREFS_KEY_POST_PRIVACY, Entry.PRIVACY_PUBLIC);
            if (!Entry.PRIVACY_PUBLIC.equals(postPrivacy)
                    && !Entry.PRIVACY_PRIVATE.equals(postPrivacy)
                    && !Entry.PRIVACY_PUBLIC_WITH_VOTING.equals(postPrivacy)
                    ) {
                postPrivacy = Entry.PRIVACY_PUBLIC;
            }
            String currentItemString = prefs.getString(SHARED_PREFS_KEY_INITIAL_SECTION, null);
            if (currentItemString != null && !getIntent().hasExtra(ARG_PAGE)) {
                currentItem = Page.valueOfPrefsName(currentItemString);
            }
        } else {
            mTlog = savedInstanceState.getParcelable(KEY_TLOG);
            currentItem = Page.values()[savedInstanceState.getInt(KEY_CURRENT_PAGE)];
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.addOnPageChangeListener(mOnPageChangedListener);
        mViewPager.setPageTransformer(true, new FadePageTransformer());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mCreatePostButtons = (CreatePostButtons)findViewById(R.id.buttons);
        mCreatePostButtons.setOnItemClickListener(mCreatePostButtonsListener);
        mCreatePostButtons.setPrivacy(postPrivacy);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTitle(currentItem.titleViewId);

        mCreatePostButton = (ImageView) findViewById(R.id.create_post_button);
        mCreatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreatePostClicked();
            }
        });
        mCreatePostButton.setEnabled(false);

        mViewPager.setCurrentItem(currentItem.ordinal(), false);
        mCreatePostButtons.setActivated(currentItem.buttonViewId);

        EventBus.getDefault().register(this);

        if (mTlogId != null) {
            if (mTlog != null) {
                setupTitle();
            } else {
                Observable<TlogInfo> observable = RestClient.getAPiTlog().getUserInfo(String.valueOf(mTlogId));
                mTlogInfoSubscription = observable
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(mTlogInfoObserver);
            }
        }

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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DBG) Log.v(TAG, "onNewIntent " + intent);
        setIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        saveState();
        EventBus.getDefault().unregister(this);
        mTlogInfoSubscription.unsubscribe();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTlog != null) outState.putParcelable(KEY_TLOG, mTlog);
        outState.putInt(KEY_CURRENT_PAGE, mViewPager.getCurrentItem());
    }

    void onCreatePostClicked() {
        PostForm post;
        CreatePostFragmentBase fragment;

        fragment = getVisibleFragment();
        if (!fragment.isFormValid()) {
            // XXX: предупреждать юзера?
            return;
        }
        post = fragment.getForm();
        post.privacy = mCreatePostButtons.getPrivacy();
        IntentService.startPostEntry(this, post);
        setUploadingStatus(true);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully) {
            // Переходим на страницу, в зависимости от статуса блокировки
            Toast.makeText(this, R.string.post_created, Toast.LENGTH_LONG).show();
            if (status.entry.isPrivatePost()) {
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
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            ert.setError(error, exception);
        }
    }

    private void setUploadingStatus(boolean uploading) {
        View progress = findViewById(R.id.create_post_progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mViewPager.setVisibility(uploading ? View.INVISIBLE : View.VISIBLE);
        mCreatePostButtons.setVisibility(uploading ? View.INVISIBLE : View.VISIBLE);
        mCreatePostButton.setEnabled(!uploading);
    }

    private final CreatePostButtons.onCreatePostButtonsListener mCreatePostButtonsListener = new CreatePostButtons.onCreatePostButtonsListener() {
        @Override
        public void onCreatePostButtonClicked(View v) {
            if (v.isActivated()) return;
            mViewPager.setCurrentItem(Page.valueOfButtonViewId(v.getId()).ordinal(), true);
        }
    };

    private void setupTitle() {
        if (mTlog == null) return;
        setTitle("#" + mTlog.author.getName());
    }

    private final ViewPager.OnPageChangeListener mOnPageChangedListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageScrolled(int i, float v, int i2) {

        }

        @Override
        public void onPageSelected(int i) {
            Page selected = Page.values()[i];
            mCreatePostButtons.setActivated(selected.buttonViewId);
            if (mTlogId == null) setTitle(selected.titleViewId);
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
        dialog.show(getSupportFragmentManager(), "SelectPhotoSourceDialogFragment");
    }

    @Override
    public void onFragmentAttached(CreatePostFragmentBase fragment) {
        mCreatePostFragments.add(fragment);
    }

    @Override
    public void onFragmentDetached(CreatePostFragmentBase fragment) {
        mCreatePostFragments.remove(fragment);
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
            Fragment fragment = getVisibleFragment();
            if (fragment instanceof CreateImagePostFragment) {
                return (CreateImagePostFragment)fragment;
            } else {
                if (DBG) throw new IllegalStateException();
            }
        }
        return null;
    }

    @Nullable
    private CreateEmbeddPostFragment getCurrentEmbeddPostFragment() {
        if (mSectionsPagerAdapter != null) {
            Fragment fragment = getVisibleFragment();
            if (fragment instanceof CreateEmbeddPostFragment) {
                return (CreateEmbeddPostFragment)fragment;
            } else {
                if (DBG) throw new IllegalStateException();
            }
        }
        return null;
    }

    @Nullable
    private CreatePostFragmentBase getVisibleFragment() {
        CreatePostFragmentBase result = null;
        for (CreatePostFragmentBase fragment : mCreatePostFragments) {
            if (fragment.getUserVisibleHint()) {
                result = fragment;
                break;
            }
        }

        if (DBG) Log.d(TAG, "getVisibleFragment() result: " + result);

        return result;
    }

    private void saveState() {
        getSharedPreferences(SHARED_PREFS_NAME, 0)
                .edit()
                .putString(SHARED_PREFS_KEY_POST_PRIVACY, mCreatePostButtons.getPrivacy())
                .putString(SHARED_PREFS_KEY_INITIAL_SECTION,
                        Page.values()[mViewPager.getCurrentItem()].namePrefs)
                .commit();
    }

    @Override
    public void doShowEmbeddMenuDialog(EmbeddMenuDialogFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment old = fm.findFragmentByTag("EmbeddMenuDialogFragment");
        FragmentTransaction ft = fm.beginTransaction();
        if (old != null) ft.remove(old);
        fragment.show(ft, "EmbeddMenuDialogFragment");
    }

    @Override
    public void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId) {
        CreateEmbeddPostFragment fragment = getCurrentEmbeddPostFragment();
        if (fragment != null) fragment.onEmbeddMenuDialogItemSelected(dialog, resId);
    }

    @Override
    public void onEmbeddMenuDialogDismissed(DialogInterface dialog) {
        CreateEmbeddPostFragment fragment = getCurrentEmbeddPostFragment();
        if (fragment != null) fragment.onEmbeddMenuDialogDismissed(dialog);
    }

    private final rx.Observer<TlogInfo> mTlogInfoObserver = new rx.Observer<TlogInfo>() {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onNext(TlogInfo tlogInfo) {
            mTlog = tlogInfo;
            setupTitle();
        }
    };

}
