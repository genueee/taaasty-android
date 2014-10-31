package ru.taaasty.ui.post;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.R;
import ru.taaasty.events.PostRemoved;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.utils.NetworkUtils;
import rx.Observable;


public class DeletePostFragment extends CommentDialogFragmentBase {
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param postId ID поста
     * @return A new instance of fragment DeletePostFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DeletePostFragment newInstance(long postId) {
        DeletePostFragment fragment = new DeletePostFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_COMMENT_ID, postId);
        fragment.setArguments(args);
        return fragment;
    }
    public DeletePostFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);

        mTitle.setText(R.string.delete_post_description);
        mButton.setText(R.string.delete_post_button);

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
        return service.deleteEntry(mCommentId);
    }

    @Override
    void onError(Throwable e) {
        if (mListener != null) mListener.notifyError(getString(R.string.error_loading_comments), e);
    }

    @Override
    void onCompleted() {
        EventBus.getDefault().post(new PostRemoved(mCommentId));
        Toast.makeText(getActivity(), R.string.post_removed, Toast.LENGTH_LONG).show();
    }
}
