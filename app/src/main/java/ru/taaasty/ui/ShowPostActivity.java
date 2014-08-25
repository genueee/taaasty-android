package ru.taaasty.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.widgets.ErrorTextView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ShowPostActivity extends Activity implements ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    public static final String ARG_POST_ID = "post_id";
    private static final int HIDE_ACTION_BAR_DELAY = 500;

    private ActionbarUserIconLoader mAbIconLoader;

    private Handler mHideActionBarHandler;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_post);

        mAbIconLoader = new ActionbarUserIconLoader(this, getActionBar()) {
            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                notifyError(getText(R.string.error_loading_image), null);
            }
        };

        mHideActionBarHandler = new Handler();

        if (savedInstanceState == null) {
            long postId = getIntent().getLongExtra(ARG_POST_ID, -1);
            if (postId < 0) throw new IllegalArgumentException("no ARG_POST_ID");
            setupActionbar(null, null);
            getActionBar().hide();
            Fragment postFragment = ShowPostFragment.newInstance(postId);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, postFragment)
                    .commit();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
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
    public void onPostLoaded(Entry entry) {
        if (entry == null) {
            setupActionbar(null, null);
        } else {
            setupActionbar(entry.getAuthor(), entry.getTitle());
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

    @Override
    public void onBottomReached(int listBottom, int listViewHeight) {
        if (DBG) Log.v(TAG, "onBottomReached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        ActionBar ab = getActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onBottomUnreached() {
        if (DBG) Log.v(TAG, "onBottomUnreached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler.postDelayed(mHideActionBarRunnable, HIDE_ACTION_BAR_DELAY);
    }

    private Runnable mHideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar ab = getActionBar();
            if (ab != null) ab.hide();
        }
    };

    void setupActionbar(User author, String postTitle) {
        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(R.string.title_activity_show_post);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setIcon(R.drawable.avatar_dummy);
            if (author != null) {
                mAbIconLoader.loadIcon(author.getUserpic(), author.getName());
            }
        }
    }
}
