package ru.taaasty.ui.post;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.PostImageForm;
import ru.taaasty.utils.UiUtils;

public class CreateImagePostFragment extends CreatePostFragmentBase {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateImagePostFragment";

    private static final String ARG_EDIT_POST = "ru.taaasty.ui.post.CreateImagePostFragment.edit_post";
    private static final String ARG_ORIGINAL_ENTRY = "ru.taaasty.ui.post.CreateImagePostFragment.original_text";
    private static final String ARG_SHARED_IMAGE_URI = "ru.taaasty.ui.post.CreateImagePostFragment.ARG_SHARED_IMAGE_URI";
    private static final String ARG_TLOG_ID = "ru.taaasty.ui.post.CreateImagePostFragment.ARG_TLOG_ID";

    private static final String SHARED_PREFS_NAME = "CreateImagePostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_IMAGE_URI = "image_uri";

    private static final String KEY_IMAGE_URI = "ru.taaasty.ui.post.CreateImagePostFragment.KEY_IMAGE_URI";

    private EditText mTitleView;
    private View mMakeImageButtonLayout;
    private ImageView mImageView;
    private View mProgressView;

    @Nullable
    private Uri mImageUri;

    private Uri mSharedImageUri;

    private boolean mEditPost;

    private PhotoSourceManager mPhotoSourceManager;

    @Nullable
    private Long mTlogId;

    public static CreateImagePostFragment newInstance(@Nullable Long tlogId, @Nullable Uri sharedImageUri) {
        CreateImagePostFragment fragment = new CreateImagePostFragment();
        Bundle bundle = new Bundle(1);
        bundle.putParcelable(ARG_SHARED_IMAGE_URI, sharedImageUri);
        if (tlogId != null) bundle.putLong(ARG_TLOG_ID, tlogId);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CreateImagePostFragment newEditPostInstance(Entry original) {
        CreateImagePostFragment fragment = new CreateImagePostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_ENTRY, original);
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
        }

        if (getArguments() != null) {
            mEditPost = getArguments().getBoolean(ARG_EDIT_POST);
            mSharedImageUri = getArguments().getParcelable(ARG_SHARED_IMAGE_URI);
            if (getArguments().containsKey(ARG_TLOG_ID)) {
                mTlogId = getArguments().getLong(ARG_TLOG_ID);
            } else {
                mTlogId = null;
            }
        } else {
            mEditPost = false;
            mSharedImageUri = null;
            mTlogId = null;
        }

        mPhotoSourceManager = new PhotoSourceManager(this, "CreateImagePost", (uri) -> mImageUri = uri);
        mPhotoSourceManager.onCreate(savedInstanceState);

        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_image_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mMakeImageButtonLayout = root.findViewById(R.id.make_photo_layout);
        mImageView = (ImageView)root.findViewById(R.id.image);
        mProgressView = root.findViewById(R.id.progress);
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.GONE);
        if (mSharedImageUri == null) {
            final OnChoosePhotoClickListener onChoosePhotoClickListener = new OnChoosePhotoClickListener();
            mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnClickListener(onChoosePhotoClickListener);
            mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnLongClickListener(onChoosePhotoClickListener);
            mImageView.setOnClickListener(onChoosePhotoClickListener);
            mImageView.setOnLongClickListener(onChoosePhotoClickListener);
        }

        if (mEditPost && savedInstanceState == null) {
            Entry original = getArguments().getParcelable(ARG_ORIGINAL_ENTRY);
            if (original == null) throw new IllegalArgumentException();
            mTitleView.setText(UiUtils.safeFromHtml(original.getTitle()));
            mImageUri = original.getFirstImageUri();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPhotoSourceManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
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
        if (mImageUri != null) outState.putParcelable(KEY_IMAGE_URI, mImageUri);
        mPhotoSourceManager.onSaveInstanceState(outState);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully && status.entry instanceof PostImageForm.AsHtml && !mEditPost) {
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
        mPhotoSourceManager.startPickPhoto();
    }

    public void onMakePhotoSelected() {
        mPhotoSourceManager.startMakePhoto();
    }

    public void onFeatherPhotoClicked() {
        mPhotoSourceManager.startFeatherPhoto(mImageUri);
    }

    @Override
    public PostForm getForm() {
        PostImageForm form = new PostImageForm();
        form.title = mTitleView.getText().toString();
        form.imageUri = mImageUri;
        form.tlogId = mTlogId;
        return form;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DBG) Log.v(TAG, "onActivityResult()");
        Uri mOriginalImageUri = mImageUri;

        mPhotoSourceManager.onActivityResult(requestCode, resultCode, data);

        if (!ru.taaasty.utils.Objects.equals(mImageUri, mOriginalImageUri)) {
            if (!mEditPost) {
                // onActivityResult() может быть вызван после onPause() и перед onResume()
                // В этом случае mImageUri у нас сохранен в preferences и его надо обновить
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
            mProgressView.setVisibility(View.GONE);
        } else {
            mMakeImageButtonLayout.setVisibility(View.GONE);
            mImageView.setVisibility(View.VISIBLE);
            mProgressView.setVisibility(View.VISIBLE);

            Picasso picasso = Picasso.with(getActivity());

            picasso.load(mImageUri)
                    .placeholder(R.drawable.image_loading_drawable)
                    .error(R.drawable.image_load_error)
                    .skipMemoryCache()
                    .fit().centerInside()
                    .into(mImageView, mPicassoCallback);
        }
    }

    private Callback mPicassoCallback = new Callback() {
        @Override
        public void onSuccess() {
            mProgressView.setVisibility(View.GONE);
        }

        @Override
        public void onError() {
            if (getActivity() == null) return;
            mProgressView.setVisibility(View.GONE);
            mImageUri = null;
            validateFormIfVisible();
            if (!mEditPost) saveInputValues();
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
        // Если мы шарим урл, игшнорируем сохраненные настройки и считаем, что расшаренный - наш урл
        if (mSharedImageUri != null) {
            mImageUri = mSharedImageUri;
        } else if (!TextUtils.isEmpty(imageUri)) {
            mImageUri = Uri.parse(imageUri);
        }
        if (mImageUri != null) {
            refreshImageView();
        }
    }

}
