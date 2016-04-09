package ru.taaasty.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import ru.taaasty.rest.model.PostFlowForm;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.ui.post.PhotoSourceManager;
import ru.taaasty.ui.post.SelectPhotoSourceDialogFragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CreateFlowFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CreateFlowFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CreateFlowFragment extends Fragment implements SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateFlowFragment";

    private static final String KEY_IMAGE_URI = "ru.taaasty.ui.CreateFlowFragment.KEY_IMAGE_URI";

    private OnFragmentInteractionListener mListener;

    private EditText mTitleView;
    private EditText mDescriptionView;
    private View mMakeImageButtonLayout;
    private ImageView mImageView;
    private View mProgressView;

    @Nullable
    private Uri mImageUri;

    boolean mFormValid = false;

    private PhotoSourceManager mPhotoSourceManager;

    public static CreateFlowFragment newInstance() {
        return new CreateFlowFragment();
    }

    public CreateFlowFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhotoSourceManager = new PhotoSourceManager(this, "CreateFlowFragment", uri -> mImageUri = uri);
        mPhotoSourceManager.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_flow, container, false);

        mTitleView = (EditText) root.findViewById(R.id.title);
        mDescriptionView = (EditText) root.findViewById(R.id.description);
        mMakeImageButtonLayout = root.findViewById(R.id.make_photo_layout);
        mImageView = (ImageView) root.findViewById(R.id.image);
        mProgressView = root.findViewById(R.id.progress);
        mImageView.setAdjustViewBounds(true);
        mImageView.setVisibility(View.GONE);

        final OnChoosePhotoClickListener onChoosePhotoClickListener = new OnChoosePhotoClickListener();
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnClickListener(onChoosePhotoClickListener);
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnLongClickListener(onChoosePhotoClickListener);
        mImageView.setOnClickListener(onChoosePhotoClickListener);
        mImageView.setOnLongClickListener(onChoosePhotoClickListener);

        TextWatcher textWatcher = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (getUserVisibleHint()) validateForm(false);
            }
        };

        mTitleView.addTextChangedListener(textWatcher);
        mDescriptionView.addTextChangedListener(textWatcher);

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        validateForm(false);
        refreshImageView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPhotoSourceManager.onSaveInstanceState(outState);
        if (mImageUri != null) outState.putParcelable(KEY_IMAGE_URI, mImageUri);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully && status.entry instanceof PostFlowForm.AsHtml) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTitleView != null) mTitleView.setText("");
            if (mDescriptionView != null) mDescriptionView.setText("");
            mImageUri = null;
            validateForm(false);
            refreshImageView();
        }
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        mImageUri = null;
        validateForm(false);
        refreshImageView();
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        mPhotoSourceManager.startPickPhoto();
    }

    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        mPhotoSourceManager.startMakePhoto();
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        mPhotoSourceManager.startFeatherPhoto(mImageUri);
    }

    public PostForm getForm() {
        PostFlowForm form = new PostFlowForm();
        form.title = mTitleView.getText().toString();
        form.description = mDescriptionView.getText().toString();
        form.imageUri = mImageUri;
        return form;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (DBG) Log.v(TAG, "onActivityResult()");
        mPhotoSourceManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPhotoSourceManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public boolean isFormValid() {
        return mImageUri != null && !TextUtils.isEmpty(mTitleView.getText());
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

    void validateForm(boolean forceCallListener) {
        boolean formValid = false;
        boolean statusChanged;
        formValid = isFormValid();
        statusChanged = formValid != mFormValid;
        mFormValid = formValid;
        if ((statusChanged || forceCallListener) && mListener != null) {
            mListener.onValidationStatusChanged(mFormValid);
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
            validateForm(false);
            Toast.makeText(getActivity(), R.string.error_loading_image, Toast.LENGTH_LONG).show();

        }
    };

    private final class OnChoosePhotoClickListener implements View.OnClickListener, View.OnLongClickListener {
        @Override
        public void onClick(View v) {
            onChoosePhotoButtonClicked(mImageUri != null);
        }

        @Override
        public boolean onLongClick(View v) {
            onChoosePhotoButtonClicked(mImageUri != null);
            return true;
        }

        public void onChoosePhotoButtonClicked(boolean hasPicture) {
            DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(hasPicture);
            dialog.show(getChildFragmentManager(), "SelectPhotoSourceDialogFragment");
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onValidationStatusChanged(boolean postValid);
    }

}
