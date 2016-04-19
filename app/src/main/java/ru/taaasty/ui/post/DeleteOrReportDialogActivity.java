package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.events.CommentRemoved;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.events.ReportCommentSent;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.RestSchedulerHelper;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.service.ApiComments;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Диалог с удалением, либо жалобой на пост или комментарий
 * При startActivityForResult может вернуть {@link ru.taaasty.Constants#ACTIVITY_RESULT_CODE_SHOW_ERROR}
 *  с текстом в {@link ru.taaasty.Constants#ACTIVITY_RESULT_CODE_SHOW_ERROR}
 */
public class DeleteOrReportDialogActivity extends ActivityBase {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CommentActionActivity";

    private static final int ACTION_DELETE_POST = 0;

    private static final int ACTION_REPORT_POST = 1;

    private static final int ACTION_DELETE_COMMENT = 2;

    private static final int ACTION_REPORT_COMMENT = 3;

    private static final String ARG_ACTION_ID = "ru.taaasty.ui.post.DeleteOrReportDialogActivity.ARG_ACTION_ID";

    private static final String ARG_COMMENT_ID = "ru.taaasty.ui.post.DeleteOrReportDialogActivity.ARG_COMMENT_ID";

    private static final String ARG_POST_ID = "ru.taaasty.ui.post.DeleteOrReportDialogActivity.ARG_POST_ID";

    private static final String ARG_TLOG_ID = "ru.taaasty.ui.post.DeleteOrReportDialogActivity.ARG_TLOG_ID";

    TextView mTitle;

    TextView mButton;

    View mProgress;

    private ActionHandler mActionHandler;

    Subscription mSubscription = Subscriptions.unsubscribed();

    private static Intent createIntent(Context context, int actionId) {
        Intent intent = new Intent(context, DeleteOrReportDialogActivity.class);
        intent.putExtra(ARG_ACTION_ID, actionId);
        return intent;
    }

    public static void startDeletePost(Activity activity, int requestCode, long tlogId, long postId) {
        Intent intent = createIntent(activity, ACTION_DELETE_POST);
        intent.putExtra(ARG_POST_ID, postId);
        intent.putExtra(ARG_TLOG_ID, tlogId);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startReportPost(Activity activity, int requestCode, long postId) {
        Intent intent = createIntent(activity, ACTION_REPORT_POST);
        intent.putExtra(ARG_POST_ID, postId);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startDeleteComment(Activity activity, int requestCode, long postId, long commentId) {
        Intent intent = createIntent(activity, ACTION_DELETE_COMMENT);
        intent.putExtra(ARG_POST_ID, postId);
        intent.putExtra(ARG_COMMENT_ID, commentId);
        activity.startActivityForResult(intent, requestCode);
        activity.startActivity(intent);
    }

    public static void startReportComment(Activity activity, int requestCode, long commentId) {
        Intent intent = createIntent(activity, ACTION_REPORT_COMMENT);
        intent.putExtra(ARG_COMMENT_ID, commentId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_post_action);
        int actionId = getIntent().getIntExtra(ARG_ACTION_ID, -1);
        long commentId = getIntent().getLongExtra(ARG_COMMENT_ID, -1);
        long postId = getIntent().getLongExtra(ARG_POST_ID, -1);
        long tlogId = getIntent().getLongExtra(ARG_TLOG_ID, -1);

        mTitle = (TextView)findViewById(R.id.title);
        mButton = (TextView)findViewById(R.id.delete_comment_button);
        mProgress = findViewById(R.id.delete_comment_progress);

        mButton.setOnClickListener(v -> doAction());

        findViewById(R.id.root).setOnTouchListener((v, event) -> {
            ActivityCompat.finishAfterTransition(DeleteOrReportDialogActivity.this);
            return true;
        });

        switch (actionId) {
            case ACTION_DELETE_POST:
                mActionHandler = new DeletePostActionHandler(tlogId, postId);
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

    public void notifyError(@Nullable Throwable exception, int fallbackResId) {
        Intent intent = new Intent();
        intent.putExtra(Constants.ACTIVITY_RESULT_ARG_ERROR_MESSAGE, UiUtils.getUserErrorText(getResources(), exception, fallbackResId));
        setResult(Constants.ACTIVITY_RESULT_CODE_SHOW_ERROR, intent);
        finish();
    }

    void doAction() {
        mButton.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);

        Observable<Object> observable = mActionHandler.createObservable();

        mSubscription = observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(RestSchedulerHelper.getScheduler())
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
            setResult(RESULT_OK);
            mActionHandler.onNext(o);
        }
    };

    private interface ActionHandler {
        void onCreate();
        Observable<Object> createObservable();
        void onError(Throwable e);
        void onCompleted();
        void onNext(Object o);
    }

    private class DeletePostActionHandler implements ActionHandler {

        private final long mTlogId;

        private final long mPostId;

        public DeletePostActionHandler(long tlogId, long postId) {
            mTlogId = tlogId;
            mPostId = postId;
        }

        @Override
        public void onCreate() {
            mTitle.setText(R.string.delete_post_description);
            mButton.setText(R.string.delete_post_button);
        }

        @Override
        public Observable<Object> createObservable() {
            return RestClient.getApiReposts().deletePost(mTlogId, mPostId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(e, R.string.error_loading_comments);
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
            ApiEntries service = RestClient.getAPiEntries();
            return service.reportEntry(mPostId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(e, R.string.error_report_to_comment);
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
            ApiComments service = RestClient.getAPiComments();
            ApiEntries entriesApi = RestClient.getAPiEntries();

            Observable<Object> delete = service.deleteComment(mCommentId);
            Observable<Entry> updateEntry = entriesApi.getEntry(mPostId, false);

            return delete.concatWith(updateEntry);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(e, R.string.error_loading_comments);
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
            ApiComments service = RestClient.getAPiComments();
            return service.reportComment(mCommentId);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(e, R.string.error_report_to_comment);
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
