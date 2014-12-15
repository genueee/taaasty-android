package ru.taaasty.ui.tabbar;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Locale;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.adapters.FragmentStatePagerAdapterBase;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.model.Entry;
import ru.taaasty.model.Stats;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.service.ApiApp;
import ru.taaasty.ui.feeds.ListFeedFragment;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

public class LiveFeedActivity extends TabbarActivityBase implements ListFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedActivity";

    private static final String BUNDLE_KEY_FEED_STATS = "feed_stats";

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    CirclePageIndicator mCircleIndicator;
    ParallaxedView mCircleIndicatorParallaxedView;

    private ApiApp mApiStatsService;

    private Subscription mStatsSubscription = SubscriptionHelper.empty();

    @Nullable
    private Stats mStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_feed);

        mApiStatsService = NetworkUtils.getInstance().createRestAdapter().create(ApiApp.class);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        if (savedInstanceState != null) {
            mStats = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_STATS);
        }

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
    protected void onResume() {
        super.onResume();
        startRefreshStats();
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
            case R.id.menu_refresh:
                refreshData();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mStats != null) outState.putParcelable(BUNDLE_KEY_FEED_STATS, mStats);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mStatsSubscription.unsubscribe();
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_live;
    }


    @Override
    void onCurrentTabButtonClicked() {
        refreshData();
    }

    @Override
    public void onGridTopViewScroll(Fragment fragment, boolean headerVisible, int viewTop) {
        if (mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem()) != fragment) return;
        updateCircleIndicatorPosition(headerVisible, viewTop);
    }

    @Override
    public void startRefreshStats() {
        if (mStatsSubscription.isUnsubscribed()) {
            if (DBG) Log.v(TAG, "startRefreshStats");
            mStatsSubscription = AndroidObservable.bindActivity(this, mApiStatsService.getStats()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mStatsObserver);
        }
    }

    @Nullable
    @Override
    public Stats getStats() {
        return mStats;
    }

    /**
     * Вызывается при смене овидимого фрагмента в viewpager
     * @param newFragment
     */
    void onPrimaryItemChanged(Fragment newFragment) {
        if (DBG) Log.v(TAG, "onPrimaryItemChanged()" + newFragment);
        ListFeedFragment fragment = (ListFeedFragment)newFragment;
        updateCircleIndicatorPosition(fragment.isHeaderVisisble(), fragment.getHeaderTop());
    }

    void refreshData() {
        if (mSectionsPagerAdapter == null || mViewPager == null) return;
        Fragment page = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        if (page != null && page instanceof ListFeedFragment) {
            ListFeedFragment listFragment = (ListFeedFragment) page;
            listFragment.refreshData();
            startRefreshStats();
        }
    }

    void notifyStatsChanged() {
        EventBus.getDefault().post(new OnStatsLoaded(mStats));
    }

    void updateCircleIndicatorPosition(boolean isFirstItemVisible, int firstItemTop) {
        if (isFirstItemVisible) {
            mCircleIndicatorParallaxedView.setOffset(firstItemTop);
            mCircleIndicator.setVisibility(View.VISIBLE);
        } else {
            mCircleIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    public void onAvatarClicked(View view, User user, TlogDesign design) {
        TlogActivity.startTlogActivity(this, user.getId(), view);
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
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
                    return ListFeedFragment.createLiveFeedInstance();
                case 1:
                    return ListFeedFragment.createBestFeedInstance();
                case 2:
                    return ListFeedFragment.createAnonymousFeedInstance();
                case 3:
                    return ListFeedFragment.createNewsFeedInstance();
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

    private final Observer<Stats> mStatsObserver = new Observer<Stats>() {
        @Override
        public void onCompleted() {
            notifyStatsChanged();
        }

        @Override
        public void onError(Throwable e) {
            if (DBG) Log.e(TAG, "onError", e);
            notifyError(getString(R.string.server_error), e);
        }

        @Override
        public void onNext(Stats st) {
            mStats = st;
        }
    };


}
