package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.utils.MessageHelper;

public class CreateAnonymousPostActivity extends ActivityBase implements OnCreatePostInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateAnonmusPostAct";

    private static final int REQUEST_CODE_LOGIN = 1;

    private ImageView mCreatePostButton;
    private View mLoginButton;

    public static void startActivity(Context context, View animateFrom) {
        Intent intent = new Intent(context , CreateAnonymousPostActivity.class);
        if (animateFrom != null && context instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity) context, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_anonyous_post);
        if (savedInstanceState == null) {
            Fragment fragment = CreateTextPostFragment.newCreateAnonymousInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            fragment.setUserVisibleHint(true);
        }
        EventBus.getDefault().register(this);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        mCreatePostButton = (ImageView) findViewById(R.id.create_post_button);
        mLoginButton = findViewById(R.id.login_button);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.create_post_button:
                        onCreatePostClicked();
                        break;
                    case R.id.login_button:
                        LoginActivity.startActivity(CreateAnonymousPostActivity.this, REQUEST_CODE_LOGIN, v);
                        break;
                }
            }
        };

        mCreatePostButton.setOnClickListener(clickListener);
        mLoginButton.setOnClickListener(clickListener);

        mCreatePostButton.setEnabled(false);
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (Session.getInstance().isAuthorized()) {
            mLoginButton.setVisibility(View.GONE);
            mCreatePostButton.setVisibility(View.VISIBLE);
            findViewById(R.id.login_to_create_post).setVisibility(View.GONE);
        } else {
            mLoginButton.setVisibility(View.VISIBLE);
            mCreatePostButton.setVisibility(View.INVISIBLE);
            findViewById(R.id.login_to_create_post).setVisibility(View.VISIBLE);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DBG) Log.v(TAG, "onDestroy()");
        EventBus.getDefault().unregister(this);
    }

    void onCreatePostClicked() {
        PostForm post;
        CreateTextPostFragment fragment;

        fragment = (CreateTextPostFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        post = fragment.getForm();
        IntentService.startPostEntry(this, post);
        setUploadingStatus(true);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully) {
            Toast.makeText(this, R.string.anonymous_post_created, Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Сообщаем об ошибке
            setUploadingStatus(false);
            MessageHelper.showError(
                    this, R.id.main_container, REQUEST_CODE_LOGIN, status.exception, status.errorFallbackResId);
        }
    }

    public void notifyError(CharSequence error, @Nullable Throwable exception) {

    }

    private void setUploadingStatus(boolean uploading) {
        View progress = findViewById(R.id.create_post_progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mCreatePostButton.setEnabled(!uploading);
    }

    @Override
    public void onValidationStatusChanged(boolean postValid) {
        mCreatePostButton.setEnabled(postValid);
    }

    @Override
    public void onChoosePhotoButtonClicked(boolean hasPicture) {
        throw new IllegalStateException();
    }

    @Override
    public void onFragmentAttached(CreatePostFragmentBase fragment) {
    }

    @Override
    public void onFragmentDetached(CreatePostFragmentBase fragment) {
    }
}
