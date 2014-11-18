package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.aviary.android.feather.library.Constants;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.model.PostForm;
import ru.taaasty.model.PostImageForm;
import ru.taaasty.utils.ImageUtils;

public class CreateImagePostFragment extends CreatePostFragmentBase {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateImagePostFragment";

    private static final String ARG_EDIT_POST = "edit_post";
    private static final String ARG_ORIGINAL_TEXT = "original_text";

    private static final String SHARED_PREFS_NAME = "CreateImagePostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_IMAGE_URI = "image_uri";

    private static final int REQUEST_PICK_PHOTO = Activity.RESULT_FIRST_USER + 100;
    private static final int REQUEST_MAKE_PHOTO = Activity.RESULT_FIRST_USER + 101;
    private static final int REQUEST_FEATHER_PHOTO = Activity.RESULT_FIRST_USER + 102;

    private static final String KEY_IMAGE_URI = "ru.taaasty.ui.post.CreateImagePostFragment.KEY_IMAGE_URI";
    private static final String KEY_MAKE_PHOTO_DST_URI = "ru.taaasty.ui.post.CreateImagePostFragment.KEY_MAKE_PHOTO_DST_URI";

    private EditText mTitleView;
    private View mMakeImageButtonLayout;
    private ImageView mImageView;

    @Nullable
    private Uri mMakePhotoDstUri;

    @Nullable
    private Uri mImageUri;

    private boolean mEditPost;

    private PostImageForm mOriginal;

    public static CreateImagePostFragment newInstance() {
        return new CreateImagePostFragment();
    }

