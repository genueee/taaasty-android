package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.widgets.ErrorTextView;

public class ShowPostActivity extends Activity implements ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    public static final String ARG_POST_ID = "post_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_post);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (savedInstanceState == null) {
            long postId = getIntent().getLongExtra(ARG_POST_ID, -1);
            if (postId < 0) throw new IllegalArgumentException("no ARG_POST_ID");
            Fragment postFragment = ShowPostFragment.newInstance(postId);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, postFragment)
                    .commit();
        }
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

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    @Override
    public void onShowImageClicked(User author, List<ImageInfo> images, String title) {
        Intent i = new Intent(this, ShowPhotoActivity.class);

        ArrayList<ImageInfo> imagesList = new ArrayList<>(images);
        i.putParcelableArrayListExtra(ShowPhotoActivity.ARG_IMAGE_URL_LIST, imagesList);
        i.putExtra(ShowPhotoActivity.ARG_TITLE, title);
        i.putExtra(ShowPhotoActivity.ARG_AUTHOR, author);
        startActivity(i);
    }
}
