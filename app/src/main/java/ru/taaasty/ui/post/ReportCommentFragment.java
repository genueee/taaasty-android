package ru.taaasty.ui.post;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.service.ApiComments;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;

/**
 * Диалог подтверждения и отправки жалобы
 */
public class ReportCommentFragment extends CommentDialogFragmentBase {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param commentId ID поста
     * @return A new instance of fragment DeletePostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportCommentFragment newInstance(long commentId) {
        ReportCommentFragment fragment = new ReportCommentFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COMMENT_ID, commentId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportCommentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mTitle.setText(R.string.report_comment_description);
        mButton.setText(R.string.report_comment_button);

        return root;
    }

    @Override
    Observable<Object> createObservable() {
        ApiComments service = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        return service.reportComment(mCommentId);
    }

    @Override
    void onError(Throwable e) {
        if (mListener != null) mListener.notifyError(getString(R.string.error_report_to_comment), e);
    }

    @Override
    void onCompleted() {
        EventBus.getDefault().post(new ReportCommentSent(mCommentId));
        Toast.makeText(getActivity(), R.string.comment_complaint_is_sent, Toast.LENGTH_LONG).show();
    }
}
