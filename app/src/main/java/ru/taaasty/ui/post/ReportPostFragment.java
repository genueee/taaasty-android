package ru.taaasty.ui.post;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;


public class ReportPostFragment extends CommentDialogFragmentBase {
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param postId ID поста
     * @return A new instance of fragment DeletePostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportPostFragment newInstance(long postId) {
        ReportPostFragment fragment = new ReportPostFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COMMENT_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportPostFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mTitle.setText(R.string.report_post_description);
        mButton.setText(R.string.report_post_button);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().finish();
    }

    @Override
    Observable<Object> createObservable() {
        ApiEntries service = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
        return service.reportEntry(mCommentId);
    }

    @Override
    void onError(Throwable e) {
        if (mListener != null) mListener.notifyError(getString(R.string.error_loading_comments), e);
    }

    @Override
    void onCompleted() {
        Toast.makeText(getActivity(), R.string.post_complaint_is_sent, Toast.LENGTH_LONG).show();
    }
}
