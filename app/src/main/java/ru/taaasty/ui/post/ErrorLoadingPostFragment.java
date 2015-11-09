package ru.taaasty.ui.post;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;

public class ErrorLoadingPostFragment extends Fragment {
    static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = "ErrorLoadingPostFragment";

    private static final String ARG_TEXT = "text";
    private static final String ARG_ICON_ID = "icon_id";

    private String mText;

    private int mIconId;

    public static ErrorLoadingPostFragment newInstance(String text, @Nullable Integer iconId) {
        ErrorLoadingPostFragment fragment = new ErrorLoadingPostFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TEXT, text);
        if (iconId != null) args.putInt(ARG_ICON_ID, iconId);
        fragment.setArguments(args);
        return fragment;
    }

    public ErrorLoadingPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mText = getArguments().getString(ARG_TEXT);
            mIconId = getArguments().getInt(ARG_ICON_ID);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        inflater = getActivity().getLayoutInflater(); // Calligraphy and support-21 bug
        View root = inflater.inflate(R.layout.fragment_error_loading_post, container, false);
        TextView textView = (TextView) root.findViewById(R.id.error_text);
        ImageView iconView = (ImageView) root.findViewById(R.id.icon);

        if (!TextUtils.isEmpty(mText)) {
            textView.setText(mText);
        } else {
            textView.setVisibility(View.GONE);
        }

        if (mIconId != 0) {
            iconView.setImageResource(mIconId);
        } else {
            iconView.setVisibility(View.GONE);
        }

        return root;
    }
}
