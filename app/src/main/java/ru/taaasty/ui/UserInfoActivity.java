package ru.taaasty.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.rest.model.conversations.Conversation;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.messages.ConversationActivity;
import ru.taaasty.ui.post.PhotoSourceManager;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;
import ru.taaasty.utils.AnalyticsHelper;

public class UserInfoActivity extends ActivityBase implements UserInfoFragment.OnFragmentInteractionListener,
        SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final String TAG = "UserInfoActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String KEY_CURRENT_PHOTO_URI = "ru.taaasty.ui.UserInfoActivity";

    private static final String DIALOG_TAG_SELECT_BACKGROUND = "DIALOG_SELECT_BACKGROUND";
    private static final String DIALOG_TAG_SELECT_AVATAR = "DIALOG_SELECT_AVATAR";

    private static final int REQUEST_BACKGROUND_PHOTO = Activity.RESULT_FIRST_USER + 2;
    private static final int REQUEST_AVATAR_PHOTO = REQUEST_BACKGROUND_PHOTO + PhotoSourceManager.START_ACTIVITY_FOR_RESULT_REQUIRED_IDS;

    private static final int PERMISSION_REQUEST_BACKGROUND_PHOTO = 37;
    private static final int PERMISSION_REQUEST_AVATAR_PHOTO = PERMISSION_REQUEST_BACKGROUND_PHOTO + PhotoSourceManager.PERMISSION_REQUEST_REQUIRED_IDS;

    private static final String ARG_USER = "ru.taaasty.ui.UserInfoActivity.author";
    private static final String ARG_USER_ID = "ru.taaasty.ui.UserInfoActivity.author_id";
    private static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.UserInfoActivity.tlog_design";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "ru.taaasty.ui.UserInfoActivity.avatar_thumbnail_res";
    private static final String ARG_BACKGROUND_THUMBNAIL_KEY = "ru.taaasty.ui.UserInfoActivity.background_thumbnail_key";
    private static final String FRAGMENT_TAG_USER_INFO_FRAGMENT = "UserInfoFragment";

    private long mUserId;

    private PhotoSourceManager mBackgroundPhotoManager;

    private PhotoSourceManager mAvatarManager;

    public static class Builder {
        private final Context mContext;

        private View mSrcView;

        private Long mUserId;

        private User mUser;

        private TlogDesign mTlogDesign;

        private Integer mAvatarThumbnailSizeRes;

        private String mThumbnailBitmapCacheKey;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setUserId(long userId) {
            mUserId = userId;
            return this;
        }

        public Builder setUser(@Nullable User user) {
            mUser = user;
            return this;
        }

        public Builder setDesign(@Nullable TlogDesign design) {
            mTlogDesign = design;
            return this;
        }

        public Builder setSrcView(View view) {
            mSrcView = view;
            return this;
        }

        public Builder set(User user, @Nullable View srcView, @Nullable TlogDesign design) {
            setSrcView(srcView);
            setUser(user);
            setDesign(design);
            return this;
        }

        /**
         * Загружать и показывать миниатюру во время загрузки основной аватарки
         * @param sizeRes ID ресурса размера аватарки. Ставить значение аватарки, которая есть
         *                где-нибудь на экране и вероятнее всего будет в кэше памяти
         * @return
         */
        public Builder setPreloadAvatarThumbnail(int sizeRes) {
            mAvatarThumbnailSizeRes = sizeRes;
            return this;
        }

        public Builder setBackgroundThumbnailKey(@Nullable String cacheKey) {
            mThumbnailBitmapCacheKey = cacheKey;
            return this;
        }

        public Intent buildIntent() {
            if (mUserId == null && mUser == null) {
                throw new IllegalStateException("user not defined");
            }

            Intent intent = new Intent(mContext, UserInfoActivity.class);
            if (mUserId != null && mUser == null) {
                intent.putExtra(UserInfoActivity.ARG_USER_ID, (long)mUserId);
            }
            if (mUser != null) intent.putExtra(UserInfoActivity.ARG_USER, mUser);

            TlogDesign design = mTlogDesign;
            if (design == null && mUser != null) design = mUser.getDesign();
            if (design != null) intent.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);

            if (mAvatarThumbnailSizeRes != null) intent.putExtra(ARG_AVATAR_THUMBNAIL_RES, (int)mAvatarThumbnailSizeRes);

            if (mThumbnailBitmapCacheKey != null) intent.putExtra(ARG_BACKGROUND_THUMBNAIL_KEY, mThumbnailBitmapCacheKey);

            return intent;
        }

        public void startActivity() {
            Intent intent = buildIntent();
            if (mSrcView != null && (mContext instanceof Activity)) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                        mSrcView, 0, 0, mSrcView.getWidth(), mSrcView.getHeight());
                ActivityCompat.startActivity((Activity) mContext, intent, options.toBundle());
            } else {
                mContext.startActivity(intent);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User user;
        TlogDesign design;
        int avatarThumbnailRes;
        String backgroundThumbnailKey;

        setContentView(R.layout.activity_user_info);
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        mUserId = getIntent().getLongExtra(ARG_USER_ID, -1);
        user = getIntent().getParcelableExtra(ARG_USER);
        design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);
        avatarThumbnailRes = getIntent().getIntExtra(ARG_AVATAR_THUMBNAIL_RES, -1);
        backgroundThumbnailKey = getIntent().getStringExtra(ARG_BACKGROUND_THUMBNAIL_KEY);
        if (getIntent().hasExtra(ARG_USER_ID)) {
            mUserId = getIntent().getLongExtra(ARG_USER_ID, -1);
        } else {
            if (user == null) throw new IllegalArgumentException("no User ans user_id");
            mUserId = user.getId();
        }

        if (savedInstanceState == null) {
            Fragment userInfoFragment = UserInfoFragment.newInstance(mUserId, user, design, avatarThumbnailRes, backgroundThumbnailKey);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment, FRAGMENT_TAG_USER_INFO_FRAGMENT)
                    .commit();
        }
        mBackgroundPhotoManager = new PhotoSourceManager(this,
                "BackgroundPhoto",
                REQUEST_BACKGROUND_PHOTO,
                PERMISSION_REQUEST_BACKGROUND_PHOTO,
                findViewById(R.id.snackbar_container),
                this::updateBackground);
        mAvatarManager = new PhotoSourceManager(this,
                "AvatarPhoto",
                REQUEST_AVATAR_PHOTO,
                PERMISSION_REQUEST_AVATAR_PHOTO,
                findViewById(R.id.snackbar_container),
                this::updateAvatar);

        mBackgroundPhotoManager.onCreate(savedInstanceState);
        mAvatarManager.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_user_info, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isMyProfile = Session.getInstance().isMe(mUserId);
        if (!ViewConfiguration.get(this).hasPermanentMenuKey()) return false; // Бывает и такое
        menu.findItem(R.id.menu_change_avatar).setVisible(isMyProfile);
        menu.findItem(R.id.menu_change_background).setVisible(isMyProfile);

        String myRelationship = getMyRelationship();
        boolean meSubscribed = myRelationship != null && Relationship.isMeSubscribed(myRelationship);
        menu.findItem(R.id.menu_follow).setVisible(myRelationship != null && !meSubscribed && !isMyProfile);
        menu.findItem(R.id.menu_unfollow).setVisible(myRelationship != null && meSubscribed && !isMyProfile);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBackgroundPhotoManager.onActivityResult(requestCode, resultCode, data)) {
            mAvatarManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        UserInfoFragment fragment;
        String action;
        switch (item.getItemId()) {
            case R.id.menu_change_avatar:
                showChangeAvatarDialog();
                action = "Открыта смена аватара из сист. меню";
                break;
            case R.id.menu_change_background:
                showChangeBackgroundDialog();
                action = "Открыта смена фона из сист. меню";
                break;
            case R.id.menu_follow:
                fragment = (UserInfoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_USER_INFO_FRAGMENT);
                if (fragment != null) fragment.follow();
                action = "Подписка из сист. меню";
                break;
            case R.id.menu_unfollow:
                fragment = (UserInfoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_USER_INFO_FRAGMENT);
                if (fragment != null) fragment.unfollow();
                action = "Отписка из сист. меню";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        AnalyticsHelper.getInstance().sendUsersEvent(action);
        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mBackgroundPhotoManager.onSaveInstanceState(outState);
        mAvatarManager.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!mBackgroundPhotoManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            mAvatarManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onEntriesCountClicked(View view) {
        if (DBG) Log.v(TAG, "onEntriesCountClicked");
        TlogActivity.startTlogActivity(this, mUserId, view, R.dimen.feed_header_avatar_normal_diameter);
        AnalyticsHelper.getInstance().sendUsersEvent("Клик на кол-ве постов");
    }

    @Override
    public void onSelectBackgroundClicked() {
        showChangeBackgroundDialog();
        AnalyticsHelper.getInstance().sendUsersEvent("Открыта смена фона");
    }

    @Override
    public void onUserAvatarClicked(View view) {
        showChangeAvatarDialog();
        AnalyticsHelper.getInstance().sendUsersEvent("Открыта смена аватара");
    }

    @Override
    public void onInitiateConversationClicked(Conversation conversation) {
        ConversationActivity.startConversationActivity(this, conversation, null);
        AnalyticsHelper.getInstance().sendUsersEvent("Открыт диалог с пользователем");
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        int requestCode;
        boolean isBackgroundTarget = DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag());
        if (isBackgroundTarget) {
            mBackgroundPhotoManager.startPickPhoto();
        } else {
            mAvatarManager.startPickPhoto();
        }

        AnalyticsHelper.getInstance().sendUsersEvent("Открыт выбор изображения",
                isBackgroundTarget ? "фона" : "аватара");
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        Intent takePictureIntent;
        boolean isBackgroundTarget = DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag());
        if (isBackgroundTarget) {
            mBackgroundPhotoManager.startMakePhoto();
        } else {
            mAvatarManager.startMakePhoto();
        }

        AnalyticsHelper.getInstance().sendUsersEvent("Открыто фотографирование",
                isBackgroundTarget ? "фона" : "аватара");
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        throw new IllegalStateException("ничего не удаляем");
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        boolean isBackground;
        Uri photoUri;
        boolean isBackgroundTarget = DIALOG_TAG_SELECT_BACKGROUND.equals(fragment.getTag());
        if (isBackgroundTarget) {
            // XXX: background может быть null
            photoUri = Uri.parse(getDesign().getBackgroundUrl());
            mBackgroundPhotoManager.startFeatherPhoto(photoUri);
        } else {
            // XXX: userpic может быть null
            photoUri = Uri.parse(getUser().getUserpic().originalUrl());
            mAvatarManager.startFeatherPhoto(photoUri);
        }

        AnalyticsHelper.getInstance().sendUsersEvent("Открыто редактирование фото в aviary", isBackgroundTarget ? "фона" : "аватара");
    }

    private @Nullable TlogDesign getDesign() {
        UserInfoFragment uf = (UserInfoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_USER_INFO_FRAGMENT);
        if (uf != null) return uf.getDesign();
        return null;
    }

    private @Nullable User getUser() {
        UserInfoFragment uf = (UserInfoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_USER_INFO_FRAGMENT);
        if (uf != null) return uf.getUser();
        return null;
    }

    private @Nullable String getMyRelationship() {
        UserInfoFragment uf = (UserInfoFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_USER_INFO_FRAGMENT);
        if (uf != null) return uf.getMyRelationship();
        return null;
    }

    private void showChangeBackgroundDialog() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(DIALOG_TAG_SELECT_BACKGROUND) != null
                | fm.findFragmentByTag(DIALOG_TAG_SELECT_AVATAR) != null) {
            return;
        }
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
        dialog.show(getSupportFragmentManager(), DIALOG_TAG_SELECT_BACKGROUND);
    }

    private void showChangeAvatarDialog() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(DIALOG_TAG_SELECT_BACKGROUND) != null
                | fm.findFragmentByTag(DIALOG_TAG_SELECT_AVATAR) != null) {
            return;
        }
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(false);
        dialog.show(getSupportFragmentManager(), DIALOG_TAG_SELECT_AVATAR);
    }

    void updateBackground(Uri imageUri) {
        IntentService.startUploadBackground(this, mUserId, imageUri);
        EventBus.getDefault().post(TlogBackgroundUploadStatus.createUploadStarted(mUserId, imageUri));
    }

    void updateAvatar(Uri imageUri) {
        IntentService.startUploadUserpic(this, mUserId, imageUri);
        EventBus.getDefault().post(UserpicUploadStatus.createUploadStarted(mUserId, imageUri));
    }
}