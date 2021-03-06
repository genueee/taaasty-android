package ru.taaasty.ui.relationships;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.MessageHelper;

public class FollowingFollowersActivity extends ActivityBase implements
        FollowingsFragment.OnFragmentInteractionListener,
        RequestsFragment.OnFragmentInteractionListener  {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FollowingFollowersAct";

    private static final int REQUEST_CODE_LOGIN = 1;

    public static final String ARG_USER = "ru.taaasty.ui.relationships.FollowingFollowersActivity.user";

    public static final String ARG_KEY_SHOW_SECTION = "ru.taaasty.ui.relationships.FollowingFollowersActivity.ARG_KEY_SHOW_SECTION";

    /**
     * Подписки
     */
    public static final int SECTION_FOLLOWINGS = -1;
    /**
     * Подписчики
     */
    public static final int SECTION_FOLLOWERS = -2;

    /**
     * Заявки
     */
    public static final int SECTION_REQUESTS = -3;

    /**
     * Друзья
     */
    public static final int SECTION_FRIENDS = -4;


    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;

    private CurrentUser mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following_followers);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

        mUser = getIntent().getParcelableExtra(ARG_USER);
        if (mUser == null) throw new IllegalArgumentException("no user");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        tabLayout.setupWithViewPager(mViewPager);

        boolean showRequests = mUser.isPrivacy();

        int initialSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, SECTION_FOLLOWERS);
        if (initialSection == SECTION_REQUESTS && !showRequests) initialSection = SECTION_FOLLOWERS;
        mViewPager.setCurrentItem(section2ViewPagerPosition(initialSection, showRequests), false);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    public void onRelationshipClicked(View view, Relationship relationship) {
        long userId;
        switch (viewPagerPosition2Section(mViewPager.getCurrentItem(), mUser.isPrivacy())) {
            case SECTION_FOLLOWERS:
            case SECTION_REQUESTS:
                userId = relationship.getReaderId();
                break;
            case SECTION_FOLLOWINGS:
            case SECTION_FRIENDS:
                userId = relationship.getUserId();
                break;
            default:
                throw new IllegalStateException();
        }
        TlogActivity.startTlogActivity(this, userId, view);
    }

    public static int viewPagerPosition2Section(int position, boolean showRequests) {
        if (!showRequests) {
            switch (position) {
                case 0:
                    return SECTION_FOLLOWINGS;
                case 1:
                    return SECTION_FOLLOWERS;
                case 2:
                    return SECTION_FRIENDS;
            }
        } else {
            switch (position) {
                case 0:
                    return SECTION_FOLLOWINGS;
                case 1:
                    return SECTION_FOLLOWERS;
                case 2:
                    return SECTION_REQUESTS;
                case 3:
                    return SECTION_FRIENDS;
            }
        }
        throw new IllegalStateException();
    }

    public static int section2ViewPagerPosition(int section, boolean showRequests) {
        switch (section) {
            case SECTION_FOLLOWINGS: return 0;
            case SECTION_FOLLOWERS: return 1;
            case SECTION_REQUESTS: return 2;
            case SECTION_FRIENDS: return showRequests ? 3 : 2;
            default: throw new IllegalStateException();
        }
    }

    @Override
    public void notifyError(Fragment fragment, @Nullable Throwable exception, int fallbackResId) {
        MessageHelper.showError(this, R.id.activityRoot,  REQUEST_CODE_LOGIN, exception, fallbackResId);
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final boolean mShowRequests;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            mShowRequests = mUser.isPrivacy();
        }

        @Override
        public Fragment getItem(int position) {
            switch (viewPagerPosition2Section(position, mShowRequests)) {
                case SECTION_FOLLOWERS:
                    return FollowersFragment.newInstance(mUser.getId());
                case SECTION_FOLLOWINGS:
                    return FollowingsFragment.newInstance(mUser.getId());
                case SECTION_REQUESTS:
                    return RequestsFragment.newInstance();
                case SECTION_FRIENDS:
                    return FriendsFragment.newInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return mShowRequests ? 4 : 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (viewPagerPosition2Section(position, mShowRequests)) {
                case SECTION_FOLLOWERS:
                    return getString(R.string.title_followers);
                case SECTION_FOLLOWINGS:
                    return getString(R.string.title_followings);
                case SECTION_REQUESTS:
                    return getString(R.string.title_requests);
                case SECTION_FRIENDS:
                    return getString(R.string.title_friends);
            }
            return null;
        }
    }
}
