package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.widgets.ErrorTextView;

public class ShowPostActivity extends ActivityBase implements ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    public static final String ARG_POST_ID = "ru.taaasty.ui.feeds.ShowPostActivity.post_id";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.feeds.ShowPostActivity.tlog_design";
    private static final int HIDE_ACTION_BAR_DELAY = 500;

    private ActionbarUserIconLoader mAbIconLoader;

    private Handler mHideActionBarHandler;

    private boolean mImeVisible;

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
            if (postId < 0) throw new IllegalArgumentException("no ARG_USER_ID");
            TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);
            setupActionbar(null, null, design);
            getActionBar().hide();
            if (design != null) {
                getWindow().getDecorView().setBackgroundColor(design.getFeedBackgroundColor(getResources()));
            }
            Fragment postFragment = ShowPostFragment.newInstance(postId, design);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, postFragment)
                    .commit();
        }

        final View activityRootView = findViewById(R.id.activity_root);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                    mImeVisible = true;
                } else {
                    mImeVisible = false;
                }
                // if (DBG) Log.v(TAG, "ime visible: " + mImeVisible);
            }
        });
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
            setupActionbar(null, null, null);
        } else {
            setupActionbar(entry.getAuthor().getUserpic(), entry.getAuthor().getSlug(), entry.getAuthor().getDesign());
        }
    }

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        Intent i = new Intent(this, TlogActivity.class);
        i.putExtra(TlogActivity.ARG_USER_ID, user.getId());
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

    @Override
    public void setPostBackgroundColor(int color) {
        Drawable from, to;
        TransitionDrawable trasition;

        from = getWindow().getDecorView().getBackground();
        if (from == null) {
            getWindow().getDecorView().setBackgroundColor(color);
            return;
        }
        to = new ColorDrawable(color);
        trasition = new TransitionDrawable(new Drawable[]{from, to});
        getWindow().setBackgroundDrawable(trasition);
        trasition.startTransition(Constants.IMAGE_FADE_IN_DURATION);
    }

    @Override
    public boolean isImeVisible() {
        return mImeVisible;
    }

    private Runnable mHideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar ab = getActionBar();
            if (ab != null) ab.hide();
        }
    };

    void setupActionbar(Userpic userpic, String username, TlogDesign design) {
        ActionBar ab = getActionBar();
        if (ab == null) return;
        SpannableString title = new SpannableString(getText(R.string.title_activity_show_post));

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
        if (userpic != null) {
            mAbIconLoader.loadIcon(userpic, username);
        }
        if (design != null && design.isDarkTheme()) {
            ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar)));
            ForegroundColorSpan textColor = new ForegroundColorSpan(Color.WHITE);
            title.setSpan(textColor, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            ab.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }

        ab.setTitle(title);
    }
}
