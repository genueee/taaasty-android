package ru.taaasty.ui.post;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.IntentService;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.ErrorTextView;

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
                fragment = CreateEmbeddPostFragment.newInstance(intent);
            } else if (isImageShare) {
                fragment = CreateImagePostFragment.newInstance(UiUtils.getSharedImageUri(intent));
            } else {
                fragment = CreateEmbeddPostFragment.newInstance(null);
            }

            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            fragment.setUserVisibleHint(true);
        }
        EventBus.getDefault().register(this);

        final ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowCustomEnabled(true);
            ab.setCustomView(R.layout.ab_custom_create_post);
            ab.setTitle(isImageShare ? R.string.title_image_post : R.string.title_embedd_post);

            mCreatePostButton = (ImageView)ab.getCustomView().findViewById(R.id.create_post_button);
            mCreatePostButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onCreatePostClicked();
                }
            });
            mCreatePostButton.setEnabled(false);
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
        CreatePostFragmentBase fragment;

        fragment = (CreatePostFragmentBase)getFragmentManager().findFragmentById(R.id.container);
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
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()));
        } else {
            ert.setError(error);
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
        FragmentManager fm = getFragmentManager();
        Fragment old = fm.findFragmentByTag("EmbeddMenuDialogFragment");
        FragmentTransaction ft = fm.beginTransaction();
        if (old != null) ft.remove(old);
        fragment.show(ft, "EmbeddMenuDialogFragment");
    }

    @Override
    public void onEmbeddMenuDialogItemSelected(DialogInterface dialog, int resId) {
        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onEmbeddMenuDialogItemSelected(dialog, resId);
    }

    @Override
    public void onEmbeddMenuDialogDismissed(DialogInterface dialog) {
        CreateEmbeddPostFragment fragment = (CreateEmbeddPostFragment)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.onEmbeddMenuDialogDismissed(dialog);
    }

    @Override
    public void onPickPhotoSelected(Fragment fragment) {
        if (DBG) Log.v(TAG, "onPickPhotoSelected");
        CreateImagePostFragment f = (CreateImagePostFragment)getFragmentManager().findFragmentById(R.id.container);
        if (f != null) f.onPickPhotoSelected();
    }

    @Override
    public void onChoosePhotoButtonClicked(boolean hasPicture) {
        DialogFragment dialog = SelectPhotoSourceDialogFragment.createInstance(hasPicture);
        dialog.show(getFragmentManager(), "SelectPhotoSourceDialogFragment");
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
        return (CreateImagePostFragment)getFragmentManager().findFragmentById(R.id.container);
    }
}
