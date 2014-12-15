package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.events.CommentChanged;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Entry;
import ru.taaasty.service.ApiComments;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class FastReplyDialogActivity extends Activity {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FastReplyDialogActivity";

    private static final String ARG_ENTRY = "ru.taaasty.ui.post.FastReplyDialogActivity.ARG_ENTRY";
    private static final String ARG_COMMENT = "ru.taaasty.ui.post.FastReplyDialogActivity.ARG_COMMENT";

    private ApiComments mCommentsService;
    private Subscription mPostCommentSubscription = SubscriptionHelper.empty();

    private EditText mReplyText;
    private View mSendButton;
    private View mProgress;

    private Entry mEntry;

    @Nullable
    private Comment mComment;

    private boolean mImeVisible;

    private boolean watchForIme = true;

    public static void startReplyToComment(Context context, Entry entry, Comment comment) {
        Intent intent = new Intent(context, FastReplyDialogActivity.class);
        intent.putExtra(ARG_ENTRY, entry);
        intent.putExtra(ARG_COMMENT, comment);
        context.startActivity(intent);
    }

    public static void startReplyToPost(Context context, Entry entry) {
        Intent intent = new Intent(context, FastReplyDialogActivity.class);
        intent.putExtra(ARG_ENTRY, entry);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fast_reply_dialog);

        mCommentsService = NetworkUtils.getInstance().createRestAdapter().create(ApiComments.class);

        mReplyText = (EditText)findViewById(R.id.reply_to_comment_text);
        mSendButton = findViewById(R.id.reply_to_comment_button);
        mProgress = findViewById(R.id.reply_to_comment_progress);

        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        mComment = getIntent().getParcelableExtra(ARG_COMMENT);

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendReply();
            }
        });

        mReplyText.setEnabled(true);
        mReplyText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.send_reply_to_comment
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_SEND) {
                    sendReply();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            initReplyText();
        }

        mReplyText.requestFocus();

        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ActivityCompat.finishAfterTransition(FastReplyDialogActivity.this);
                return true;
            }
        });

        // Следим за софтовой клавиатурой. Как юзер её прячет - закрываемся
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    onImeVisibilityChanged(true);
                    mImeVisible = true;
                } else {
                    onImeVisibilityChanged(false);
                }
                // if (DBG) Log.v(TAG, "ime visible: " + mImeVisible);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPostCommentSubscription.unsubscribe();
    }

    private void initReplyText() {
        String username;

        if (mComment != null) {
            username = mComment.getAuthor().getName();
        } else {
            username = mEntry.getAuthor().getName();
        }

        mReplyText.setText("@" + username + ", ");

    }

    private void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
        }
    }

    void onImeVisibilityChanged(boolean newStatusVisible) {
        if (!watchForIme) return;
        if (newStatusVisible == mImeVisible) return;
        if (mImeVisible && !newStatusVisible) {
            ActivityCompat.finishAfterTransition(FastReplyDialogActivity.this);
        }
        mImeVisible = newStatusVisible;
    }

    void sendReply() {
        String comment = mReplyText.getText().toString();

        if (comment.isEmpty() || comment.matches("(\\@\\w+\\,?\\s*)+")) {
            Toast t = Toast.makeText(this, R.string.please_write_something, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            return;
        }

        mPostCommentSubscription.unsubscribe();

        Observable<Comment> observablePost = AndroidObservable.bindActivity(this,
                mCommentsService.postComment(mEntry.getId(), comment));

        watchForIme = false;
        mReplyText.setEnabled(false);
        mProgress.setVisibility(View.VISIBLE);
        mSendButton.setVisibility(View.INVISIBLE);
        mPostCommentSubscription = observablePost
                .observeOn(AndroidSchedulers.mainThread())
                .finallyDo(new Action0() {
                    @Override
                    public void call() {
                        mReplyText.setEnabled(true);
                        mProgress.setVisibility(View.INVISIBLE);
                        mSendButton.setVisibility(View.VISIBLE);
                    }
                })
                .subscribe(mPostCommentObserver);
    }

    private final Observer<Comment> mPostCommentObserver = new Observer<Comment>() {
        @Override
        public void onCompleted() {
            if (mReplyText != null) mReplyText.setText("");
            mEntry.setCommentsCount(mEntry.getCommentsCount() + 1);
            // TODO: здесь надо обновлять с сервера
            EventBus.getDefault().post(new EntryChanged(mEntry));
            watchForIme = false;
            ActivityCompat.finishAfterTransition(FastReplyDialogActivity.this);
        }

        @Override
        public void onError(Throwable e) {
            notifyError(getString(R.string.error_post_comment), e);
            watchForIme = true;
        }

        @Override
        public void onNext(Comment comment) {
            EventBus.getDefault().post(new CommentChanged(mEntry, comment));
        }
    };

}
