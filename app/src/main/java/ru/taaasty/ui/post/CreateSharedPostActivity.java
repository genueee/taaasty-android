package ru.taaasty.ui.post;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.utils.MessageHelper;
import ru.taaasty.utils.UiUtils;

public class CreateSharedPostActivity extends ActivityBase implements
        OnCreatePostInteractionListener,
        SelectPhotoSourceDialogFragment.SelectPhotoSourceDialogListener,
        CreateEmbeddPostFragment.InteractionListener,
        EmbeddMenuDialogFragment.OnDialogInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateShrdPostAct";

    private ImageView mCreatePostButton;

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

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_embedd_post);

        boolean isImageShare = false;
        boolean isTextShare = false;
        Intent intent = getIntent();
        if (intent.getType() != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            if ("text/plain".equals(intent.getType())) {
                isTextShare = true;
            } else if (intent.getType().startsWith("image/")) {
                isImageShare = true;
            }
        }

        if (savedInstanceState == null) {
            Fragment fragment = null;

            if (isTextShare) {
                fragment = CreateEmbeddPostFragment.newInstance(null, intent);
            } else if (isImageShare) {
                fragment = CreateImagePostFragment.newInstance(null, UiUtils.getSharedImageUri(intent));
            } else {
                fragment = CreateEmbeddPostFragment.newInstance(null, null);
            }

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            fragment.setUserVisibleHint(true);
        }
        EventBus.getDefault().register(this);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        setTitle(isImageShare ? R.string.title_image_post : R.string.title_embedd_post);

        mCreatePostButton = (ImageView) findViewById(R.id.create_post_button);
        mCreatePostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreatePostClicked();
            }
        });
        mCreatePostButton.setEnabled(false);
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
        CreatePostFragmentBase fragment;

        fragment = (CreatePostFragmentBase)getSupportFragmentManager().findFragmentById(R.id.container);
        post = fragment.getForm();
        IntentService.startPostEntry(this, post);
        setUploadingStatus(true);
    }

    public void onEventMainThread(EntryUploadStatus status) {
        if (!status.isFinished()) return;
        if (status.successfully) {
            Toast.makeText(this, R.string.post_created, Toast.LENGTH_LONG).show();
            finish();
        } else {
            // Сообщаем об ошибке
            setUploadingStatus(false);
            notifyError(status.error, status.exception);
        }
    }

    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            MessageHelper.showError(this, error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            MessageHelper.showError(this, error, exception);
        }
    }

    private void setUploadingStatus(boolean uploading) {
        View progress = findViewById(R.id.progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mCreatePostButton.setEnabled(!uploading);
    }


    @Override
    public void onValidationStatusChanged(boolean postValid) {
        mCreatePostButton.setEnabled(postValid);
    }

    @Override
    public void doShowEmbeddMenuDialog(EmbeddMenuDialogFragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        Fragment old = fm.findFragmentByTag("EmbeddMenuDialogFragment");
        FragmentTransaction ft = fm.beginTransaction();
        if (old != null) ft.remove(old);
        fragment.show(ft, "EmbeddMenuDialogFragment");
    }

    @Override
    public void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId) {
        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onEmbeddMenuDialogItemSelected(dialog, resId);
    }

    @Override
    public void onEmbeddMenuDialogDismissed(DialogInterface dialog) {
        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onEmbeddMenuDialogDismissed(dialog);
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        if (DBG) Log.v(TAG, "onPickPhotoSelected");
        CreateImagePostFragment f = (CreateImagePostFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        if (f != null) f.onPickPhotoSelected();
    }

    @Override
    public void onChoosePhotoButtonClicked(boolean hasPicture) {
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(hasPicture);
        dialog.show(getSupportFragmentManager(), "SelectPhotoSourceDialogFragment");
    }

    @Override
    public void onFragmentAttached(CreatePostFragmentBase fragment) {
    }

    @Override
    public void onFragmentDetached(CreatePostFragmentBase fragment) {
    }


    @Override
    public void onMakePhotoSelected(Fragment fragment) {
        if (DBG) Log.v(TAG, "onMakePhotoSelected");
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onMakePhotoSelected();
    }

    @Override
    public void onDeletePhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onDeleteImageClicked();
    }

    @Override
    public void onFeatherPhotoSelected(Fragment fragment) {
        CreateImagePostFragment f = getCurrentImagePostFragment();
        if (f != null) f.onFeatherPhotoClicked();
    }

    CreateImagePostFragment getCurrentImagePostFragment() {
        return (CreateImagePostFragment)getSupportFragmentManager().findFragmentById(R.id.container);
    }
}
