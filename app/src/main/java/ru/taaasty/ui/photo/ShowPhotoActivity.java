package ru.taaasty.ui.photo;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.model.ImageInfo;
import ru.taaasty.model.User;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.utils.NetworkUtils;

public class ShowPhotoActivity extends ActivityBase implements ShowPhotoFragment.OnFragmentInteractionListener {
    public static final String ARG_IMAGE_URL_LIST = "ru.taaasty.ui.photo.ShowPhotoActivity.image_url_list";
    public static final String ARG_TITLE = "ru.taaasty.ui.photo.ShowPhotoActivity.title";
    public static final String ARG_AUTHOR = "ru.taaasty.ui.photo.ShowPhotoActivity.author";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private boolean isNavigationHidden = false;
    private final Handler mHideActionBarHandler = new Handler();
    private volatile boolean userForcedToChangeOverlayMode = false;

    private PhotoAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        ArrayList<ImageInfo> images = getIntent().getParcelableArrayListExtra(ARG_IMAGE_URL_LIST);
        String title = getIntent().getStringExtra(ARG_TITLE);
        User author = getIntent().getParcelableExtra(ARG_AUTHOR);

        if (images == null) {
            throw new IllegalStateException("ARG_IMAGE_URL_LIST not defined");
        }

        mAdapter = new PhotoAdapter(getFragmentManager(), images);
        ViewPager viewPager = (ViewPager) findViewById(R.id.PhotoViewPager);
        viewPager.setAdapter(mAdapter);

        if (getActionBar() != null) {
            ActionBar ab = getActionBar();
            ab.setTitle(title == null ?  "" : Html.fromHtml(title));
            ab.setDisplayHomeAsUpEnabled(true);

            Drawable dummyAvatar = getResources().getDrawable(R.drawable.ic_user_stub);
            ab.setIcon(dummyAvatar);

            ActionbarUserIconLoader abIconLoader = new ActionbarUserIconLoader(this, getActionBar()) {
                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    Toast.makeText(ShowPhotoActivity.this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
                }
            };
            abIconLoader.loadIcon(author.getUserpic(), author.getName());
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
