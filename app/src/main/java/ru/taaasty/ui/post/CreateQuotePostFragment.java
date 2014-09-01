package ru.taaasty.ui.post;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.model.PostQuoteEntry;

public class CreateQuotePostFragment extends CreatePostFragmentBase {

    private static final String SHARED_PREFS_NAME = "CreateQuotePostFragment";
    private static final String SHARED_PREFS_KEY_TEXT = "text";
    private static final String SHARED_PREFS_KEY_SOURCE = "source";

    private EditText mTextView;
    private EditText mSourceView;

    private OnCreatePostInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment CreateTextPostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreateQuotePostFragment newInstance() {
        return new CreateQuotePostFragment();
    }
    public CreateQuotePostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((View)mTextView.getParent()).requestFocus();
    }

    @Override
    public PostQuoteEntry getForm() {
        PostQuoteEntry form= new PostQuoteEntry();
        form.text = mTextView.getText().toString();
        form.source = mSourceView.getText().toString();
        return form;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_quote_post, container, false);
        mTextView = (EditText)root.findViewById(R.id.text);
        mSourceView = (EditText)root.findViewById(R.id.edit_quote_source);
        mTextView.addTextChangedListener(mTextWatcher);
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
        if (status.successfully && status.entry instanceof PostQuoteEntry) {
            // Скорее всего наша форма. Очищаем все и вся
            if (mTextView != null) mTextView.setText("");
            if (mSourceView != null) mSourceView.setText("");
            clearSharedPrefs();
        }
    }

    public void setTextPostForm(CharSequence title, CharSequence text) {
        mTextView.setText(title);
        mSourceView.setText(text);
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
        if (mSourceView == null || mTextView == null || getActivity() == null) return;
        saveInputValues(mTextView.getText().toString(), mSourceView.getText().toString());
    }

    private void clearSharedPrefs() {
        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0).edit().clear().commit();
    }

    private void saveInputValues(String text, String source) {
        if (getActivity() == null) return;

        getActivity().getSharedPreferences(SHARED_PREFS_NAME,0)
                .edit()
                .putString(SHARED_PREFS_KEY_TEXT, text)
                .putString(SHARED_PREFS_KEY_SOURCE,  source)
                .commit();
    }

    private void restoreInputValues() {
        if (mTextView == null || mSourceView == null || getActivity() == null) return;
        SharedPreferences prefs = getActivity().getSharedPreferences(SHARED_PREFS_NAME, 0);
        String text = prefs.getString(SHARED_PREFS_KEY_TEXT, null);
        String source = prefs.getString(SHARED_PREFS_KEY_SOURCE, null);
        if (text != null) mTextView.setText(text);
        if (source != null) mSourceView.setText(source);
    }

}
