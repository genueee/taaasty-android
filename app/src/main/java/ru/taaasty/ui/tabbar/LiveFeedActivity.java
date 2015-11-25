package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import com.fernandocejas.frodo.annotation.RxLogSubscriber;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nirhart.parallaxscroll.views.ParallaxedView;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.greenrobot.event.EventBus;
import io.intercom.android.sdk.Intercom;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.events.OnStatsLoaded;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.Stats;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.User;
import ru.taaasty.rest.service.ApiApp;
import ru.taaasty.ui.feeds.FeedFragment;
import ru.taaasty.ui.feeds.FlowListFragment;
import ru.taaasty.ui.feeds.IFeedsFragment;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.SharePostActivity;
import ru.taaasty.utils.MessageHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

public class LiveFeedActivity extends TabbarActivityBase implements FeedFragment.OnFragmentInteractionListener,
        FlowListFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LiveFeedActivity";

    private static final String BUNDLE_KEY_FEED_STATS = "feed_stats";

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    CirclePageIndicator mCircleIndicator;
    ParallaxedView mCircleIndicatorParallaxedView;

    View mLoginButton;

    private Subscription mCurrentUserSubscription = Subscriptions.unsubscribed();

    private Subscription mStatsSubscription = Subscriptions.unsubscribed();

    private Set<Fragment> mAttachedFragments = new HashSet<>(6);

    @Nullable
    private Stats mStats;

    private boolean mOnCreateTaskDone = false;

    private enum SectionType {

        MY_SUBSCRIPTIONS(R.string.title_my_subscriptions, "Мои подписки"),

        FLOWS(R.string.title_flows, "Потоки"),

        LIVE_FEED(R.string.title_live_feed, "Прямой эфир"),

        BEST_FEED(R.string.title_best_feed, "Лучшее"),

        ANONYMOUS_FEED(R.string.title_anonymous_feed, "Анонимки")

        ;

        public final String analyticsName;

        public final int titleResId;

        SectionType(int titleResId, String analyticsName) {
            this.titleResId = titleResId;
            this.analyticsName = analyticsName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_feed);

        mLoginButton = findViewById(R.id.login_button);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mCircleIndicator = (CirclePageIndicator)findViewById(R.id.circle_page_indicator);

        getWindow().getDecorView().setBackgroundDrawable(null);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        if (savedInstanceState != null) {
            mStats = savedInstanceState.getParcelable(BUNDLE_KEY_FEED_STATS);
        }

        // Set up the ViewPager with the sections adapter.

        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(6);

        Tracker t =  ((TaaastyApplication) getApplication()).getTracker();
        t.setScreenName(mSectionsPagerAdapter.getAnalyticsScreenName(mViewPager.getCurrentItem()));
        t.send(new HitBuilders.AppViewBuilder().build());

        mCircleIndicator.setViewPager(mViewPager);
        mCircleIndicator.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                if (mSectionsPagerAdapter == null) return;
                Tracker t = ((TaaastyApplication) getApplication()).getTracker();
                t.setScreenName(mSectionsPagerAdapter.getAnalyticsScreenName(position));
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

        mLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginActivity.startActivity(LiveFeedActivity.this, REQUEST_CODE_LOGIN, v);
            }
        });

        doOnCreateTask();
    }

    private void doOnCreateTask() {
        if (mOnCreateTaskDone) return;
        if (Session.getInstance().isAuthorized()) {
            ((TaaastyApplication) getApplicationContext()).startIntercomSession();
            Intercom.client().openGCMMessage(getIntent());
            mOnCreateTaskDone = true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SHARE) {
            SharePostActivity.handleActivityResult(this, findViewById(R.id.main_container), resultCode, data);
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        startRefreshStats();
        startRefreshCurrentUser(); // TODO не обновлять так часто?
        Session.getInstance().getUserObservable()
                .distinctUntilChanged()
                .compose(this.<CurrentUser>bindToLifecycle())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CurrentUserChangesSubscriber());
    }

    @RxLogSubscriber
    private final class CurrentUserChangesSubscriber extends Subscriber<CurrentUser> {

        @Override
        public void onCompleted() {
            if (!isUnsubscribed()) {
                unsubscribe();
            }
        }

        @Override
        public void onError(Throwable e) {}

        @Override
        public void onNext(CurrentUser currentUser) {
            mSectionsPagerAdapter.validateIsAuthorized();
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)mViewPager.getLayoutParams();
            if (Session.getInstance().isAuthorized()) {
                lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.tabbar_size);
                mViewPager.setLayoutParams(lp);
                mTabbar.showFab(false);
                mLoginButton.setVisibility(View.GONE);
                doOnCreateTask();
            } else {
                lp.bottomMargin = 0;
                mViewPager.setLayoutParams(lp);
                mLoginButton.setVisibility(View.VISIBLE);
            }
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
        if (!fragment.getUserVisibleHint()) return;
        updateCircleIndicatorPosition(headerVisible, viewTop);
    }

    @Override
    public void startRefreshStats() {
        if (mStatsSubscription.isUnsubscribed()) {
            if (DBG) Log.v(TAG, "startRefreshStats");
            ApiApp api = RestClient.getAPiApp();
            mStatsSubscription = api.getStats()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Stats>() {
                        @Override
                        public void onCompleted() {
                            EventBus.getDefault().post(new OnStatsLoaded(mStats));
                        }

                        @Override
                        public void onError(Throwable exception) {
                            MessageHelper.showError(LiveFeedActivity.this, R.id.main_container,  REQUEST_CODE_LOGIN, exception, R.string.server_error);
                        }

                        @Override
                        public void onNext(Stats st) {
                            mStats = st;
                        }
                    });
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
        if (!Session.getInstance().isAuthorized()) return;
        if (DBG) Log.v(TAG, "startRefreshCurrentUser()");
        Observable<CurrentUser> observableCurrentUser = Session.getInstance().reloadCurrentUser();
        mCurrentUserSubscription = observableCurrentUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CurrentUser>() {
                    @Override
                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {}

                    @Override
                    public void onNext(CurrentUser currentUser) {}
                });
    }

    @Override
    public void notifyError(Fragment fragment, @Nullable Throwable exception, int fallbackResId) {
        MessageHelper.showError(this, R.id.main_container,  REQUEST_CODE_LOGIN, exception, fallbackResId);
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
        Fragment page = getVisibleFragment();
        if (page != null && page instanceof FeedFragment) {
            FeedFragment listFragment = (FeedFragment) page;
            listFragment.refreshData(true);
            startRefreshStats();
        }
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
        SharePostActivity.startActivity(this, entry, REQUEST_CODE_SHARE);
    }

    @Nullable
    private Fragment getVisibleFragment() {
        Fragment result = null;
        for (Fragment fragment : mAttachedFragments) {
            if (fragment.getUserVisibleHint()) {
                result = fragment;
                break;
            }
        }

        if (DBG) Log.d(TAG, "getVisibleFragment() result: " + result);

        return result;
    }

    @Override
    public void onFragmentAttached(Fragment fragment) {
        mAttachedFragments.add(fragment);
    }

    @Override
    public void onFragmentDetached(Fragment fragment) {
        mAttachedFragments.remove(fragment);
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private IFeedsFragment mPrimaryItem;

        public boolean isAuthorized;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            isAuthorized = mSession.isAuthorized();
        }

        public void validateIsAuthorized() {
            if (isAuthorized != mSession.isAuthorized()) {
                isAuthorized = mSession.isAuthorized();
                notifyDataSetChanged();
            }
        }

        public SectionType getSectionType(int position) {
            if (isAuthorized) {
                switch (position) {
                    case 0: return SectionType.MY_SUBSCRIPTIONS;
                    case 1: return SectionType.FLOWS;
                    case 2: return SectionType.LIVE_FEED;
                    case 3: return SectionType.BEST_FEED;
                    case 4: return SectionType.ANONYMOUS_FEED;
                    default: throw new IllegalArgumentException();
                }
            } else {
                switch (position) {
                    case 0: return SectionType.FLOWS;
                    case 1: return SectionType.LIVE_FEED;
                    case 2: return SectionType.BEST_FEED;
                    case 3: return SectionType.ANONYMOUS_FEED;
                    default: throw new IllegalArgumentException();
                }
            }
        }

        @Override
        public Fragment getItem(int position) {
            switch (getSectionType(position)) {
                case MY_SUBSCRIPTIONS:
                    return FeedFragment.createMySubscriptionsFeedInstance();
                case FLOWS:
                    return FlowListFragment.createInstance();
                case LIVE_FEED:
                    return FeedFragment.createLiveFeedInstance();
                case BEST_FEED:
                    return FeedFragment.createBestFeedInstance();
                case ANONYMOUS_FEED:
                    return FeedFragment.createAnonymousFeedInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getItemPosition(Object object) {
            SectionType objectType = null;
            int itemPosition = POSITION_NONE;
            /*
            // Не работает этот вариант нихуя https://code.google.com/p/android/issues/detail?id=37990
            if (object instanceof FlowListFragment) {
                objectType = SectionType.FLOWS;
            }else if (object instanceof FeedFragment) {
                switch (((FeedFragment) object).getFeedType()) {
                    case FeedFragment.FEED_ANONYMOUS:
                        objectType = SectionType.ANONYMOUS_FEED;
                        break;
                    case FeedFragment.FEED_LIVE:
                        objectType = SectionType.LIVE_FEED;
                        break;
                    case FeedFragment.FEED_BEST:
                        objectType = SectionType.BEST_FEED;
                        break;
                    case FeedFragment.FEED_MY_SUBSCRIPTIONS:
                        objectType = SectionType.MY_SUBSCRIPTIONS;
                        break;
                    default:
                        break;
                }
            }
            if (objectType != null) {
                for (int i = getCount() - 1; i >= 0; --i) {
                    if (getSectionType(i) == objectType) {
                        itemPosition = i;
                        break;
                    }
                }
            }
            */
            if (DBG) Log.v(TAG, "getItemPosition() object: " + object + " new position: " + itemPosition);
            return itemPosition;
        }

        @Override
        public int getCount() {
            return isAuthorized ? 5 : 4;
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
            return getString(getSectionType(position).titleResId).toUpperCase(Locale.getDefault());
        }

        public String getAnalyticsScreenName(int position) {
            return getSectionType(position).analyticsName;
        }
    }
}
