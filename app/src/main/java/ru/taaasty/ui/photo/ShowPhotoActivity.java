package ru.taaasty.ui.photo;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import ru.taaasty.ActivityBase;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.model.User;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.widgets.PhotoScrollPositionIndicator;

public class ShowPhotoActivity extends ActivityBase implements ShowPhotoFragment.OnFragmentInteractionListener {
    public static final String ARG_IMAGE_URL_LIST = "ru.taaasty.ui.photo.ShowPhotoActivity.image_url_list";
    public static final String ARG_TITLE = "ru.taaasty.ui.photo.ShowPhotoActivity.title";
    public static final String ARG_AUTHOR = "ru.taaasty.ui.photo.ShowPhotoActivity.author";
    public static final String ARG_PREVIEW_URL = "ru.taaasty.ui.photo.ShowPhotoActivity.ARG_PREVIEW_URL";

    private static final int HIDE_ACTION_BAR_DELAY = 5000;

    private boolean isNavigationHidden = false;
    private final Handler mHideActionBarHandler = new Handler();
    private volatile boolean userForcedToChangeOverlayMode = false;

    private PhotoAdapter mAdapter;
    private PhotoScrollPositionIndicator mIndicator;
    private boolean mIndicatorVisible = false;

    public static void startShowPhotoActivity(Context context,
                                              User author,
                                              String title,
                                              List<String> images,
                                              String previewUrl,
                                              View animateFrom) {
        ArrayList<String> imagesList;
        Intent intent = new Intent(context, ShowPhotoActivity.class);

        if (images instanceof ArrayList) {
            imagesList = (ArrayList<String>) images;
        } else {
            imagesList = new ArrayList<>(images);
        }
        intent.putStringArrayListExtra(ShowPhotoActivity.ARG_IMAGE_URL_LIST, imagesList);
        intent.putExtra(ShowPhotoActivity.ARG_TITLE, title);
        intent.putExtra(ShowPhotoActivity.ARG_AUTHOR, author);
        if (previewUrl != null) intent.putExtra(ShowPhotoActivity.ARG_PREVIEW_URL, previewUrl);

        if (animateFrom != null) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            if (context instanceof Activity) {
                ActivityCompat.startActivity((Activity) context, intent, options.toBundle());
            } else {
                context.startActivity(intent);
            }
        } else {
            context.startActivity(intent);
        }
    }

    public static void startShowPhotoActivity(Context context, Entry entry, String previewUrl, View animateFrom) {
        final ArrayList<String> images = entry.getImageUrls(false);
        startShowPhotoActivity(context, entry.getAuthor(), entry.getTitle(), images,  previewUrl, animateFrom);
    }

    public static boolean canShowEntry(Entry entry) {
        return !entry.getImageUrls(false).isEmpty();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);

        ArrayList<String> images = getIntent().getStringArrayListExtra(ARG_IMAGE_URL_LIST);
        String title = getIntent().getStringExtra(ARG_TITLE);
        User author = getIntent().getParcelableExtra(ARG_AUTHOR);
        String previewUrl = getIntent().getStringExtra(ARG_PREVIEW_URL);

        if (images == null) {
            throw new IllegalStateException("ARG_IMAGE_URL_LIST not defined");
        }

        mIndicator = (PhotoScrollPositionIndicator)findViewById(R.id.photo_position_indicator);
        mAdapter = new PhotoAdapter(getFragmentManager(), images, previewUrl);
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
    @SuppressLint("InlinedApi")
    public void toggleShowOrHideHideyBarMode() {

        int newUiOptions;

        if (Build.VERSION.SDK_INT >= 19) {
            newUiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        } else {
            newUiOptions = getWindow().getDecorView().getSystemUiVisibility();
        }

        if (!isNavigationHidden) {
            if (Build.VERSION.SDK_INT >= 19) {
                newUiOptions
                        |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE;
            } else {
                newUiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE;
            }
            getActionBar().hide();
            if (mIndicatorVisible) mIndicator.hide();
            isNavigationHidden = true;
        } else {
            if (Build.VERSION.SDK_INT < 19) {
                newUiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            }
            getActionBar().show();
            isNavigationHidden = false;
            userForcedToChangeOverlayMode = false;
            if (mIndicatorVisible) mIndicator.show();
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

    @Override
    public void onMatrixChanged(int photoViewWidth, RectF imageRect) {
        if (photoViewWidth >= imageRect.width()) {
            if (mIndicatorVisible) {
                mIndicatorVisible = false;
                mIndicator.hide();
            }
        } else {
            mIndicator.setScrollSizes(photoViewWidth, imageRect);
            if (!mIndicatorVisible) {
                mIndicatorVisible = true;
                if (!isNavigationHidden) mIndicator.show();
            }
        }
    }

    public static class PhotoAdapter extends FragmentStatePagerAdapter {

        private final List<String> mImages;
        private final String mPreview;

        public PhotoAdapter(FragmentManager fm, List<String> images, String preview) {
            super(fm);
            mImages = images;
            mPreview = preview;
        }

        @Override
        public int getCount() {
            return mImages.size();
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return ShowPhotoFragment.newInstance(mImages.get(position), mPreview);
            } else {
                return ShowPhotoFragment.newInstance(mImages.get(position), null);
            }
        }
    }
}
