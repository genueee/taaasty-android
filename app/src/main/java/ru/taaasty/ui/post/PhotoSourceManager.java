package ru.taaasty.ui.post;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.adobe.creativesdk.aviary.AdobeImageIntent;

import java.io.File;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.MessageHelper;

public class PhotoSourceManager {

    public static final int START_ACTIVITY_FOR_RESULT_REQUIRED_IDS = 5;

    public static final int PERMISSION_REQUEST_REQUIRED_IDS = 5;

    private static final int REQUEST_PICK_PHOTO = 0;
    private static final int REQUEST_MAKE_PHOTO = 1;
    private static final int REQUEST_FEATHER_PHOTO = 2;

    private static final boolean REMOVE_ORIGINAL_AFTER_EDIT = true;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_MAKE_PHOTO = 0;

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FEATHER_PHOTO = 1;

    private static final String KEY_MAKE_PHOTO_DST_URI = "ru.taaasty.ui.post.PhotoSourceManager.KEY_MAKE_PHOTO_DST_URI";
    private static final String KEY_FEATHER_PHOTO_URI = "ru.taaasty.ui.post.PhotoSourceManager.KEY_FEATHER_PHOTO_URI";
    private static final String KEY_IMAGE_EDITOR_EXTRAS_ORIGINAL_URI = "ru.taaasty.ui.post.PhotoSourceManager.KEY_IMAGE_EDITOR_EXTRAS_ORIGINAL_URI";
    private static final String KEY_IMAGE_EDITOR_DISPATCH_ORIGINAL_URI_ON_FAIL = "ru.taaasty.ui.post.PhotoSourceManager.KEY_IMAGE_EDITOR_DISPATCH_ORIGINAL_URI_ON_FAIL";

    private static final String TAG = "PhotoSourceManager";
    private static final boolean DBG = BuildConfig.DEBUG;

    @Nullable
    private final Fragment mFragment;

    @Nullable
    private final Activity mActivity;

    private final String mUniqPrefix;

    private final int mStartActivityForResultBase;

    private final int mRequestPermissionBase;

    @Nullable
    private final View mSnackbarRootView;

    private final Callbacks mCallbacks;

    // URI, который передан в MediaStore.EXRA_OUTPUT_PATH при вызове приложения камеры.
    @Nullable
    private Uri mMakePhotoDstUri;

    // URI фотографии, для которой запрошено редактирование. Только для сохранения если вдруг
    // приложение будет перезапущено во время запроса необходимых разрешений
    @Nullable
    private Uri mFeatherPhotoUri;

    public interface Callbacks {
        void onNewImageUriReceived(Uri uri);
    }

    public PhotoSourceManager(Fragment fragment, String uniqPrefix, Callbacks callbacks) {
        this(fragment, uniqPrefix, Activity.RESULT_FIRST_USER+100, 35683, null, callbacks);
    }

