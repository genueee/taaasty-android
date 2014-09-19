package ru.taaasty.ui.post;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Диалог подтверждения и удаление комментария
 */
public abstract class CommentDialogFragmentBase extends DialogFragment {
    static final boolean DBG = BuildConfig.DEBUG;
    static final String TAG = "CommentDialogFragmentBase";

    static final String ARG_COMMENT_ID = "comment_id";

    long mCommentId;

    TextView mTitle;
    TextView mButton;
    View mProgress;

    CustomErrorView mListener;

    Subscription mSubscription = SubscriptionHelper.empty();

    public CommentDialogFragmentBase() {
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
        View root = inflater.inflate(R.layout.fragment_comment_confirmation_dialog, null, false);
        mTitle = (TextView)root.findViewById(R.id.title);
        mButton = (TextView)root.findViewById(R.id.delete_comment_button);
        mProgress = root.findViewById(R.id.delete_comment_progress);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAction();
            }
        });

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                CommentDialogFragmentBase.this.getDialog().cancel();
                return true;
            }
        });

        return root;
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        getDialog().getWindow()
                .getAttributes().windowAnimations = R.style.DialogFadeInAnimation;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSubscription.unsubscribe();
        mButton = null;
        mProgress = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    abstract Observable<Object> createObservable();

    abstract void onError(Throwable e);

    abstract void onCompleted();

    void doAction() {
        mButton.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);

        Observable<Object> observable = AndroidObservable.bindFragment(this,
                createObservable());

        mSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mObserver);

    }

    private final Observer<Object> mObserver = new Observer<Object>() {

        @Override
        public void onCompleted() {
            CommentDialogFragmentBase.this.onCompleted();
            CommentDialogFragmentBase.this.getDialog().cancel();
        }

        @Override
        public void onError(Throwable e) {
            CommentDialogFragmentBase.this.onError(e);
            mButton.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
            CommentDialogFragmentBase.this.getDialog().cancel();
        }

        @Override
        public void onNext(Object o) {
            if (DBG) Log.v(TAG, "response: " + o);
        }
    };

}
