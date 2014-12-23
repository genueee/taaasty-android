package ru.taaasty.ui.post;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.events.EntryRemoved;
import ru.taaasty.model.Entry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.widgets.ErrorTextView;

public class ShowPostActivity extends ActivityBase implements ShowCommentsFragment.OnFragmentInteractionListener, ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    private static final String ARG_POST_ID = "ru.taaasty.ui.feeds.ShowPostActivity.post_id";
    private static final String ARG_ENTRY = "ru.taaasty.ui.feeds.ShowPostActivity.entry";
    private static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.feeds.ShowPostActivity.tlog_design";
    private static final String ARG_SHOW_FULL_POST = "ru.taaasty.ui.feeds.ShowPostActivity.show_full_post";
    private static final String ARG_COMMENT_ID = "ru.taaasty.ui.feeds.ShowPostActivity.comment_id";
    private static final String ARG_THUMBNAIL_BITMAP_CACHE_KEY = "ru.taaasty.ui.feeds.ShowPostActivity.thumbnail_bitmap_cache_key";

    private static final int HIDE_ACTION_BAR_DELAY = 500;

    private ActionbarUserIconLoader mAbIconLoader;

    private Handler mHideActionBarHandler;

    private boolean mShowFullPost;

    private long mPostId;

    public static class Builder {

        private final Context mContext;

        private View mSrcView;

        private Long mPostId;

        private Entry mEntry;

        private TlogDesign mTlogDesign;

        private Long mCommentId;

        private Bitmap mThumbnailBitmap;

        private String mThumbnailBitmapCacheKey;

        private boolean mShowFullPost;

        public Builder(Context context) {
            mContext = context;
        }

        public Builder setEntryId(long entryId) {
            mPostId = entryId;
            return this;
        }

        public Builder setCommentId(Long commentId) {
            mCommentId = commentId;
            return this;
        }

        public Builder setEntry(@Nullable Entry entry) {
            mEntry = entry;
            return this;
        }

        public Builder setDesign(@Nullable TlogDesign design) {
            mTlogDesign = design;
            return this;
        }

        public Builder setSrcView(View view) {
            mSrcView = view;
            return this;
        }

        public Builder setThumbnailBitmap(Bitmap bitmap, @Nullable String cacheKey) {
            mThumbnailBitmap = bitmap;
            mThumbnailBitmapCacheKey = cacheKey;
            return this;
        }

        public Builder setShowFullPost(boolean enable) {
            mShowFullPost = enable;
            return this;
        }


        public Intent buildIntent() {
            if (mPostId == null && mEntry == null) {
                throw new IllegalStateException("post not defined");
            }

            Intent intent = new Intent(mContext, ShowPostActivity.class);
            intent.putExtra(ShowPostActivity.ARG_POST_ID, mEntry == null ? (long)mPostId : mEntry.getId());
            if (mEntry != null) intent.putExtra(ShowPostActivity.ARG_ENTRY, mEntry);
            if (mTlogDesign != null) intent.putExtra(ShowPostActivity.ARG_TLOG_DESIGN, mTlogDesign);
            if (mCommentId != null) intent.putExtra(ShowPostActivity.ARG_COMMENT_ID, mCommentId);

            if (mShowFullPost) intent.putExtra(ShowPostActivity.ARG_SHOW_FULL_POST, mShowFullPost);

            if (mThumbnailBitmap != null) {
                String key = mThumbnailBitmapCacheKey != null ? mThumbnailBitmapCacheKey : "thumbnail";
                ImageUtils.getInstance().putBitmapToCache(key, mThumbnailBitmap);
                intent.putExtra(ARG_THUMBNAIL_BITMAP_CACHE_KEY, key);
            }

            return intent;
        }

        public void startActivity() {
            Intent intent = buildIntent();
            if (mSrcView != null) {
                ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                        mSrcView, 0, 0, mSrcView.getWidth(), mSrcView.getHeight());
                if (mContext instanceof  Activity) {
                    ActivityCompat.startActivity((Activity) mContext, intent, options.toBundle());
                } else {
                    mContext.startActivity(intent);
                }
            } else {
                mContext.startActivity(intent);
            }
        }
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

        mShowFullPost = getIntent().getBooleanExtra(ARG_SHOW_FULL_POST, false);

        if (savedInstanceState == null) {
            // TODO: скролл к комментарию
            mPostId = getIntent().getLongExtra(ARG_POST_ID, -1);
            if (mPostId < 0) throw new IllegalArgumentException("no ARG_USER_ID");
            Entry entry = getIntent().getParcelableExtra(ARG_ENTRY);
            TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);
            String thumbnailKey = getIntent().getStringExtra(ARG_THUMBNAIL_BITMAP_CACHE_KEY);
            setupActionbar(null, null, design);
            if (design != null) {
                getWindow().getDecorView().setBackgroundColor(design.getFeedBackgroundColor(getResources()));
            }

            Fragment fragment;
            if (mShowFullPost) {
                fragment = ShowPostFragment.newInstance(mPostId, entry, design);
            } else {
                fragment = ShowCommentsFragment.newInstance(mPostId, entry, design);
            }

            getFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
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
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                showShareMenu();
                return true;
        }

        return super.onKeyDown(keycode, e);
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
                setupActionbar(entry.getAuthor().getUserpic(), entry.getAuthor().getName(), entry.getAuthor().getDesign());
        }
    }

    @Override
    public void onPostLoadError(Throwable e) {
        String text = "";
        Integer iconId = null;
        // При ошибке загруи поста заменяем фрагмент на фрагмент с сообщением об ошибке
        if (e instanceof NetworkUtils.ResponseErrorException && ((NetworkUtils.ResponseErrorException)e).error.responseCode == 403) {
            text = getString(R.string.error_tlog_access_denied);
            iconId = R.drawable.post_load_error;
        } else {
            if (e instanceof  NetworkUtils.ResponseErrorException) {
                text = ((NetworkUtils.ResponseErrorException)e).error.error;
            } else {
                text = getString(R.string.error_post_comment);
            }
        }

        ErrorLoadingPostFragment newFragment = ErrorLoadingPostFragment.newInstance(text, iconId);
        getFragmentManager().beginTransaction().replace(R.id.container, newFragment).commit();
        ActionBar ab = getActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view);
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        showShareMenu();
    }

    @Override
    public void onEdgeReached(boolean atTop) {
        if (DBG) Log.v(TAG, "onBottomReached atTop: " + atTop);
        if (!atTop) return;
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        ActionBar ab = getActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onEdgeUnreached() {
        if (DBG) Log.v(TAG, "onEdgeUnreached");
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

    private Runnable mHideActionBarRunnable = new Runnable() {
        @Override
        public void run() {
            ActionBar ab = getActionBar();
            if (ab != null) ab.hide();
        }
    };

    private Entry getCurrentEntry() {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.container);

        if (fragment == null) return null;
        if (fragment instanceof  ShowCommentsFragment) {
            return ((ShowCommentsFragment) fragment).getCurrentEntry();
        } else if (fragment instanceof ShowPostFragment) {
            return ((ShowPostFragment) fragment).getCurrentEntry();
        }
        return null;
    }

    void showShareMenu() {
        Entry entry = getCurrentEntry();
        if (entry != null) {
            Intent intent = new Intent(this, SharePostActivity.class);
            intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
            startActivity(intent);
        }
    }

    void setupActionbar(Userpic userpic, String username, TlogDesign design) {
        ActionBar ab = getActionBar();
        if (ab == null) return;

        SpannableString title = new SpannableString(getText(
                mShowFullPost ? R.string.title_activity_show_post : R.string.title_activity_comments));

        ab.setDisplayHomeAsUpEnabled(true);
        ab.setIcon(new ColorDrawable(Color.TRANSPARENT));
        if (userpic != null) {
            mAbIconLoader.loadIcon(userpic, username);
        }
        if (design != null && design.isDarkTheme()) {
            ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark)));
            ForegroundColorSpan textColor = new ForegroundColorSpan(Color.WHITE);
            title.setSpan(textColor, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_light)));
        }

        ab.setTitle(title);
    }

    public void onEventMainThread(EntryRemoved event) {
        if( event.postId == mPostId) {
            finish();
        }
    }
}
