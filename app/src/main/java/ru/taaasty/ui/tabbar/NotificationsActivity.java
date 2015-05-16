package ru.taaasty.ui.tabbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.adapters.FragmentStatePagerAdapterBase;
import ru.taaasty.events.ConversationChanged;
import ru.taaasty.model.Conversation;
import ru.taaasty.ui.messages.ConversationActivity;
import ru.taaasty.ui.messages.ConversationsListFragment;
import ru.taaasty.ui.messages.InitiateConversationFragment;
import ru.taaasty.ui.messages.NotificationListFragment;

public class NotificationsActivity extends TabbarActivityBase implements
        NotificationListFragment.OnFragmentInteractionListener,
        ConversationsListFragment.OnFragmentInteractionListener,
        InitiateConversationFragment.OnFragmentInteractionListener
{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationsActivity";

    public static final String ARG_KEY_SHOW_SECTION = "ru.taaasty.ui.tabbar.NotificationsActivity.ARG_KEY_SHOW_SECTION";

    public static final int SECTION_NOTIFICATIONS = 0;
    public static final int SECTION_CONVERSATIONS = 1;

    private static final String TAG_INITIATE_CONVERSATION_DIALOG = "TAG_INITIATE_CONVERSATION_DIALOG";

    ViewPager mViewPager;
    SectionsPagerAdapter mSectionsPagerAdapter;
    HeaderController mHeaderController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        mViewPager = (ViewPager) findViewById(R.id.pager);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);

        int initialSection = getIntent().getIntExtra(ARG_KEY_SHOW_SECTION, SECTION_NOTIFICATIONS);
        mViewPager.setCurrentItem(initialSection, false);

        PagerIndicator indicator = new PagerIndicator((ViewGroup)findViewById(R.id.notifications_conversations_title), mViewPager);
        indicator.setSection(initialSection);

        mHeaderController = new HeaderController(findViewById(R.id.notifications_conversations_container));
    }

    @Override
    protected void onStart() {
        super.onStart();
        PusherService.disableStatusBarNotifications(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        PusherService.enableStatusBarNotifications(this);
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_notifications;
    }

    @Override
    void onCurrentTabButtonClicked() {
    }

    @Override
    public void onConversationCreated(Conversation conversation) {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        EventBus.getDefault().post(new ConversationChanged(conversation));
        ConversationActivity.startConversationActivity(this, conversation, null);
    }

    @Override
    public void onInitiateConversationClicked(View view) {
        Fragment initiateConversationFragment = new InitiateConversationFragment();
        getFragmentManager()
                .beginTransaction()
                .addToBackStack(null)
                .replace(R.id.initiate_conversation_container, initiateConversationFragment, TAG_INITIATE_CONVERSATION_DIALOG)
                .commit();

    }

    @Override
    public void onListScrolled(int scrollY, boolean atTop) {
        mHeaderController.onListScrolled(scrollY, atTop);
    }

    @Override
    public void onListScrollStateChanged(int state) {
        mHeaderController.onScrollStateChanged(state);
    }

    public class SectionsPagerAdapter extends FragmentStatePagerAdapterBase {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case SECTION_NOTIFICATIONS:
                    return NotificationListFragment.newInstance();
                case SECTION_CONVERSATIONS:
                    return ConversationsListFragment.newInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case SECTION_NOTIFICATIONS:
                    return getString(R.string.title_notifications);
                case SECTION_CONVERSATIONS:
                    return getString(R.string.title_conversations);
            }
            return null;
        }
    }

    /**
     * Индикатор сверху: "Уведомления - сообщения"
     */
    private class PagerIndicator implements ViewPager.OnPageChangeListener, View.OnClickListener {
        private final ViewGroup mRoot;
        private final ViewPager mPager;

        public PagerIndicator(ViewGroup root, ViewPager pager) {
            mRoot = root;
            mPager = pager;
            mPager.setOnPageChangeListener(this);
            int count = mRoot.getChildCount();
            for (int i = 0; i < count; ++i) mRoot.getChildAt(i).setOnClickListener(this);
        }

        public void setSection(int section) {
            mPager.setCurrentItem(section, false);
            setActivatedView(section);
        }

        private int section2ViewId(int section) {
            int viewId;
            switch (section) {
                case SECTION_NOTIFICATIONS: viewId = R.id.notifications_title; break;
                case SECTION_CONVERSATIONS: viewId = R.id.conversations_title; break;
                default: throw new IllegalArgumentException();
            }
            return viewId;
        }

        private int viewId2Section(int viewId) {
            int section;
            switch (viewId) {
                case R.id.notifications_title: section = SECTION_NOTIFICATIONS; break;
                case R.id.conversations_title: section = SECTION_CONVERSATIONS; break;
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
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int i) {
            setActivatedView(i);
            mHeaderController.showView();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }

        @Override
        public void onClick(View v) {
            mPager.setCurrentItem(viewId2Section(v.getId()), true);
        }
    }

    public static class HeaderController {

        private final View mView;

        private int mMaxDy;

        private int mLastListScrolledDy;

        private ValueAnimator mShowViewAnimator;

        public HeaderController(View view) {
            mView = view;
            mView.setTranslationY(0);
            if (mView.getWidth() == 0) {
                if (mView.getViewTreeObserver().isAlive()) {
                    mView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            if (mView.getViewTreeObserver().isAlive()) mView.getViewTreeObserver().removeOnPreDrawListener(this);
                            initView();
                            return true;
                        }
                    });
                }
            } else {
                initView();
            }
        }

        public void onListScrolled(int dy, boolean atTop) {
            if (atTop) {
                if (mShowViewAnimator == null) showView();
                return;
            }

            mLastListScrolledDy = dy;
            float translationY = mView.getTranslationY();
            if (dy > 0) {
                // Скролл вниз по списку
                if (mShowViewAnimator != null) mShowViewAnimator.cancel();
                float newTranslation = translationY - dy;
                mView.setTranslationY(newTranslation);
            } else {
                if (mShowViewAnimator == null) {
                    float newTranslation = Math.min(0f, translationY - dy);
                    mView.setTranslationY(newTranslation);
                }
            }
        }

        public void onScrollStateChanged(int newState) {
            if (mLastListScrolledDy < 0 && (newState == RecyclerView.SCROLL_STATE_SETTLING)) {
                if (mView.getTranslationY() < -mMaxDy) mView.setTranslationY(-mMaxDy);
            }
        }

        private void initView() {
            int location[] = new int[2];
            mView.getLocationInWindow(location);
            mMaxDy = 2 * (location[1] + mView.getHeight());
        }

        public void showView() {
            if (mView.getTranslationY() != 0f) {
                mShowViewAnimator = ObjectAnimator.ofFloat(mView, "translationY", mView.getTranslationY(), 0f)
                        .setDuration(mView.getResources().getInteger(R.integer.longAnimTime));
                mShowViewAnimator.addListener(mAnimatorListener);
                mShowViewAnimator.start();
            }
        }

        private final Animator.AnimatorListener mAnimatorListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mShowViewAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mShowViewAnimator = null;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        };
    }
}