    public PhotoSourceManager(Object fragmentOrActivity,
                              @Nullable String uniqPrefix,
                              int startActivityFoResultBase,
                              int permissionRequestBase,
                              @Nullable View snackbarRootView,
                              Callbacks callbacks) {
        if (fragmentOrActivity instanceof Fragment) {
            mFragment = (Fragment)fragmentOrActivity;
            mActivity = null;
        } else if (fragmentOrActivity instanceof Activity) {
            mFragment = null;
            mActivity = (Activity)fragmentOrActivity;
        } else {
            throw new IllegalArgumentException("Should be Fragment or Activity");
        }
        mCallbacks = callbacks;
        mUniqPrefix = uniqPrefix == null ? "" : uniqPrefix;
        mStartActivityForResultBase = startActivityFoResultBase;
        mSnackbarRootView = snackbarRootView;
        mRequestPermissionBase = permissionRequestBase;
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mFeatherPhotoUri = savedInstanceState.getParcelable(mUniqPrefix + KEY_FEATHER_PHOTO_URI);
            mMakePhotoDstUri = savedInstanceState.getParcelable(mUniqPrefix + KEY_MAKE_PHOTO_DST_URI);
        }
    }

    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri newUri;

        switch (requestCode - mStartActivityForResultBase) {
            case REQUEST_PICK_PHOTO: // Выбрали фотографию из галереи
                if (resultCode == Activity.RESULT_OK) {
                    newUri = data.getData();
                    if (DBG) Log.v(TAG, "Image picked. Uri: " + newUri);
                    if (hasExternalStoragePermission()) {
                        startFeatherPhotoAfterPermissionGranted(newUri, true);
                    } else {
                        dispatchNewImageUriReceived(newUri);
                    }
                }
                return true;
            case REQUEST_MAKE_PHOTO: // Сфотографировались
                if (resultCode == Activity.RESULT_OK) {
                    newUri = mMakePhotoDstUri;
                    mMakePhotoDstUri = null;
                    if (DBG)
                        Log.v(TAG, "Take picture complete uri: " + newUri + " intent data uri: " + data.getData());
                    ImageUtils.galleryUpdatePic(getContext(), newUri);
                    if (hasExternalStoragePermission()) {
                        startFeatherPhotoAfterPermissionGranted(newUri, true);
                    } else {
                        // Мы не можем попасть сюда без разрешений, мы должны были их получить до фотографирования.
                        // Разве что, у нас их отобрали во время процесса
                        if (DBG)
                            Log.e(TAG, "Has photo but no permissions", new IllegalStateException());
                        dispatchNewImageUriReceived(newUri);
                    }
                }
                break;
            case REQUEST_FEATHER_PHOTO: // отредактировали фотографию
                boolean changed = false;
                Uri originalUri = null;
                boolean dispatchOriginalUriOnFail = false;

                if (data != null && data.getExtras() != null) {
                    Bundle extra = data.getExtras();
                    changed = extra.getBoolean(AdobeImageIntent.EXTRA_OUT_BITMAP_CHANGED);
                    Bundle myExtras = extra.getBundle(AdobeImageIntent.EXTRA_IN_EXTRAS);
                    if (myExtras != null) {
                        originalUri = myExtras.getParcelable(mUniqPrefix + KEY_IMAGE_EDITOR_EXTRAS_ORIGINAL_URI);
                        dispatchOriginalUriOnFail = myExtras.getBoolean(mUniqPrefix + KEY_IMAGE_EDITOR_DISPATCH_ORIGINAL_URI_ON_FAIL);
                    }
                }

                if (resultCode == Activity.RESULT_OK) {
                    newUri = data.getData();
                    if (newUri.toString().startsWith("/")) {
                        newUri = Uri.fromFile(new File(newUri.toString())); // Мозгоблядство от aviary
                    }

                    if (changed) {
                        // Пользователь внес какие-то изменения и нажал "готово"
                        dispatchNewImageUriReceived(newUri);
                        if (!ru.taaasty.utils.Objects.equals(newUri, originalUri)) {
                            if (newUri != null) ImageUtils.galleryUpdatePic(getContext(), newUri);
                            if (ImageUtils.isUriInPicturesDirectory(getContext(), originalUri) && REMOVE_ORIGINAL_AFTER_EDIT) {
                                if (deleteFileNoThrow(originalUri)) {
                                    ImageUtils.galleryUpdatePic(getContext(), originalUri);
                                    if (DBG) Log.i(TAG, "File removed after edit: " + originalUri);
                                }
                            }
                        }
                    } else {
                        // Пользователь нажал "готово", но при этом в исходное изображение не внес никаких изменений
                        // Если мы редактируем фотку после выбора из галереи или фотографирования,
                        // то возвращаем урл, полученный там.
                        if (dispatchOriginalUriOnFail) {
                            dispatchNewImageUriReceived(originalUri);
                            // Файл, созданный aviary, нам не понадобится
                            if (deleteFileNoThrow(newUri)) {
                                ImageUtils.galleryUpdatePic(getContext(), newUri);
                                if (DBG) Log.i(TAG, "File removed after edit: " + newUri);
                            }
                        } else {
                            // Если мы редактируем имеющуюся фотку, то ничего никуда не возвращаем

                        }
                    }

                    if (DBG) Log.v(TAG, "Feather photo complete. Original URI: " + originalUri +
                            " new image uri: " + newUri + " bitmap changed: " + changed);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // Пользователь отменил редактирование
                    if (DBG) {
                        Log.v(TAG, "edit RESULT_CANCELED.");
                        // TODO удалять фотографию?
                    }
                }
                return true;
        }

        return false;
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode - mRequestPermissionBase) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_MAKE_PHOTO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startMakePhotoAfterPermissionGranted();
                    AnalyticsHelper.getInstance().sendEvent(Constants.ANALYTICS_CATEGORY_PERMISSIONS, "GRANTED", "WRITE_EXTERNAL_STORAGE");
                } else {
                    showNoExternalPhotoPermissionSnackbar(v -> startMakePhoto());
                    AnalyticsHelper.getInstance().sendEvent(Constants.ANALYTICS_CATEGORY_PERMISSIONS, "DENIED", "WRITE_EXTERNAL_STORAGE");
                }
                return true;
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FEATHER_PHOTO:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startFeatherPhotoAfterPermissionGranted(mFeatherPhotoUri, false);
                    AnalyticsHelper.getInstance().sendEvent(Constants.ANALYTICS_CATEGORY_PERMISSIONS, "GRANTED", "WRITE_EXTERNAL_STORAGE");
                } else {
                    showNoExternalPhotoPermissionSnackbar(v -> startFeatherPhoto(mFeatherPhotoUri));
                    AnalyticsHelper.getInstance().sendEvent(Constants.ANALYTICS_CATEGORY_PERMISSIONS, "DENIED", "WRITE_EXTERNAL_STORAGE");
                }
                return true;
        }
        return false;
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mMakePhotoDstUri != null)
            outState.putParcelable(mUniqPrefix + KEY_MAKE_PHOTO_DST_URI, mMakePhotoDstUri);
        if (mFeatherPhotoUri != null)
            outState.putParcelable(mUniqPrefix + KEY_FEATHER_PHOTO_URI, mFeatherPhotoUri);
    }

    /**
     * Выбор фотографии из галереи.
     * После выбора открывается редактор
     */
    public void startPickPhoto() {
        Intent photoPickerIntent = ImageUtils.createPickImageActivityIntent();
        startActivityForResult(photoPickerIntent, REQUEST_PICK_PHOTO);
    }

    /**
     * Фотографирование.
     * По окончании запускается редактор
     */
    public void startMakePhoto() {
        if (!hasExternalStoragePermission()) {
            requestExternalStoragePermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_MAKE_PHOTO);
        } else {
            startMakePhotoAfterPermissionGranted();
        }
    }

    /**
     * Редактирование фотографии
     * @param imageUri URI фотографии, которую редактируем
     */
    public void startFeatherPhoto(Uri imageUri) {
        mFeatherPhotoUri = imageUri;
        if (!hasExternalStoragePermission()) {
            requestExternalStoragePermission(PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE_FEATHER_PHOTO);
        } else {
            startFeatherPhotoAfterPermissionGranted(imageUri, false);
        }
    }

    private Context getContext() {
        return mFragment != null ? mFragment.getContext() : mActivity;
    }

    private void startActivityForResult(Intent intent, int requestCode) {
        if (mFragment != null) {
            mFragment.startActivityForResult(intent, requestCode + mStartActivityForResultBase);
        } else {
            assert mActivity != null;
            mActivity.startActivityForResult(intent, requestCode + mStartActivityForResultBase);
        }
    }

    private void requestExternalStoragePermission(int requestCode) {
        if (mFragment != null) {
            mFragment.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode + mRequestPermissionBase);
        } else {
            assert mActivity != null;
            ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode + mRequestPermissionBase);
        }
    }

    private boolean hasExternalStoragePermission() {
        return (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private void startMakePhotoAfterPermissionGranted() {
        try {
            Intent takePictureIntent;
            takePictureIntent = ImageUtils.createMakePhotoIntent(getContext(), false);
            mMakePhotoDstUri = takePictureIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            startActivityForResult(takePictureIntent, REQUEST_MAKE_PHOTO);
        } catch (ImageUtils.MakePhotoException e) {
            MessageHelper.showError(getContext(), getContext().getText(e.errorResourceId), e);
        } catch (SecurityException e) {
            // Реально возможно, не параноя.
            MessageHelper.showError(getContext(), getContext().getText(R.string.error_no_photo_dir_permission), e);
        }
    }

    private void dispatchNewImageUriReceived(Uri uri) {
        if (DBG) Log.d(TAG, "dispatchNewImageUriReceived() called with: " +  "uri = [" + uri + "]");
        mCallbacks.onNewImageUriReceived(uri);
    }

    /**
     * Редактирование фотографии. Считаем, что у нас есть все разрешения
     * @param imageUri URI изображения, которое надо отредактировать
     * @param dispatchUriOnFail если true, то в случае ошибки или отмены imageUri
     *                         возвращен как новый URL.
     *                         Используется после фотографирования/выбора из галереи: "у нас уже
     *                         есть новоре изображение, мы пытаемся его отредактировать, но если у
     *                         нас ничего не выходит - возвращаем то, что есть"
     */
    private void startFeatherPhotoAfterPermissionGranted(Uri imageUri, boolean dispatchUriOnFail) {
        boolean failed = false;
        try {
            Bundle extras = new Bundle(1);
            extras.putParcelable(mUniqPrefix + KEY_IMAGE_EDITOR_EXTRAS_ORIGINAL_URI, imageUri);
            extras.putBoolean(mUniqPrefix + KEY_IMAGE_EDITOR_DISPATCH_ORIGINAL_URI_ON_FAIL, dispatchUriOnFail);
            Intent newIntent = ImageUtils.createFeatherPhotoIntent(getContext(), imageUri, extras);
            startActivityForResult( newIntent, REQUEST_FEATHER_PHOTO);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(getContext(), e.errorResourceId, Toast.LENGTH_LONG).show();
            failed = true;
        } catch (SecurityException e) {
            MessageHelper.showError(getContext(), getContext().getText(R.string.error_no_photo_dir_permission), e);
            failed = true;
        }
        if (failed && dispatchUriOnFail) dispatchNewImageUriReceived(imageUri);
    }


    private void showNoExternalPhotoPermissionSnackbar(View.OnClickListener retryClickedAction) {
        View rootView;
        boolean showRetryButton = false;
        if (mSnackbarRootView != null) {
            rootView = mSnackbarRootView;
        } else if (mFragment != null) {
            rootView = mFragment.getView();
        } else {
            rootView = null;
        }

        if (mFragment != null) {
            showRetryButton = mFragment.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            assert mActivity != null;
            showRetryButton = ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (rootView == null) return;
        Snackbar snackbar = Snackbar.make(rootView, R.string.error_external_photo_dir_permission_required, Snackbar.LENGTH_LONG);
        if (showRetryButton) {
            snackbar.setAction(R.string.grant_access_to_external_photo_dir_button, retryClickedAction);
        }
        snackbar.show();
    }

    private static boolean deleteFileNoThrow(Uri uri) {
        try {
            return (new File(uri.getPath())).delete();
        } catch (Throwable e) {
            if (DBG) Log.e(TAG, "File.delete() error", e);
            return false;
        }
    }
}
