package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Locale;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.feeds.GridFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class LiveFeedActivity extends Activity implements GridFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedActivity";

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    CirclePageIndicator mCircleIndicator;
    ParallaxedView mCircleIndicatorParallaxedView;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX
        if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

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
    public void onFeedButtonClicked(Uri uri) {

    }

    @Override
    public void onGridTopViewScroll(Fragment fragment, boolean firstItemVisible, int firstItemTop) {
        if (mSectionsPagerAdapter.getCurrentPrimaryItem() != fragment) return;
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
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    void switchToMyFeed() {
        Intent i = new Intent(LiveFeedActivity.this, MyFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private Tabbar.onTabbarButtonListener mTabbarListener = new Tabbar.onTabbarButtonListener() {
        @Override
        public void onTabbarButtonClicked(View v) {
            switch (v.getId()) {
                case R.id.btn_tabbar_live:
                    Fragment page = (Fragment)mSectionsPagerAdapter.getCurrentPrimaryItem();
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

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private Fragment mCurrentPrimaryItem;

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
                    return GridFeedFragment.createNewsFeedInstance();
                case 3:
                    return GridFeedFragment.createAnonymousFeedInstance();
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
            if (mCurrentPrimaryItem != object) {
                mCurrentPrimaryItem = (Fragment) object;
                onPrimaryItemChanged(mCurrentPrimaryItem);
            }
        }

        public Fragment getCurrentPrimaryItem() {
            return mCurrentPrimaryItem;
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
