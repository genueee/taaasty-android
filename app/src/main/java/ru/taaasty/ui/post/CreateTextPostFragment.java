package ru.taaasty.ui.post;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import ru.taaasty.R;

public class CreateTextPostFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_TEXT = "text";

    private EditText mTitleView;
    private EditText mTextView;

    public static class TextPostForm {
        public final CharSequence title;
        public final CharSequence text;

        public TextPostForm(CharSequence text, CharSequence title) {
            this.title = title;
            this.text = text;
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param title
     * @param text
     * @return A new instance of fragment CreateTextPostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CreateTextPostFragment newInstance(@Nullable String title, @Nullable String text) {
        CreateTextPostFragment fragment = new CreateTextPostFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }
    public CreateTextPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            if (args != null) {
                String title = args.getString(ARG_TITLE);
                if (title != null) mTitleView.setText(title);
                String text = args.getString(ARG_TEXT);
                if (text != null) mTextView.setText(text);
            }
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_create_text_post, container, false);
        mTitleView = (EditText)root.findViewById(R.id.title);
        mTextView = (EditText)root.findViewById(R.id.text);
        return root;
    }

    public TextPostForm getTextPostForm() {
        return new TextPostForm(mTitleView.getText().toString(), mTextView.getText().toString());
    }

    public void setTextPostForm(TextPostForm form) {
        setTextPostForm(form.title, form.text);
    }

    public void setTextPostForm(CharSequence title, CharSequence text) {
        mTitleView.setText(title);
        mTextView.setText(text);
    }
}
