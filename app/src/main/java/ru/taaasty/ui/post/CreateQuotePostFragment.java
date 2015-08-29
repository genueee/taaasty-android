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
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.PostQuoteForm;
import ru.taaasty.utils.UiUtils;

public class CreateQuotePostFragment extends CreatePostFragmentBase {

    private static final String SHARED_PREFS_NAME = "CreateQuotePostFragment";
    private static final String SHARED_PREFS_KEY_TEXT = "text";
    private static final String SHARED_PREFS_KEY_SOURCE = "source";

    private static final String ARG_EDIT_POST = "ru.taaasty.ui.post.CreateQuotePostFragment.edit_post";
    private static final String ARG_ORIGINAL_ENTRY = "ru.taaasty.ui.post.CreateQuotePostFragment.original_entry";
    private static final String ARG_TLOG_ID = "ru.taaasty.ui.post.CreateQuotePostFragment.tlog_id";

    private EditText mTextView;
    private EditText mSourceView;

    private boolean mEditPost;

    @Nullable
    private Long mTlogId;

    private OnCreatePostInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment CreateTextPostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreateQuotePostFragment newInstance(@Nullable Long tlogId) {
        CreateQuotePostFragment fragment =  new CreateQuotePostFragment();
        if (tlogId != null) {
            Bundle bundle = new Bundle(1);
            bundle.putLong(ARG_TLOG_ID, tlogId);
            fragment.setArguments(bundle);
        }

        return fragment;
    }

    public static CreateQuotePostFragment newEditPostInstance(Entry originalEntry) {
        CreateQuotePostFragment fragment = new CreateQuotePostFragment();
        Bundle bundle = new Bundle(2);
        bundle.putBoolean(ARG_EDIT_POST, true);
        bundle.putParcelable(ARG_ORIGINAL_ENTRY, originalEntry);
        fragment.setArguments(bundle);
        return fragment;
    }


    public CreateQuotePostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        if (getArguments() != null) {
            mEditPost = getArguments().getBoolean(ARG_EDIT_POST);
            if (getArguments().containsKey(ARG_TLOG_ID)) {
                mTlogId = getArguments().getLong(ARG_TLOG_ID);
            } else {
                mTlogId = null;
            }
        } else {
            mTlogId = null;
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((View)mTextView.getParent()).requestFocus();
    }

    @Override
    public PostQuoteForm getForm() {
        PostQuoteForm form= new PostQuoteForm();
        form.text = PostForm.getTextVievVal(mTextView);
        form.source = PostForm.getTextVievVal(mSourceView);
        form.tlogId = mTlogId;
        return form;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_quote_post, container, false);
        mTextView = (EditText)root.findViewById(R.id.text);
        mSourceView = (EditText)root.findViewById(R.id.edit_quote_source);

        if (mEditPost) {
            Entry original = getArguments().getParcelable(ARG_ORIGINAL_ENTRY);
            if (original == null) throw new IllegalArgumentException();
            mTextView.setText(UiUtils.safeFromHtml(original.getText()));
            mSourceView.setText(UiUtils.safeFromHtml(original.getSource()));
        }

        mTextView.addTextChangedListener(mTextWatcher);
        return root;
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
        if (status.successfully && status.entry instanceof PostQuoteForm.AsHtml && !mEditPost) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTextView != null) mTextView.setText("");
            if (mSourceView != null) mSourceView.setText("");
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
        if (mSourceView == null || mTextView == null || getActivity() == null || mEditPost) return;
        saveInputValues(mTextView.getText().toString(), mSourceView.getText().toString());
    }

    private void clearSharedPrefs() {
        if (mEditPost) return;
        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0).edit().clear().commit();
    }

    private void saveInputValues(String text, String source) {
        if (getActivity() == null) return;
        if (mEditPost) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0)
                .edit()
                .putString(SHARED_PREFS_KEY_TEXT, text)
                .putString(SHARED_PREFS_KEY_SOURCE,  source)
                .commit();
    }

    private void restoreInputValues() {
        if (mTextView == null || mSourceView == null || getActivity() == null) return;
        if (mEditPost) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0);
        String text = prefs.getString(SHARED_PREFS_KEY_TEXT, null);
        String source = prefs.getString(SHARED_PREFS_KEY_SOURCE, null);
        if (text != null) mTextView.setText(text);
        if (source != null) mSourceView.setText(source);
    }

}
