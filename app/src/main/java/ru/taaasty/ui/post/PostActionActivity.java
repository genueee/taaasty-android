package ru.taaasty.ui.post;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionLoginBehavior;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.FragmentActivityBase;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.ui.CustomErrorView;

public class PostActionActivity extends FragmentActivityBase implements CustomErrorView {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.PostActionActivity.entry";

    public static final String ACTION_DELETE = "ru.taaasty.ui.post.PostActionActivity.delete";
    public static final String ACTION_REPORT = "ru.taaasty.ui.post.PostActionActivity.report";
    public static final String ACTION_SHARE_FACEBOOK = "ru.taaasty.ui.post.PostActionActivity.share_in_facebook";

    private static final String FRAGMENT_TAG_DELETE_REPORT_POST = "FRAGMENT_TAG_DELETE_REPORT_POST";

    private Entry mEntry;
    private String mAction;

    private static final String TAG = "PostActionActivity";

    private UiLifecycleHelper mUiHelper;

    private Session.StatusCallback mSessionStatusCallback;
    private Session mCurrentSession = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_action);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        mAction = getIntent().getAction();

        mUiHelper = new UiLifecycleHelper(this, null);
        mUiHelper.onCreate(savedInstanceState);

        if( ACTION_DELETE.equals(mAction) ) {
            // show delete post dialog
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_POST) != null) return;
            DialogFragment f = DeletePostFragment.newInstance(mEntry.getId());
            f.show(fm, FRAGMENT_TAG_DELETE_REPORT_POST);
        }
        else if( ACTION_REPORT.equals(mAction) ) {
            // show report post dialog
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_POST) != null) return;
            DialogFragment f = ReportPostFragment.newInstance(mEntry.getId());
            f.show(fm, FRAGMENT_TAG_DELETE_REPORT_POST);
        }
        else if(ACTION_SHARE_FACEBOOK.equals(mAction)) {
            if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
                    FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
                // Publish the post using the Share Dialog (есть Facebook клиент)
                FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
                        .setLink(mEntry.getEntryUrl())
                        .setDescription(getString(R.string.facebook_sharing_description))
                        .build();
                mUiHelper.trackPendingDialogCall(shareDialog.present());

            } else {
                // create instace for sessionStatusCallback
                mSessionStatusCallback = new Session.StatusCallback() {

                    @Override
                    public void call(Session session, SessionState state, Exception exception) {
                        onSessionStateChange(session, state, exception);

                    }
                };
                connectToFB();
            }
        }
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mUiHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mUiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        mUiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mUiHelper.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(mCurrentSession != null )
            mCurrentSession.onActivityResult(this, requestCode, resultCode, data);

        mUiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Toast.makeText(PostActionActivity.this, R.string.facebook_sharing_fail, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Toast.makeText(PostActionActivity.this, R.string.facebook_sharing_ok, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    public void connectToFB() {

        List<String> permissions = new ArrayList<String>();
        permissions.add("publish_stream");

        mCurrentSession = new Session.Builder(this).build();
        mCurrentSession.addCallback(mSessionStatusCallback);

        Session.OpenRequest openRequest = new Session.OpenRequest(this);
        openRequest.setLoginBehavior(SessionLoginBehavior.SUPPRESS_SSO);
        openRequest.setRequestCode(Session.DEFAULT_AUTHORIZE_ACTIVITY_CODE);
        openRequest.setPermissions(permissions);
        mCurrentSession.openForPublish(openRequest);

    }

    private void onSessionStateChange(Session session, SessionState state, Exception exception) {
        if (session != mCurrentSession) {
            return;
        }

        if (state.isOpened()) {
            publishFeedDialog();
        } else if (state.isClosed()) {
        }
    }

    private void publishFeedDialog() {
        Bundle params = new Bundle();
        params.putString("link", mEntry.getEntryUrl());
        params.putString("description", getString(R.string.facebook_sharing_description));

        WebDialog feedDialog = (
            new WebDialog.FeedDialogBuilder(this, mCurrentSession, params))
            .setOnCompleteListener(new WebDialog.OnCompleteListener() {
                @Override
                public void onComplete(Bundle values, FacebookException error) {
                    if (error == null) {
                        // When the story is posted, echo the success
                        // and the post Id.
                        final String postId = values.getString("post_id");
                        if (postId != null) {
                            Toast.makeText(PostActionActivity.this, R.string.facebook_sharing_ok, Toast.LENGTH_LONG).show();
                        } else {
                            // User clicked the Cancel button
                        }
                    } else if (error instanceof FacebookOperationCanceledException) {
                        // User clicked the "x" button
                    } else {
                        // Generic, ex: network error
                        Toast.makeText(PostActionActivity.this, R.string.facebook_sharing_fail, Toast.LENGTH_LONG).show();
                    }
                    finish();
                }
            })
            .build();
        feedDialog.show();
    }
}
