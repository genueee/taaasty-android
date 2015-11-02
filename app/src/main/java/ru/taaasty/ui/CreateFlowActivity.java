package ru.taaasty.ui;

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
import ru.taaasty.events.FlowUploadStatus;
import ru.taaasty.rest.model.PostFlowForm;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.MessageHelper;

/**
 * Created by alexey on 13.09.15.
 */
public class CreateFlowActivity extends ActivityBase implements CreateFlowFragment.OnFragmentInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "CreateFlowPostAct";

    private ImageView mCreateFlowButton;

    public static void startActivity(Context context, View animateFrom) {
        Intent intent = new Intent(context , CreateFlowActivity.class);
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
        setContentView(R.layout.activity_create_flow);
        if (savedInstanceState == null) {
            Fragment fragment = CreateFlowFragment.newInstance();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
            fragment.setUserVisibleHint(true);
        }
        EventBus.getDefault().register(this);

        setSupportActionBar((Toolbar)findViewById(R.id.toolbar));
        mCreateFlowButton = (ImageView) findViewById(R.id.create_post_button);
        mCreateFlowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateFlowClicked();
            }
        });
        mCreateFlowButton.setEnabled(false);
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

    void onCreateFlowClicked() {
        PostFlowForm post;
        CreateFlowFragment fragment;

        fragment = (CreateFlowFragment)getSupportFragmentManager().findFragmentById(R.id.container);
        post = (PostFlowForm)fragment.getForm();
        IntentService.startPostFlow(this, post);
        setUploadingStatus(true);
    }

    public void onEventMainThread(FlowUploadStatus status) {
        if (status.successfully) {
            Toast.makeText(this, R.string.flow_created, Toast.LENGTH_LONG).show();
            finish();
            TlogActivity.startTlogActivity(this, status.flow.getId(), null);
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
        View progress = findViewById(R.id.create_flow_progress);
        progress.setVisibility(uploading ? View.VISIBLE : View.GONE);
        mCreateFlowButton.setEnabled(!uploading);
    }

    @Override
    public void onValidationStatusChanged(boolean postValid) {
        mCreateFlowButton.setEnabled(postValid);
    }
}
