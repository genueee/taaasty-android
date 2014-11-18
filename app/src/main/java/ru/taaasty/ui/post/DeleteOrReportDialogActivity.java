package ru.taaasty.ui.post;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.model.Entry;
import ru.taaasty.service.ApiComments;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Диалог с удалением, либо жалобой на пост или комментарий
 */
public class DeleteOrReportDialogActivity extends ActivityBase implements CustomErrorView  {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CommentActionActivity";

    private static final int ACTION_DELETE_POST = 0;

    private static final int ACTION_REPORT_POST = 1;

    private static final int ACTION_DELETE_COMMENT = 2;

    private static final int ACTION_REPORT_COMMENT = 3;

    private static final String ARG_ACTION_ID = "ru.taaasty.ui.post.CommentActionActivity.ARG_ACTION_ID";

    private static final String ARG_COMMENT_ID = "ru.taaasty.ui.post.CommentActionActivity.ARG_COMMENT_ID";

    private static final String ARG_POST_ID = "ru.taaasty.ui.post.CommentActionActivity.ARG_POST_ID";

    TextView mTitle;

    TextView mButton;

    View mProgress;

    private ActionHandler mActionHandler;

    Subscription mSubscription = SubscriptionHelper.empty();

    private static void startActivityAction(Context context, int actionId, long postId, long commentId) {
        Intent i = new Intent(context, DeleteOrReportDialogActivity.class);
        i.putExtra(ARG_ACTION_ID, actionId);
        switch(actionId) {
            case ACTION_DELETE_COMMENT:
            case ACTION_REPORT_COMMENT:
                i.putExtra(ARG_POST_ID, postId);
                i.putExtra(ARG_COMMENT_ID, commentId);
                break;
            default:
                i.putExtra(ARG_POST_ID, postId);
                break;
        }

        context.startActivity(i);
    }

    public static void startDeletePost(Context context, long postId) {
        startActivityAction(context, ACTION_DELETE_POST, postId, -1);
    }

    public static void startReportPost(Context context, long postId) {
        startActivityAction(context, ACTION_REPORT_POST, postId, -1);
    }

    public static void startDeleteComment(Context context, long postId, long commentId) {
        startActivityAction(context, ACTION_DELETE_COMMENT, postId, commentId);
    }

