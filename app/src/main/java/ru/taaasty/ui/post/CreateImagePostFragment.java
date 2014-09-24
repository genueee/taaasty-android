package ru.taaasty.ui.post;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.model.PostEntry;
import ru.taaasty.model.PostImageEntry;

public class CreateImagePostFragment extends CreatePostFragmentBase {

    private static final String SHARED_PREFS_NAME = "CreateImagePostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_IMAGE_URI = "image_uri";

    private EditText mTitleView;
    private View mMakeImageButtonLayout;
    private ImageView mImageView;

    @Nullable
    private Uri mImageUri;

    public static CreateImagePostFragment newInstance() {
        return new CreateImagePostFragment();
    }
    public CreateImagePostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_image_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mMakeImageButtonLayout = root.findViewById(R.id.make_photo_layout);
        mImageView = (ImageView)root.findViewById(R.id.image);
        mImageView.setVisibility(View.GONE);
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnClickListener(mOnChoosePhotoClickListener);
        mImageView.setOnClickListener(mOnChoosePhotoClickListener);
        return root;
    }

    private final View.OnClickListener mOnChoosePhotoClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onChoosePhotoButtonClicked(mImageUri != null);
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        restoreInputValues();
        validateFormIfVisible();
    }

    @Override
    public void onPause() {
        super.onPause();
        saveInputValues();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(PostUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully && status.entry instanceof PostImageEntry) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTitleView != null) mTitleView.setText("");
            mImageUri = null;
            clearSharedPrefs();
            validateFormIfVisible();
            refreshImageView();
        }
    }

    public void onImageSelected(Uri imageUri) {
        mImageUri = imageUri;
        validateFormIfVisible();
        saveInputValues();
        refreshImageView();
    }

    public void onDeleteImageClicked() {
        mImageUri = null;
        validateFormIfVisible();
        saveInputValues();
        refreshImageView();
    }

    @Override
    public PostEntry getForm() {
        PostImageEntry form = new PostImageEntry();
        form.title = mTitleView.getText().toString();
        form.imageUri = mImageUri;
        return form;
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
            mMakeImageButtonLayout.setVisibility(View.INVISIBLE);
            mImageView.setVisibility(View.VISIBLE);
            mImageView.setImageResource(R.drawable.image_loading_drawable);

            Picasso picasso = new Picasso.Builder(getActivity()).listener(
                    new Picasso.Listener() {
                        @Override
                        public void onImageLoadFailed(Picasso picasso, Uri uri,
                                                      Exception exception) {
                            exception.printStackTrace();
                        }

                    }).indicatorsEnabled(true).build();

            picasso.load(mImageUri)
                    .placeholder(R.drawable.image_loading_drawable)
                    .error(R.drawable.image_loading_drawable)
                    .fit().centerInside()
                    .into(mImageView);
        }
    }

    private void saveInputValues() {
        if (mTitleView == null || getActivity() == null) return;
        saveInputValues(mTitleView.getText().toString(), mImageUri);
    }

    private void clearSharedPrefs() {
        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0).edit().clear().commit();
    }

    private void saveInputValues(String title, @Nullable Uri imageUri) {
        if (getActivity() == null) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0)
                .edit()
                .putString(SHARED_PREFS_KEY_TITLE, title)
                .putString(SHARED_PREFS_KEY_IMAGE_URI, imageUri == null ? "" : imageUri.toString())
                .commit();
    }

    private void restoreInputValues() {
        if (mTitleView == null || getActivity() == null) return;
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
