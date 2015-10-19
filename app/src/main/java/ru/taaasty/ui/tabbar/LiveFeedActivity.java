package ru.taaasty.ui.tabbar;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.facebook.FacebookSdk;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.Locale;

import de.greenrobot.event.EventBus;
import io.intercom.android.sdk.Intercom;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.adapters.FragmentStatePagerAdapterBase;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Stats;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiApp;
import ru.taaasty.ui.feeds.FlowListFragment;
import ru.taaasty.ui.feeds.IFeedsFragment;
import ru.taaasty.ui.feeds.ListFeedFragment;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class LiveFeedActivity extends TabbarActivityBase implements ListFeedFragment.OnFragmentInteractionListener,
        FlowListFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedActivity";

    private static final String BUNDLE_KEY_FEED_STATS = "feed_stats";

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    CirclePageIndicator mCircleIndicator;
    ParallaxedView mCircleIndicatorParallaxedView;

    private Subscription mCurrentUserSubscription = Subscriptions.unsubscribed();

    private Subscription mStatsSubscription = Subscriptions.unsubscribed();

    @Nullable
    private Stats mStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_feed);

        getWindow().getDecorView().setBackgroundDrawable(null);

        FacebookSdk.sdkInitialize(this.getApplicationContext());
        ((TaaastyApplication)getApplicationContext()).startIntercomSession();

        Intercom.client().openGCMMessage(getIntent());

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        if (savedInstanceState != null) {
            mStats = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_STATS);
        }

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(5);

        Tracker t =  ((TaaastyApplication) getApplication()).getTracker();
        t.setScreenName(mSectionsPagerAdapter.getScreenName(mViewPager.getCurrentItem()));
        t.send(new HitBuilders.AppViewBuilder().build());

        mCircleIndicator = (CirclePageIndicator)findViewById(R.id.circle_page_indicator);
        mCircleIndicator.setViewPager(mViewPager);
        mCircleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mSectionsPagerAdapter == null) return;
                Tracker t = ((TaaastyApplication)getApplication()).getTracker();
                t.setScreenName(mSectionsPagerAdapter.getScreenName(position));
                t.send(new HitBuilders.AppViewBuilder().build());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mCircleIndicatorParallaxedView = new ParallaxedView(mCircleIndicator) {
            @Override
            protected void translatePreICS(View view, float offset) { throw new IllegalStateException("Not implemented"); }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTabbar.showFab(false);
        startRefreshStats();
        startRefreshCurrentUser();
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
                ((TaaastyApplication)getApplication()).sendAnalyticsEvent(
                        Constants.ANALYTICS_CATEGORY_FEEDS, "Обновление из сист. меню", null);
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
        mCurrentUserSubscription.unsubscribe();
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
            ApiApp api = RestClient.getAPiApp();
            mStatsSubscription = AppObservable.bindActivity(this, api.getStats())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mStatsObserver);
        }
    }

    @Nullable
    @Override
    public Stats getStats() {
        return mStats;
    }

    @Override
    public void startRefreshCurrentUser() {
        // Выводим пока сюда, чтобы если уже загружаем - не загружать снова
        if (!mCurrentUserSubscription.isUnsubscribed()) return;
        if (DBG) Log.v(TAG, "startRefreshCurrentUser()");
        Observable<CurrentUser> observableCurrentUser = AppObservable.bindActivity(this,
                Session.getInstance().getCurrentUser());

        mCurrentUserSubscription = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CurrentUser>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(CurrentUser currentUser) {
                    }
                });
    }

    /**
     * Вызывается при смене овидимого фрагмента в viewpager
     * @param newFragment
     */
    void onPrimaryItemChanged(IFeedsFragment newFragment) {
        if (DBG) Log.v(TAG, "onPrimaryItemChanged()" + newFragment);
        updateCircleIndicatorPosition(newFragment.isHeaderVisible(), newFragment.getHeaderTop());
    }

    void refreshData() {
        if (mSectionsPagerAdapter == null || mViewPager == null) return;
        Fragment page = mSectionsPagerAdapter.getRegisteredFragment(mViewPager.getCurrentItem());
        if (page != null && page instanceof ListFeedFragment) {
            ListFeedFragment listFragment = (ListFeedFragment) page;
            listFragment.refreshData(true);
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
        TlogActivity.startTlogActivity(this, user.getId(), view, R.dimen.avatar_extra_small_diameter_34dp);
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        SharePostActivity.startActivity(this, entry);
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

        private IFeedsFragment mPrimaryItem;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return ListFeedFragment.createMySubscriptionsFeedInstance();
                case 1:
                    return FlowListFragment.createInstance();
                case 2:
                    return ListFeedFragment.createLiveFeedInstance();
                case 3:
                    return ListFeedFragment.createBestFeedInstance();
                case 4:
                    return ListFeedFragment.createAnonymousFeedInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            if (mPrimaryItem != object) {
                mPrimaryItem = (IFeedsFragment)object;
                onPrimaryItemChanged((IFeedsFragment)object);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_my_subscriptions).toUpperCase(l);
                case 1:
                    return getString(R.string.title_flows).toUpperCase(l);
                case 2:
                    return getString(R.string.title_live_feed).toUpperCase(l);
                case 3:
                    return getString(R.string.title_best_feed).toUpperCase(l);
                case 4:
                    return getString(R.string.title_anonymous_feed).toUpperCase(l);
            }
            return null;
        }

        public String getScreenName(int position) {
            switch (position) {
                case 0:
                    return "Мои подписки";
                case 1:
                    return "Потоки";
                case 2:
                    return "Прямой эфир";
                case 3:
                    return "Лучшее";
                case 4:
                    return "Новости";
                case 5:
                    return "Анонимки";
                default:
                    throw new IllegalStateException();
            }
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
            notifyError(UiUtils.getUserErrorText(getResources(), e, R.string.server_error), e);
        }

        @Override
        public void onNext(Stats st) {
            mStats = st;
        }
    };
}