    public static void startReportComment(Context context, long commentId) {
        startActivityAction(context, ACTION_REPORT_COMMENT, -1, commentId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_action);
        int actionId = getIntent().getIntExtra(ARG_ACTION_ID, -1);
        long commentId = getIntent().getLongExtra(ARG_COMMENT_ID, -1);
        long postId = getIntent().getLongExtra(ARG_POST_ID, -1);

        mTitle = (TextView)findViewById(R.id.title);
        mButton = (TextView)findViewById(R.id.delete_comment_button);
        mProgress = findViewById(R.id.delete_comment_progress);

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doAction();
            }
        });

        findViewById(R.id.root).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ActivityCompat.finishAfterTransition(DeleteOrReportDialogActivity.this);
                return true;
            }
        });

        switch (actionId) {
            case ACTION_DELETE_POST:
                mActionHandler = new DeletePostActionHandler(postId);
                break;
            case ACTION_REPORT_POST:
                mActionHandler = new ReportPostActionHandler(postId);
                break;
            case ACTION_DELETE_COMMENT:
                mActionHandler = new DeleteCommentActionHandler(postId, commentId);
                break;
            case ACTION_REPORT_COMMENT:
                mActionHandler = new ReportCommentActionHandler(commentId);
                break;
            default:
                throw new IllegalArgumentException("invalid action");
        }

        mActionHandler.onCreate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    void doAction() {
        mButton.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);

        Observable<Object> observable = AndroidObservable.bindActivity(this,
                mActionHandler.createObservable());

        mSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mObserver);

    }

    private final Observer<Object> mObserver = new Observer<Object>() {

        @Override
        public void onCompleted() {
            mActionHandler.onCompleted();
            ActivityCompat.finishAfterTransition(DeleteOrReportDialogActivity.this);
        }

        @Override
        public void onError(Throwable e) {
            mButton.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.INVISIBLE);
            mActionHandler.onError(e);
        }

        @Override
        public void onNext(Object o) {
            if (DBG) Log.v(TAG, "response: " + o);
            mActionHandler.onNext(o);
        }
    };


    private interface ActionHandler {
        abstract void onCreate();
        abstract Observable<Object> createObservable();
        abstract void onError(Throwable e);
        abstract void onCompleted();
        abstract void onNext(Object o);
    }

    private class DeletePostActionHandler implements ActionHandler {

        private final long mPostId;

        public DeletePostActionHandler(long postId) {
            mPostId = postId;
        }

        @Override
        public void onCreate() {
            mTitle.setText(R.string.delete_post_description);
            mButton.setText(R.string.delete_post_button);
        }

        @Override
        public Observable<Object> createObservable() {
            ApiEntries service = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
            return service.deleteEntry(mPostId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_loading_comments), e);
        }

        @Override
        public void onCompleted() {
            EventBus.getDefault().post(new EntryRemoved(mPostId));
            Toast.makeText(DeleteOrReportDialogActivity.this, R.string.post_removed, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(Object o) {
        }
    }

    private class ReportPostActionHandler implements ActionHandler {

        private final long mPostId;

        public ReportPostActionHandler(long postId) {
            mPostId = postId;
        }

        @Override
        public void onCreate() {
            mTitle.setText(R.string.report_post_description);
            mButton.setText(R.string.report_post_button);
        }

        @Override
        public Observable<Object> createObservable() {
            ApiEntries service = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
            return service.reportEntry(mPostId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_loading_comments), e);
        }

        @Override
        public void onCompleted() {
            Toast.makeText(DeleteOrReportDialogActivity.this, R.string.post_complaint_is_sent, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(Object o) {
        }
    }

    private class DeleteCommentActionHandler implements ActionHandler {

        private final long mPostId;

        private final long mCommentId;

        private Entry mNewEntry;

        public DeleteCommentActionHandler(long postId, long commentId) {
            mPostId = postId;
            mCommentId = commentId;
        }

        @Override
        public void onCreate() {
            mTitle.setText(R.string.delete_comment_description);
            mButton.setText(R.string.delete_comment_button);
        }

        @Override
        public Observable<Object> createObservable() {
            ApiComments service = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
            ApiEntries entriesApi = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);

            Observable<Object> delete = service.deleteComment(mCommentId);
            Observable<Entry> updateEntry = entriesApi.getEntry(mPostId, false);

            return delete.concatWith(updateEntry);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_loading_comments), e);
        }

        @Override
        public void onCompleted() {
            EventBus bus = EventBus.getDefault();
            bus.post(new CommentRemoved(mCommentId));
            if (mNewEntry != null) bus.post(new EntryChanged(mNewEntry));
            Toast.makeText(DeleteOrReportDialogActivity.this, R.string.comment_removed, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(Object o) {
            if (o instanceof Entry) {
                mNewEntry = (Entry) o;
            }
        }
    }

    private class ReportCommentActionHandler implements ActionHandler {

        private final long mCommentId;

        public ReportCommentActionHandler(long commentId) {
            mCommentId = commentId;
        }

        @Override
        public void onCreate() {
            mTitle.setText(R.string.report_comment_description);
            mButton.setText(R.string.report_comment_button);
        }

        @Override
        public Observable<Object> createObservable() {
            ApiComments service = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);
            return service.reportComment(mCommentId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_report_to_comment), e);
        }

        @Override
        public void onCompleted() {
            EventBus.getDefault().post(new ReportCommentSent(mCommentId));
            Toast.makeText(DeleteOrReportDialogActivity.this, R.string.comment_complaint_is_sent, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext(Object o) {
        }
    }
}
