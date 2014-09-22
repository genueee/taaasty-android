package ru.taaasty.ui.post;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.service.ApiComments;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;

/**
 * Диалог подтверждения и удаление комментария
 */
public class DeleteCommentFragment extends CommentDialogFragmentBase {

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param commentId ID поста
     * @return A new instance of fragment DeletePostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeleteCommentFragment newInstance(long commentId) {
        DeleteCommentFragment fragment = new DeleteCommentFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COMMENT_ID, commentId);
        fragment.setArguments(args);
        return fragment;
    }
    public DeleteCommentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mTitle.setText(R.string.delete_comment_description);
        mButton.setText(R.string.delete_comment_button);

        return root;
    }

    @Override
    Observable<Object> createObservable() {
        ApiComments service = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        return service.deleteComment(mCommentId);
    }

    @Override
    void onError(Throwable e) {
        if (mListener != null) mListener.notifyError(getString(R.string.error_loading_comments), e);
    }

    @Override
    void onCompleted() {
        EventBus.getDefault().post(new CommentRemoved(mCommentId));
        Toast.makeText(getActivity(), R.string.comment_removed, Toast.LENGTH_LONG).show();
    }
}