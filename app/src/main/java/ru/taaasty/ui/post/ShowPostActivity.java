package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.FragmentActivityBase;
import ru.taaasty.R;
import ru.taaasty.events.YoutubeRecoveryActionPerformed;
import ru.taaasty.model.Comment;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.photo.ShowPhotoActivity;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.widgets.ErrorTextView;

public class ShowPostActivity extends FragmentActivityBase implements ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    public static final String ARG_POST_ID = "ru.taaasty.ui.feeds.ShowPostActivity.post_id";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.feeds.ShowPostActivity.tlog_design";

    private static final int HIDE_ACTION_BAR_DELAY = 500;
    private static final String FRAGMENT_TAG_DELETE_REPORT_COMMENT = "FRAGMENT_TAG_DELETE_REPORT_COMMENT";

    public static final int YOUTUBE_RECOVERY_DIALOG_REQUEST = Activity.RESULT_FIRST_USER;

    private ActionbarUserIconLoader mAbIconLoader;

    private Handler mHideActionBarHandler;

    private boolean mImeVisible;

    private boolean mYoutubeFullscreen = false;

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
            getSupportFragmentManager().beginTransaction()
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == YOUTUBE_RECOVERY_DIALOG_REQUEST) {
            EventBus.getDefault().post(new YoutubeRecoveryActionPerformed());
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
            setupActionbar(null, null, null);
        } else {
            if( entry.getAuthor() != User.DUMMY )
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
    public void onShowImageClicked(User author, List<String> images, String title, String previewUrl) {
        ArrayList<String> imagesList;
        Intent i = new Intent(this, ShowPhotoActivity.class);

        if (images instanceof ArrayList) {
            imagesList = (ArrayList<String>) images;
        } else {
            imagesList = new ArrayList<>(images);
        }
        i.putStringArrayListExtra(ShowPhotoActivity.ARG_IMAGE_URL_LIST, imagesList);
        i.putExtra(ShowPhotoActivity.ARG_TITLE, title);
        i.putExtra(ShowPhotoActivity.ARG_AUTHOR, author);
        if (previewUrl != null) i.putExtra(ShowPhotoActivity.ARG_PREVIEW_URL, previewUrl);

        startActivity(i);
    }

    @Override
    public void onBottomReached(int listBottom, int listViewHeight) {
        if (DBG) Log.v(TAG, "onBottomReached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        if (mYoutubeFullscreen) return;
        ActionBar ab = getActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onBottomUnreached() {
        if (DBG) Log.v(TAG, "onBottomUnreached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        if (mYoutubeFullscreen) return;
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
    public void onDeleteCommentClicked(Comment comment) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_COMMENT) != null) return;
        DialogFragment f = DeleteCommentFragment.newInstance(comment.getId());
        f.show(fm, FRAGMENT_TAG_DELETE_REPORT_COMMENT);
    }

    @Override
    public void onReportCommentClicked(Comment comment) {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_COMMENT) != null) return;
        DialogFragment f = ReportCommentFragment.newInstance(comment.getId());
        f.show(fm, FRAGMENT_TAG_DELETE_REPORT_COMMENT);
    }

    @Override
    public void onYoutubeFullscreen(boolean isFullscreen) {
        mYoutubeFullscreen = isFullscreen;
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
