package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.TlogInfo;
import ru.taaasty.rest.model.User;
import ru.taaasty.ui.OnBackPressedListener;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.FeedBackground;
import ru.taaasty.widgets.ErrorTextView;


public class TlogActivity extends ActivityBase implements TlogFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogActivity";

    private static final String ARG_USER_ID = "ru.taaasty.ui.feeds.TlogActivity.user_id";
    private static final String ARG_AVATAR_THUMBNAIL_RES = "ru.taaasty.ui.feeds.TlogActivity.avatar_thumbnail_res";
    private static final String ARG_USER_SLUG = "ru.taaasty.ui.feeds.TlogActivity.user_slug";

    private FeedBackground mFeedBackground;

    public static Intent getStartTlogActivityIntent(Context source, long userId,  int avatarThumbnailSizeRes) {
        Intent intent = new Intent(source, TlogActivity.class);
        intent.putExtra(ARG_USER_ID, userId);
        intent.putExtra(ARG_AVATAR_THUMBNAIL_RES, avatarThumbnailSizeRes);
        return intent;
    }

    public static Intent getStartTlogActivityIntent(Context source, long userId) {
        return getStartTlogActivityIntent(source, userId, R.dimen.avatar_extra_small_diameter_34dp);
    }

    public static void startTlogActivity(Context source, long userId, View animateFrom, int avatarThumbnailSizeRes) {
        Intent intent = getStartTlogActivityIntent(source, userId, avatarThumbnailSizeRes);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity)source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    public static void startTlogActivity(Context source, long userId, View animateFrom) {
        startTlogActivity(source, userId, animateFrom, R.dimen.avatar_extra_small_diameter_34dp);
    }

    public static void startTlogActivity(Context source, String userSlug, View animateFrom) {
        Intent intent = new Intent(source, TlogActivity.class);
        intent.putExtra(ARG_USER_SLUG, userSlug);
        if (animateFrom != null && source instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivity((Activity)source, intent, options.toBundle());
        } else {
            source.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tlog);

        View container = findViewById(R.id.container);
        mFeedBackground = new FeedBackground(container, null, R.dimen.feed_header_height);

        // Используем background у контейнера. Там стоит тот же background, что и у activity - так и должно быть,
        // иначе на nexus 5 в landscape справа граница неправильная из-за того, что там правее
        // системные кнопки и background на activity лежит под ними.
        getWindow().getDecorView().setBackgroundDrawable(null);


        if (savedInstanceState == null) {
            Fragment tlogFragment;

            if (getIntent().hasExtra(ARG_USER_ID)) {
                long userId = getIntent().getLongExtra(ARG_USER_ID, -1);
                tlogFragment = TlogFragment.newInstance(userId, getIntent().getIntExtra(ARG_AVATAR_THUMBNAIL_RES, 0));
            } else {
                String userIdOrSlug = getIntent().getStringExtra(ARG_USER_SLUG);
                tlogFragment = TlogFragment.newInstance(userIdOrSlug);
            }

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, tlogFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_refresh:
                refreshData();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null && fragment instanceof OnBackPressedListener) {
            if (((OnBackPressedListener) fragment).onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            ert.setError(error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            ert.setError(error, exception);
        }
    }

    @Override
    public void onTlogInfoLoaded(TlogInfo tlogInfo) {
        if (tlogInfo.getDesign() != null) {
            mFeedBackground.setTlogDesign(tlogInfo.getDesign());
        }
    }

    @Override
    public void onSharePostMenuClicked(Entry entry, long tlogId) {
        SharePostActivity.startActivity(this, entry, tlogId);
    }

    @Override
    public void onNoSuchUser() {
        Toast.makeText(this, getString(R.string.error_user_with_this_name_not_found), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view, R.dimen.avatar_extra_small_diameter_34dp);
    }

    @Override
    public void onListScroll(int dy, int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        mFeedBackground.setHeaderVisibleFraction(firstVisibleItem == 0 ? firstVisibleFract : 0);
    }

    private void refreshData() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) ((IRereshable)fragment).refreshData(true);
    }
}