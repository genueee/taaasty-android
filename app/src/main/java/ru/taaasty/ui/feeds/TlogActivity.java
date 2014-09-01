package ru.taaasty.ui.feeds;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.util.Log;
import android.view.MenuItem;

import com.squareup.picasso.Picasso;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.widgets.AlphaForegroundColorSpan;
import ru.taaasty.widgets.ErrorTextView;


public class TlogActivity extends ActivityBase implements TlogFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TlogActivity";

    public static final String ARG_USER_ID = "ru.taaasty.ui.feeds.TlogActivity.user_id";

    private Drawable mAbBackgroundDrawable;
    private Drawable mAbIconDrawable;
    int mLastAlpha = 0;

    private AlphaForegroundColorSpan mAlphaForegroundColorSpan;
    private SpannableString mAbTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tlog);

        mAbTitle = new SpannableString("");
        mAlphaForegroundColorSpan = new AlphaForegroundColorSpan(Color.WHITE);

        mAbBackgroundDrawable = new ColorDrawable(getResources().getColor(R.color.semi_transparent_action_bar));
        mAbBackgroundDrawable.setAlpha(0);
        mAbIconDrawable = new ColorDrawable(Color.TRANSPARENT);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setBackgroundDrawable(mAbBackgroundDrawable);
        }

        if (savedInstanceState == null) {
            long userId = getIntent().getLongExtra(ARG_USER_ID, -1);
            if (userId < 0) throw new IllegalArgumentException("no ARG_USER_ID");
            Fragment tlogFragment = TlogFragment.newInstance(userId);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, tlogFragment)
                    .commit();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
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
    public void setFeedBackgroundColor(int color) {
        getWindow().getDecorView().setBackgroundColor(color);
    }

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        if (user == null) return;
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }

    @Override
    public void onAuthorLoaded(User author) {
        mAbTitle = new SpannableString(author.getSlug());
        mAbTitle.setSpan(mAlphaForegroundColorSpan, 0, mAbTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ImageUtils.getInstance().loadAvatar(this, author.getUserpic(), author.getSlug(),
                mPicassoTarget, android.R.dimen.app_icon_size);
    }

    @Override
    public void onListScroll(int firstVisibleItem, float firstVisibleFract, int visibleCount, int totalCount) {
        float abAlpha;
        int intAlpha;

        if (totalCount == 0 || visibleCount == totalCount) {
            abAlpha = 0;
        } else {
            if (totalCount > 5) totalCount = 5;
            abAlpha = (firstVisibleItem + firstVisibleFract) / (float)totalCount;
            abAlpha = UiUtils.clamp(abAlpha, 0f, 1f);
        }

        intAlpha = (int)(255f * abAlpha);

        if (intAlpha == 0
                || (intAlpha == 255 && mLastAlpha != 255)
                || Math.abs(mLastAlpha - intAlpha) > 20) {
            mLastAlpha = intAlpha;
            mAbBackgroundDrawable.setAlpha(intAlpha);
            mAbIconDrawable.setAlpha(intAlpha);
            if (mAbTitle != null) {
                mAlphaForegroundColorSpan.setAlpha(abAlpha);
                getActionBar().setTitle(mAbTitle);
            }
        }
    }

    private final ImageUtils.DrawableTarget mPicassoTarget = new ImageUtils.DrawableTarget() {
        @Override
        public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
            mAbIconDrawable = new BitmapDrawable(getResources(), bitmap);
            mAbIconDrawable.setAlpha(mLastAlpha);
            getActionBar().setIcon(mAbIconDrawable);
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            notifyError(getText(R.string.error_loading_image), null);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }

        @Override
        public void onDrawableReady(Drawable drawable) {
            mAbIconDrawable = drawable;
            mAbIconDrawable.setAlpha(mLastAlpha);
            getActionBar().setIcon(mAbIconDrawable);
        }
    };
}