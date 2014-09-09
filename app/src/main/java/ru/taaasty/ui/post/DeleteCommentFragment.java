package ru.taaasty.ui.post;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.service.ApiComments;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Диалог подтверждения и удаление комментария
 */
public class DeleteCommentFragment extends DialogFragment {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "DeleteCommentFragment";

    private static final String ARG_COMMENT_ID = "comment_id";

    private long mCommentId;

    private View mDeleteCommentButton;
    private View mDeleteCommentProgress;

    private CustomErrorView mListener;

    private Subscription mDeleteCommentSubscription = SubscriptionHelper.empty();

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.NoFrameDialog);
        if (getArguments() != null) {
            mCommentId = getArguments().getLong(ARG_COMMENT_ID);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (CustomErrorView) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement CustomErrorView");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_delete_comment, null, false);
        mDeleteCommentButton = root.findViewById(R.id.delete_comment_button);
        mDeleteCommentProgress = root.findViewById(R.id.delete_comment_progress);

        mDeleteCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteComment();
            }
        });

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                DeleteCommentFragment.this.getDialog().cancel();
                return true;
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mDeleteCommentSubscription.unsubscribe();
        mDeleteCommentButton = null;
        mDeleteCommentProgress = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    void deleteComment() {
        mDeleteCommentButton.setVisibility(View.INVISIBLE);
        mDeleteCommentProgress.setVisibility(View.VISIBLE);

        ApiComments service = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
        Observable<Object> observable = AndroidObservable.bindFragment(this,
                service.deleteComment(mCommentId));

        mDeleteCommentSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mDeleteCommentObserver);

    }

    private final Observer<Object> mDeleteCommentObserver = new Observer<Object>() {

        @Override
        public void onCompleted() {
            EventBus.getDefault().post(new CommentRemoved(mCommentId));
            DeleteCommentFragment.this.getDialog().cancel();
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_loading_comments), e);
            mDeleteCommentButton.setVisibility(View.VISIBLE);
            mDeleteCommentProgress.setVisibility(View.INVISIBLE);
            DeleteCommentFragment.this.getDialog().cancel();
        }

        @Override
        public void onNext(Object o) {
            if (DBG) Log.v(TAG, "delete comment response: " + o);
        }
    };

}
