package ru.taaasty.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.ThumborUrlBuilder;

import org.w3c.dom.Text;

import ru.taaasty.R;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

public class ShowPhotoActivity extends Activity {
    public static final String ARG_IMAGE_URL = "ru.taaasty.ui.ShowPhotoActivity.image_url";
    public static final String ARG_TITLE = "ru.taaasty.ui.ShowPhotoActivity.title";
    public static final String ARG_AUTHOR = "ru.taaasty.ui.ShowPhotoActivity.author";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private PhotoViewAttacher mPhotoViewAttacher;

    private boolean isNavigationHidden = false;
    private final Handler mHideActionBarHandler = new Handler();
    private volatile boolean userForcedToChangeOverlayMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        Picasso picasso = NetworkUtils.getInstance().getPicasso(this);

        String imageUrl = getIntent().getStringExtra(ARG_IMAGE_URL);
        String title = getIntent().getStringExtra(ARG_TITLE);
        User author = getIntent().getParcelableExtra(ARG_AUTHOR);

        if (TextUtils.isEmpty(imageUrl)) {
            throw new IllegalStateException("ARG_IMAGE_URL not defined");
        }

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(title == null ? "" : Html.fromHtml(title));
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setupAuthor(author);
        picasso.load(imageUrl).into(mPicassoTarget);

    }

    private void setupAuthor(User author) {
        ActionBar ab;
        Picasso picasso;
        Userpic up;
        String userpicUrl;
        ThumborUrlBuilder b;
        CircleTransformation circleTransformation;
        int avatarDiameter;

        ab = getActionBar();
        if (ab == null) return;
        picasso = NetworkUtils.getInstance().getPicasso(this);
        circleTransformation = new CircleTransformation();
        up = author.getUserpic();
        userpicUrl = up.largeUrl;
        if (TextUtils.isEmpty(userpicUrl)) {
            ab.setIcon(new DefaultUserpicDrawable(up, author.getName()));
            return;
        }

        b = NetworkUtils.createThumborUrl(userpicUrl);
        avatarDiameter = getResources().getDimensionPixelSize(android.R.dimen.app_icon_size);
        if (b != null) {
            userpicUrl = b.resize(avatarDiameter, avatarDiameter)
                    .smart()
                    .toUrl();
            // if (DBG) Log.d(TAG, "userpicUrl: " + userpicUrl);
            picasso.load(userpicUrl)
                    .placeholder(R.drawable.ic_user_stub_dark)
                    .error(R.drawable.ic_user_stub_dark)
                    .transform(circleTransformation)
                    .noFade()
                    .into(mUserAvatarTarget);
        } else {
            picasso.load(userpicUrl)
                    .resize(avatarDiameter, avatarDiameter)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_stub_dark)
                    .error(R.drawable.ic_user_stub_dark)
                    .transform(circleTransformation)
                    .noFade()
                    .into(mUserAvatarTarget);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
    }

    private final Target mUserAvatarTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            ActionBar ab = getActionBar();
            if (ab != null) ab.setIcon(new BitmapDrawable(getResources(), bitmap));
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    private final Target mPicassoTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            findViewById(R.id.progressView).setVisibility(View.GONE);
            PhotoView pv = (PhotoView) findViewById(R.id.picturePhotoView);
            pv.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            mPhotoViewAttacher = new PhotoViewAttacher(pv);
            mPhotoViewAttacher.setOnPhotoTapListener(mOnTapListener);
            runHideActionBarTimer();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            findViewById(R.id.progressView).setVisibility(View.GONE);
            Toast.makeText(ShowPhotoActivity.this, R.string.error_loading_image, Toast.LENGTH_LONG).show();
            finish();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            findViewById(R.id.progressView).setVisibility(View.VISIBLE);
        }
    };

    private final PhotoViewAttacher.OnPhotoTapListener mOnTapListener = new PhotoViewAttacher.OnPhotoTapListener() {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            userForcedToChangeOverlayMode = true;
            toggleShowOrHideHideyBarMode();
        }
    };


    private void runHideActionBarTimer() {
        mHideActionBarHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!userForcedToChangeOverlayMode && !isNavigationHidden) {
                    toggleShowOrHideHideyBarMode();
                }
            }
        }, HIDE_ACTION_BAR_DELAY);
    }

    /**
     * Detects and toggles actionbarOverlay mode (also known as "hidey bar" mode).
     */
    public void toggleShowOrHideHideyBarMode() {

        int newUiOptions = getWindow().getDecorView().getSystemUiVisibility();

        if (!isNavigationHidden) {
            if(Build.VERSION.SDK_INT >= 14) {
                newUiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }
            getActionBar().hide();
            isNavigationHidden = true;
        } else {
            if(Build.VERSION.SDK_INT >= 14) {
                newUiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            }
            getActionBar().show();
            isNavigationHidden = false;
            userForcedToChangeOverlayMode = false;
            runHideActionBarTimer();
        }
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }


}
