package ru.taaasty.ui.post;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
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
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.model.Userpic;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;

public class ShowPostActivity extends ActivityBase implements ShowCommentsFragment.OnFragmentInteractionListener, ShowPostFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ShowPostActivity";

    private static final String ARG_POST_ID = "ru.taaasty.ui.feeds.ShowPostActivity.post_id";
    private static final String ARG_TLOG_ID = "ru.taaasty.ui.feeds.ShowPostActivity.tlog_id";
    private static final String ARG_ENTRY = "ru.taaasty.ui.feeds.ShowPostActivity.entry";
    private static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.feeds.ShowPostActivity.tlog_design";
    private static final String ARG_SHOW_FULL_POST = "ru.taaasty.ui.feeds.ShowPostActivity.show_full_post";
    private static final String ARG_COMMENT_ID = "ru.taaasty.ui.feeds.ShowPostActivity.comment_id";
    private static final String ARG_THUMBNAIL_BITMAP_CACHE_KEY = "ru.taaasty.ui.feeds.ShowPostActivity.thumbnail_bitmap_cache_key";

    private static final int HIDE_ACTION_BAR_DELAY = 500;

    public static final int REQUEST_CODE_SHARE = 3;

    private Handler mHideActionBarHandler;

    private boolean mShowFullPost;

    private long mPostId;

    /**
     * ID тлога, при просмотре которого был открыт этот пост. Нужен при удалении постов, например
     */
    private long mTlogId;

    public static class Builder {

        private final Context mContext;

        private View mSrcView;

        private Long mPostId;

        private Entry mEntry;

        private TlogDesign mTlogDesign;

        private Long mCommentId;

        private Long mTlogId;

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

        public Builder setTlogId(long tlogId) {
            mTlogId = tlogId;
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
            if (mTlogId != null) intent.putExtra(ShowPostActivity.ARG_TLOG_ID, mTlogId.longValue());
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
        TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);
        Entry entry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (entry != null && entry.getAuthor() != null && design == null) design = entry.getAuthor().getDesign();
        if (design != null) {
            getWindow().getDecorView().setBackgroundResource(design.getFeedBackgroundDrawable());
            if (design.isLightTheme()) {
                setTheme(R.style.AppThemeLightOverlayingActionBar);
            } else {
                setTheme(R.style.AppThemeOverlayingActionBar);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_post);

        mShowFullPost = getIntent().getBooleanExtra(ARG_SHOW_FULL_POST, false);

        //noinspection ConstantConditions
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mHideActionBarHandler = new Handler();

        mPostId = getIntent().getLongExtra(ARG_POST_ID, -1);
        if (mPostId < 0) throw new IllegalArgumentException("no ARG_USER_ID");
        mTlogId = getIntent().getLongExtra(ARG_TLOG_ID, -1);

        if (savedInstanceState == null) {
            // TODO: скролл к комментарию

            String thumbnailKey = getIntent().getStringExtra(ARG_THUMBNAIL_BITMAP_CACHE_KEY);
            setupActionbar(null, null, design);

            Fragment fragment;
            if (mShowFullPost) {
                fragment = ShowPostFragment.newInstance(mPostId, entry, design);
            } else {
                fragment = ShowCommentsFragment.newInstance(mPostId, entry, design);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SHARE) {
            SharePostActivity.handleActivityResult(this, findViewById(R.id.activity_root), resultCode, data);
        }
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
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
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
        if (e instanceof ApiErrorException && ((ApiErrorException)e).isError403Forbidden()) {
            text = getString(R.string.error_tlog_access_denied);
            iconId = R.drawable.post_load_error;
        } else if (e instanceof ApiErrorException && ((ApiErrorException)e).isError404NotFound()) {
            // С сервера приходит "Такой публикация не существует". Ставим свой текст
            text = getString(R.string.error_post_not_found);
        } else {
            text = UiUtils.getUserErrorText(getResources(), e, R.string.error_post_comment);
        }

        ErrorLoadingPostFragment newFragment = ErrorLoadingPostFragment.newInstance(text, iconId);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, newFragment).commit();
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view, R.dimen.feed_header_avatar_normal_diameter);
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
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.show();
    }

    @Override
    public void onEdgeUnreached() {
        if (DBG) Log.v(TAG, "onEdgeUnreached");
        mHideActionBarHandler.removeCallbacks(mHideActionBarRunnable);
        mHideActionBarHandler.postDelayed(mHideActionBarRunnable, HIDE_ACTION_BAR_DELAY);

    }

    @Override
    public void setPostBackground(@DrawableRes int background, boolean animate) {
        Drawable from, to;

        from = getWindow().getDecorView().getBackground();
        to = ResourcesCompat.getDrawable(getResources(), background, null);

        if (from != null && from.equals(to)) return;

        if (animate) {
            TransitionDrawable transition;
            transition = new TransitionDrawable(new Drawable[]{from, to});
            getWindow().setBackgroundDrawable(transition);
            transition.startTransition(Constants.IMAGE_FADE_IN_DURATION);
        } else {
            getWindow().setBackgroundDrawable(to);
        }
    }

    @Override
    public void onDeleteCommentClicked(long postId, long commentId) {
        DeleteOrReportDialogActivity.startDeleteComment(this, REQUEST_CODE_SHARE, mPostId, commentId);
    }

    @Override
    public void onReportCommentClicked(long postId, long commentId) {
        DeleteOrReportDialogActivity.startReportComment(this, REQUEST_CODE_SHARE, commentId);
    }

    private Runnable mHideActionBarRunnable = () -> {
        ActionBar ab = getSupportActionBar();
        if (ab != null) ab.hide();
    };

    private Entry getCurrentEntry() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);

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
            if (mTlogId > 0) {
                SharePostActivity.startActivity(this, entry, mTlogId, REQUEST_CODE_SHARE);
            } else {
                SharePostActivity.startActivity(this, entry, REQUEST_CODE_SHARE);
            }
        }
    }

    void setupActionbar(Userpic userpic, String username, TlogDesign design) {
        ActionBar ab = getSupportActionBar();
        if (ab == null) return;

        SpannableString title = new SpannableString(getText(
                mShowFullPost ? R.string.title_activity_show_post : R.string.title_activity_comments));

        if (design != null) {
            if (design.isDarkTheme()) {
                ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_dark)));
                if (DBG) Log.v(TAG, "setupActionbar dark theme");
            } else {
                ab.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar_light)));
                ForegroundColorSpan textColor = new ForegroundColorSpan(Color.BLACK);
                title.setSpan(textColor, 0, title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if (DBG) Log.v(TAG, "setupActionbar light theme");
            }
        }

        setTitle(title);
    }

    public void onEventMainThread(EntryRemoved event) {
        if( event.postId == mPostId) {
            finish();
        }
    }
}
