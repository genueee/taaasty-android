package ru.taaasty.ui.photo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.R;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.User;
import ru.taaasty.model.Userpic;
import ru.taaasty.ui.DefaultUserpicDrawable;
import ru.taaasty.utils.CircleTransformation;
import ru.taaasty.utils.NetworkUtils;

public class ShowPhotoActivity extends Activity implements ShowPhotoFragment.OnFragmentInteractionListener {
    public static final String ARG_IMAGE_URL_LIST = "ru.taaasty.ui.photo.ShowPhotoActivity.image_url_list";
    public static final String ARG_TITLE = "ru.taaasty.ui.photo.ShowPhotoActivity.title";
    public static final String ARG_AUTHOR = "ru.taaasty.ui.photo.ShowPhotoActivity.author";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private boolean isNavigationHidden = false;
    private final Handler mHideActionBarHandler = new Handler();
    private volatile boolean userForcedToChangeOverlayMode = false;

    private PhotoAdapter mAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        Picasso picasso = NetworkUtils.getInstance().getPicasso(this);

        ArrayList<ImageInfo> images = getIntent().getParcelableArrayListExtra(ARG_IMAGE_URL_LIST);
        String title = getIntent().getStringExtra(ARG_TITLE);
        User author = getIntent().getParcelableExtra(ARG_AUTHOR);

        if (images == null) {
            throw new IllegalStateException("ARG_IMAGE_URL_LIST not defined");
        }

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setTitle(title == null ? "" : Html.fromHtml(title));
            ab.setDisplayHomeAsUpEnabled(true);
        }

        setupAuthor(author);

        mAdapter = new PhotoAdapter(getFragmentManager(), images);
        mViewPager = (ViewPager)findViewById(R.id.PhotoViewPager);
        mViewPager.setAdapter(mAdapter);
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


    @Override
    public void onBitmapLoaded() {
        runHideActionBarTimer();
    }

    @Override
    public void onPhotoTap() {
        userForcedToChangeOverlayMode = true;
        toggleShowOrHideHideyBarMode();
    }

    @Override
    public void onLoadBitmapFailed() {
        Toast.makeText(ShowPhotoActivity.this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
        if (mAdapter != null && mAdapter.getCount() <= 1) finish();
    }

    public static class PhotoAdapter extends FragmentStatePagerAdapter {

        private final List<ImageInfo> mImages;

        public PhotoAdapter(FragmentManager fm, List<ImageInfo> images) {
            super(fm);
            mImages = images;
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public Fragment getItem(int position) {
            String url = NetworkUtils.createThumborUrlFromPath(mImages.get(position).image.path).toUrl();
            return ShowPhotoFragment.newInstance(url);
        }
    }

}
