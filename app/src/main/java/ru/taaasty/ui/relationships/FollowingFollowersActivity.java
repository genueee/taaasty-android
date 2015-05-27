package ru.taaasty.ui.relationships;

import android.app.Fragment;
import android.app.FragmentManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.Locale;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.model.Relationship;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.widgets.ErrorTextView;

public class FollowingFollowersActivity extends ActivityBase implements
        FollowingsFragment.OnFragmentInteractionListener,
        RequestsFragment.OnFragmentInteractionListener  {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "FollowingFollowersAct";
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

        mUser = getIntent().getParcelableExtra(ARG_USER);
        if (mUser == null) throw new IllegalArgumentException("no user");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);

        boolean showRequests = mUser.isPrivacy();

        int initialSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, SECTION_FOLLOWERS);

        PagerIndicator indicator = new PagerIndicator((ViewGroup)findViewById(R.id.following_followers_indicator), mViewPager, showRequests);
        if (initialSection == SECTION_REQUESTS && !showRequests) initialSection = SECTION_FOLLOWERS;
        indicator.setSection(initialSection);

        ActionbarUserIconLoader abIconLoader = new ActionbarUserIconLoader(this, getActionBar()) {
            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                notifyError(getText(R.string.error_loading_image), null);
            }
        };

        abIconLoader.loadIcon(mUser.getUserpic(), mUser.getName());

    }

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
            Locale l = Locale.getDefault();
            switch (viewPagerPosition2Section(position, mShowRequests)) {
                case SECTION_FOLLOWERS:
                    return getString(R.string.title_followers).toUpperCase(l);
                case SECTION_FOLLOWINGS:
                    return getString(R.string.title_followings).toUpperCase(l);
                case SECTION_REQUESTS:
                    return getString(R.string.title_requests).toUpperCase(l);
                case SECTION_FRIENDS:
                    return getString(R.string.title_friends).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * Индикатор сверху: "Подписки - подписчики - друзья"
     */
    private static class PagerIndicator implements ViewPager.OnPageChangeListener, View.OnClickListener {
        private final ViewGroup mRoot;
        private final ViewPager mPager;

        private final boolean mShowRequests;

        public PagerIndicator(ViewGroup root, ViewPager pager, boolean showRequests) {
            mRoot = root;
            mPager = pager;
            mShowRequests = showRequests;
            mPager.setOnPageChangeListener(this);
            int count = mRoot.getChildCount();
            for (int i = 0; i < count; ++i) mRoot.getChildAt(i).setOnClickListener(this);
            mRoot.findViewById(R.id.your_requests_indicator).setVisibility(mShowRequests ? View.VISIBLE : View.GONE);
        }

        public void setSection(int section) {
            mPager.setCurrentItem(section2ViewPagerPosition(section, mShowRequests), false);
            setActivatedView(section);
        }

        private static int section2ViewId(int section) {
            int viewId;
            switch (section) {
                case SECTION_FOLLOWERS: viewId = R.id.your_followers_indicator; break;
                case SECTION_FOLLOWINGS: viewId = R.id.you_follow_indicator; break;
                case SECTION_REQUESTS: viewId = R.id.your_requests_indicator; break;
                case SECTION_FRIENDS: viewId = R.id.your_friends_indicator; break;
                default: throw new IllegalArgumentException();
            }
            return viewId;
        }

        private static int viewId2Section(int viewId) {
            int section;
            switch (viewId) {
                case R.id.your_followers_indicator: section = SECTION_FOLLOWERS; break;
                case R.id.you_follow_indicator: section = SECTION_FOLLOWINGS; break;
                case R.id.your_requests_indicator: section = SECTION_REQUESTS; break;
                case R.id.your_friends_indicator: section = SECTION_FRIENDS; break;
                default: throw new IllegalStateException();
            }
            return section;
        }

        private void setActivatedView(int section) {
            int viewId = section2ViewId(section);
            int count = mRoot.getChildCount();
            for (int i = 0; i < count; ++i) {
                View child = mRoot.getChildAt(i);
                child.setActivated(child.getId() == viewId);
            }
        }

        @Override
        public void onPageScrolled(int i, float v, int i2) {

        }

        @Override
        public void onPageSelected(int position) {
            setActivatedView(viewPagerPosition2Section(position, mShowRequests));
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }

        @Override
        public void onClick(View v) {
            mPager.setCurrentItem(section2ViewPagerPosition(viewId2Section(v.getId()), mShowRequests), true);
        }
    }

}