    public static CreateImagePostFragment newEditPostInstance(PostImageForm originalText) {
        CreateImagePostFragment fragment = new CreateImagePostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_TEXT, originalText);
        fragment.setArguments(bundle);
        return fragment;
    }


    public CreateImagePostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DBG) Log.v(TAG, "onCreate()");
        if (savedInstanceState != null) {
            mImageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
            mMakePhotoDstUri = savedInstanceState.getParcelable(KEY_MAKE_PHOTO_DST_URI);
        }

        if (getArguments() != null) {
            mEditPost = getArguments().getBoolean(ARG_EDIT_POST);
            mOriginal = getArguments().getParcelable(ARG_ORIGINAL_TEXT);
            if (mOriginal == null && mEditPost) throw new IllegalArgumentException();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_image_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mMakeImageButtonLayout = root.findViewById(R.id.make_photo_layout);
        mImageView = (ImageView)root.findViewById(R.id.image);
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.GONE);
        final OnChoosePhotoClickListener onChoosePhotoClickListener = new OnChoosePhotoClickListener();
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnClickListener(onChoosePhotoClickListener);
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnLongClickListener(onChoosePhotoClickListener);
        mImageView.setOnClickListener(onChoosePhotoClickListener);
        mImageView.setOnLongClickListener(onChoosePhotoClickListener);

        if (mEditPost && savedInstanceState == null) {
            mTitleView.setText(mOriginal.title);
            mImageUri = mOriginal.imageUri;
            refreshImageView();
        }

        return root;
    }

    private final class OnChoosePhotoClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onChoosePhotoButtonClicked(mImageUri != null);
        }

        @Override
        public boolean onLongClick(View v) {
            if (mListener != null) {
                mListener.onChoosePhotoButtonClicked(mImageUri != null);
                return true;
            }
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mEditPost) {
            restoreInputValues();
        } else {
            refreshImageView();
        }
        validateFormIfVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!mEditPost) saveInputValues();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMakePhotoDstUri != null) outState.putParcelable(KEY_MAKE_PHOTO_DST_URI, mMakePhotoDstUri);
        if (mImageUri != null) outState.putParcelable(KEY_IMAGE_URI, mImageUri);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully && status.entry instanceof PostImageForm && !mEditPost) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTitleView != null) mTitleView.setText("");
            mImageUri = null;
            clearSharedPrefs();
            validateFormIfVisible();
            refreshImageView();
        }
    }

    public void onDeleteImageClicked() {
        mImageUri = null;
        validateFormIfVisible();
        if (!mEditPost) saveInputValues();
        refreshImageView();
    }

    public void onPickPhotoSelected() {
        Intent photoPickerIntent = ImageUtils.createPickImageActivityIntent();
        startActivityForResult(photoPickerIntent, REQUEST_PICK_PHOTO);
    }

    public void onMakePhotoSelected() {
        Intent takePictureIntent;
        try {
            takePictureIntent = ImageUtils.createMakePhotoIntent(getActivity(),false);
            mMakePhotoDstUri = takePictureIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
            startActivityForResult(takePictureIntent, REQUEST_MAKE_PHOTO);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(getActivity(), e.errorResourceId, Toast.LENGTH_LONG).show();
        }
    }

    public void onFeatherPhotoClicked() {
        startFeatherPhoto();
    }

    @Override
    public PostForm getForm() {
        PostImageForm form = new PostImageForm();
        form.title = mTitleView.getText().toString();
        form.imageUri = mImageUri;
        return form;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DBG) Log.v(TAG, "onActivityResult()");
        Uri mOriginalImageUri = mImageUri;
        Uri imageUri = null;

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_PICK_PHOTO:
                    mImageUri = data.getData();
                    if (DBG) Log.v(TAG, "image uri: " + mImageUri);
                    startFeatherPhoto();
                    break;
                case REQUEST_MAKE_PHOTO:
                    if (DBG) Log.v(TAG, "image uri: " + mMakePhotoDstUri);
                    ImageUtils.galleryAddPic(getActivity(), mMakePhotoDstUri);
                    mImageUri = mMakePhotoDstUri;
                    mMakePhotoDstUri = null;
                    startFeatherPhoto();
                    break;
                case REQUEST_FEATHER_PHOTO:
                    mImageUri = data.getData();
                    if (mImageUri.toString().startsWith("/")) {
                        mImageUri = Uri.fromFile(new File(mImageUri.toString())); // Мозгоблядство от aviary
                    }
                    boolean changed = false;
                    Bundle extra = data.getExtras();
                    if (null != extra) {
                        // image has been changed by the user?
                        changed = extra.getBoolean(Constants.EXTRA_OUT_BITMAP_CHANGED);
                        /* Пока не удаляем файлы
                        if (changed && !ru.taaasty.utils.Objects.equals(mImageUri, mOriginalImageUri)) {
                            deleteFileNoThrow(mOriginalImageUri);
                        }
                        */
                    }
                    if (DBG) Log.v(TAG, "REQUEST_FEATHER_PHOTO. imageuri: " + mOriginalImageUri +
                            " new image uri: " + mImageUri + " bitmap changed: " + changed);
                    break;
            }
        }

        if (!ru.taaasty.utils.Objects.equals(mImageUri, mOriginalImageUri)) {
            if (mImageUri != null) ImageUtils.galleryAddPic(getActivity(), mImageUri);
            if (!mEditPost) {
                getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0)
                        .edit()
                        .putString(SHARED_PREFS_KEY_IMAGE_URI, mImageUri == null ? "" : mImageUri.toString())
                        .commit();
            }
        }
    }

    @Override
    public boolean isFormValid() {
        return mImageUri != null;
    }

    private void refreshImageView() {
        if (mImageUri == null) {
            mMakeImageButtonLayout.setVisibility(View.VISIBLE);
            mImageView.setVisibility(View.GONE);
        } else {
            mMakeImageButtonLayout.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);

            Picasso picasso = Picasso.with(getActivity());

            picasso.load(mImageUri)
                    .placeholder(R.drawable.image_loading_drawable)
                    .error(R.drawable.image_load_error)
                    .skipMemoryCache()
                    .fit().centerInside()
                    .into(mImageView, mPicassoCallback);
        }
    }

    private void deleteFileNoThrow(Uri uri) {
        if (DBG) Log.v(TAG, "delete file " + uri);
        if (ImageUtils.isUriInPicturesDirectory(getActivity(), uri)) {
            try {
                new File(new URI(uri.toString())).delete();
            } catch (URISyntaxException ignore) {
            }
        }
    }

    private void startFeatherPhoto() {
        try {
            Intent newIntent = ImageUtils.createFeatherPhotoIntent(getActivity(), mImageUri);
            startActivityForResult( newIntent, REQUEST_FEATHER_PHOTO);
        } catch (ImageUtils.MakePhotoException e) {
            Toast.makeText(getActivity(), e.errorResourceId, Toast.LENGTH_LONG).show();
        }
    }

    private Callback mPicassoCallback = new Callback() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError() {
            if (getActivity() == null) return;
            mImageUri = null;
            validateFormIfVisible();
            if (!mEditPost) saveInputValues();
            refreshImageView();
            Toast.makeText(getActivity(), R.string.error_loading_image, Toast.LENGTH_LONG).show();

        }
    };

    private void saveInputValues() {
        if (mTitleView == null || getActivity() == null || mEditPost) return;
        saveInputValues(mTitleView.getText().toString(), mImageUri);
    }

    private void clearSharedPrefs() {
        if (mEditPost) return;
        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0).edit().clear().commit();
    }

    private void saveInputValues(String title, @Nullable Uri imageUri) {
        if (getActivity() == null || mEditPost) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0)
                .edit()
                .putString(SHARED_PREFS_KEY_TITLE, title)
                .putString(SHARED_PREFS_KEY_IMAGE_URI, imageUri == null ? "" : imageUri.toString())
                .commit();
    }

    private void restoreInputValues() {
        if (mTitleView == null || getActivity() == null || mEditPost) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0);
        String title = prefs.getString(SHARED_PREFS_KEY_TITLE, null);
        String imageUri = prefs.getString(SHARED_PREFS_KEY_IMAGE_URI, null);
        if (title != null) mTitleView.setText(title);
        if (!TextUtils.isEmpty(imageUri)) {
            mImageUri = Uri.parse(imageUri);
            refreshImageView();
        }
    }

}
