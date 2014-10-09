package ru.taaasty.ui.tabbar;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Locale;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.adapters.FragmentStatePagerAdapterBase;
import ru.taaasty.ui.feeds.GridFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;


public class LiveFeedActivity extends ActivityBase implements GridFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedActivity";

    private static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 4;

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    CirclePageIndicator mCircleIndicator;
    ParallaxedView mCircleIndicatorParallaxedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX
        if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        if (DBG) Log.v(TAG, "onCreate savedInstanceState: " + savedInstanceState);

        setContentView(R.layout.activity_live_feed);

        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        mTabbar.setActivated(R.id.btn_tabbar_live);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mCircleIndicator = (CirclePageIndicator)findViewById(R.id.circle_page_indicator);
        mCircleIndicator.setViewPager(mViewPager);
        mCircleIndicatorParallaxedView = new ParallaxedView(mCircleIndicator) {
            @Override
            protected void translatePreICS(View view, float offset) { throw new IllegalStateException("Not implemented"); }
        };

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREATE_POST_ACTIVITY_REQUEST_CODE:
                switch (resultCode) {
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                        switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG);
                        break;
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                        switchToMyFeed(MyFeedActivity.SECTION_HIDDEN);
                        break;
                }
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DBG) Log.v(TAG, "onNewIntent");
    }

    @Override
    public void onFeedButtonClicked(Uri uri) {

    }

    @Override
    public void onGridTopViewScroll(Fragment fragment, boolean firstItemVisible, int firstItemTop) {
        if (mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()) != fragment) return;
        updateCircleIndicatorPosition(firstItemVisible, firstItemTop);
    }

    /**
     * Вызывается при смене овидимого фрагмента в viewpager
     * @param newFragment
     */
    void onPrimaryItemChanged(Fragment newFragment) {
        if (DBG) Log.v(TAG, "onPrimaryItemChanged()" + newFragment);
        GridFeedFragment fragment = (GridFeedFragment)newFragment;
        updateCircleIndicatorPosition(fragment.isFirstChildVisisble(), fragment.getFirstChildTop());
    }

    void updateCircleIndicatorPosition(boolean isFirstItemVisible, int firstItemTop) {
        if (isFirstItemVisible) {
            mCircleIndicatorParallaxedView.setOffset(firstItemTop);
            mCircleIndicator.setVisibility(View.VISIBLE);
        } else {
            mCircleIndicator.setVisibility(View.GONE);
        }
    }


    private void switchToLoginForm() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToSubscribtions() {
        Intent i = new Intent(LiveFeedActivity.this, SubscribtionsActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToMyFeed() { switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG); }

    void switchToMyFeed(int initialSection) {
        Intent i = new Intent(LiveFeedActivity.this, MyFeedActivity.class);
        i.putExtra(MyFeedActivity.ARG_KEY_SHOW_SECTION, initialSection);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void openCreatePost() {
        Intent i = new Intent(this,     CreatePostActivity.class);
        startActivityForResult(i, CREATE_POST_ACTIVITY_REQUEST_CODE);
    }

    private Tabbar.onTabbarButtonListener mTabbarListener = new Tabbar.onTabbarButtonListener() {
        @Override
        public void onTabbarButtonClicked(View v) {
            switch (v.getId()) {
                case R.id.btn_tabbar_live:
                    Fragment page = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
                    if (page != null && page instanceof GridFeedFragment) {
                        GridFeedFragment gff = (GridFeedFragment) page;
                        gff.refreshData();
                    }
                    break;
                case R.id.btn_tabbar_my_feed:
                    switchToMyFeed();
                    break;
                case R.id.btn_tabbar_subscribtions:
                    switchToSubscribtions();
                    break;
                case R.id.btn_tabbar_post:
                    openCreatePost();
                    break;
                default:
                    Toast.makeText(LiveFeedActivity.this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

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

    public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

        private Fragment mPrimaryItem;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return GridFeedFragment.createLiveFeedInstance();
                case 1:
                    return GridFeedFragment.createBestFeedInstance();
                case 2:
                    return GridFeedFragment.createAnonymousFeedInstance();
                case 3:
                    return GridFeedFragment.createNewsFeedInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (mPrimaryItem != object) {
                mPrimaryItem = (Fragment)object;
                onPrimaryItemChanged((Fragment)object);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_live_feed).toUpperCase(l);
                case 1:
                    return getString(R.string.title_best_feed).toUpperCase(l);
                case 2:
                    return getString(R.string.title_news).toUpperCase(l);
                case 3:
                    return getString(R.string.title_anonymous_feed).toUpperCase(l);
            }
            return null;
        }
    }

}
