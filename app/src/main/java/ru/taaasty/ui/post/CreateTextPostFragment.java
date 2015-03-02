package ru.taaasty.ui.post;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.model.PostAnonymousTextForm;
import ru.taaasty.model.PostForm;
import ru.taaasty.model.PostTextForm;

public class CreateTextPostFragment extends CreatePostFragmentBase {
    private EditText mTitleView;
    private EditText mTextView;

    private static final String ARG_EDIT_POST = "edit_post";
    private static final String ARG_IS_ANONYMOUS = "is_anonymous";
    private static final String ARG_ORIGINAL_TEXT = "original_text";

    private static final String SHARED_PREFS_NAME = "CreateTextPostFragment";
    private static final String SHARED_PREFS_ANONYMOUS_NAME = "CreateAnonymousPostFragment";
    private static final String SHARED_PREFS_KEY_TITLE = "title";
    private static final String SHARED_PREFS_KEY_TEXT = "text";

    private boolean mEditPost;

    private boolean mIsAnonymous;

    @Nullable
    private PostForm mOriginal;

    public static CreateTextPostFragment newCreatePostInstance() {
        return new CreateTextPostFragment();
    }

    public static CreateTextPostFragment newEditPostInstance(PostTextForm originalText) {
        CreateTextPostFragment fragment = new CreateTextPostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_TEXT, originalText);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CreateTextPostFragment newCreateAnonymousInstance() {
        CreateTextPostFragment fragment = new CreateTextPostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_IS_ANONYMOUS, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static CreateTextPostFragment newCreateEditAnonymousInstance(PostAnonymousTextForm originalText) {
        CreateTextPostFragment fragment = new CreateTextPostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_IS_ANONYMOUS, true);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_TEXT, originalText);
        fragment.setArguments(bundle);
        return fragment;
    }

    public CreateTextPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            mEditPost = getArguments().getBoolean(ARG_EDIT_POST);
            mIsAnonymous = getArguments().getBoolean(ARG_IS_ANONYMOUS);
            mOriginal = getArguments().getParcelable(ARG_ORIGINAL_TEXT);
            if (mOriginal == null && mEditPost) throw new IllegalArgumentException();
        } else {
            mEditPost = false;
            mIsAnonymous = false;
            mOriginal = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_text_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mTextView = (EditText)root.findViewById(R.id.text);

        if (mIsAnonymous) {
            mTextView.setHint(R.string.edit_text_anonymous_text);
            if (mEditPost) {
                mTitleView.setText(((PostAnonymousTextForm)mOriginal).title);
                mTextView.setText(((PostAnonymousTextForm)mOriginal).text);
            }
        } else {
            if (mEditPost) {
                mTitleView.setText(((PostTextForm)mOriginal).title);
                mTextView.setText(((PostTextForm)mOriginal).text);
            }
        }

        mTitleView.addTextChangedListener(mTextWatcher);
        mTextView.addTextChangedListener(mTextWatcher);

        return root;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((View)mTitleView.getParent()).requestFocus();
    }

    @Override
    public PostForm getForm() {
        PostForm form;
        if (mIsAnonymous) {
            PostAnonymousTextForm anonForm = new PostAnonymousTextForm();
            anonForm.title = mTitleView.getText();
            anonForm.text = mTextView.getText();
            form = anonForm;
        } else {
            PostTextForm textForm = new PostTextForm();
            textForm.title = mTitleView.getText();
            textForm.text = mTextView.getText();
            form = textForm;
        }
        return form;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mEditPost) restoreInputValues();
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

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (mEditPost) return;
        if (status.successfully && status.entry instanceof PostTextForm) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTextView != null) mTextView.setText("");
            if (mTitleView != null) mTitleView.setText("");
            clearSharedPrefs();
        }
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            if (getUserVisibleHint()) validateForm();
        }
    };

    @Override
    public boolean isFormValid() {
        return !TextUtils.isEmpty(mTextView.getText());
    }

    private void saveInputValues() {
        if (mEditPost) return;
        if (mTitleView == null || mTextView == null || getActivity() == null) return;
        saveInputValues(mTitleView.getText().toString(), mTextView.getText().toString());
    }

    private SharedPreferences getSharedPreferences() {
        return getActivity().getSharedPreferences(mIsAnonymous ? SHARED_PREFS_ANONYMOUS_NAME : SHARED_PREFS_NAME, 0);
    }

    private void clearSharedPrefs() {
        if (mEditPost) return;
        getSharedPreferences().edit().clear().commit();
    }

    private void saveInputValues(String title, String text) {
        if (getActivity() == null) return;
        if (mEditPost) return;

        getSharedPreferences()
                .edit()
                .putString(SHARED_PREFS_KEY_TITLE,  title)
                .putString(SHARED_PREFS_KEY_TEXT, text)
                .commit();
    }

    private void restoreInputValues() {
        if (mTitleView == null
                || mTextView == null
                || getActivity() == null
                || mEditPost) return;
        SharedPreferences prefs = getSharedPreferences();
        String title = prefs.getString(SHARED_PREFS_KEY_TITLE, null);
        String text = prefs.getString(SHARED_PREFS_KEY_TEXT, null);
        if (title != null) mTitleView.setText(title);
        if (text != null) mTextView.setText(text);
    }

}
