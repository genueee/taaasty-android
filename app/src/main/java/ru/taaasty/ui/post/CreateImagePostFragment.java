package ru.taaasty.ui.post;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.model.PostEntry;
import ru.taaasty.model.PostImageEntry;

public class CreateImagePostFragment extends CreatePostFragmentBase {

    private static final String SHARED_PREFS_NAME = "CreateImagePostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_IMAGE = "image";

    private EditText mTitleView;
    private View mMakeImageButtonLayout;
    private ImageView mImageView;

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
        mMakeImageButtonLayout.findViewById(R.id.make_photo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onChoosePhotoButtonClicked();
            }
        });
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreInputValues();
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
            clearSharedPrefs();
        }
    }

    @Override
    public PostEntry getForm() {
        PostImageEntry form = new PostImageEntry();
        // XXX: image
        form.title = mTitleView.getText().toString();
        return form;
    }

    @Override
    public boolean isFormValid() {
        return false;
    }

    private void saveInputValues() {
        if (mTitleView == null || getActivity() == null) return;
        saveInputValues(mTitleView.getText().toString());
    }

    private void clearSharedPrefs() {
        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0).edit().clear().commit();
    }

    private void saveInputValues(String title) {
        if (getActivity() == null) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0)
                .edit()
                .putString(SHARED_PREFS_KEY_TITLE, title)
                .commit();
    }

    private void restoreInputValues() {
        if (mTitleView == null || getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0);
        String title = prefs.getString(SHARED_PREFS_KEY_TITLE, null);
        if (title != null) mTitleView.setText(title);
    }

}
