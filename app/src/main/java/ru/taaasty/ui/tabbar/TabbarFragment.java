package ru.taaasty.ui.tabbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.events.MessagingStatusReceived;
import ru.taaasty.rest.model.MessagingStatus;
import ru.taaasty.utils.FabHelper;


public class TabbarFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TabbarFragment";

    private static final String BUNDLE_ARG_ACTIVATED_ELEMENT = "ru.taaasty.ui.tabbar.BUNDLE_ARG_ACTIVATED_ELEMENT";

    private static final String BUNDLE_ARG_NOTIFICATIONS_COUNT = "ru.taaasty.ui.tabbar.BUNDLE_ARG_NOTIFICATIONS_COUNT";
    private static final String BUNDLE_ARG_CONVERSATIONS_COUNT = "ru.taaasty.ui.tabbar.BUNDLE_ARG_CONVERSATIONS_COUNT";

    private static final int[] sItemIds = new int[] {
            R.id.btn_tabbar_live,
            R.id.btn_tabbar_conversations,
            R.id.btn_tabbar_post,
            R.id.btn_tabbar_notifications,
            R.id.btn_tabbar_my_feed
    };

    private TextView mNotificationsCountView;
    private TextView mConversationsCountView;

    private FabHelper mFabHelper;

    private int mCreatePostViewLocation[] = new int[2];

    private onTabbarButtonListener mListener;

    private int mActivatedElement;

    private int mUnreadNotificationsCount;
    private int mUnreadConversationsCount;

    PusherService mPusherService;
    boolean mBound = false;

    public TabbarFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        if (savedInstanceState == null) {
            mActivatedElement = View.NO_ID;
            mUnreadNotificationsCount = -1;
        } else {
            mActivatedElement = savedInstanceState.getInt(BUNDLE_ARG_ACTIVATED_ELEMENT);
            mUnreadNotificationsCount = savedInstanceState.getInt(BUNDLE_ARG_NOTIFICATIONS_COUNT);
            mUnreadConversationsCount = savedInstanceState.getInt(BUNDLE_ARG_CONVERSATIONS_COUNT);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (onTabbarButtonListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onTabbarButtonListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.tabbar, container, false);
        mNotificationsCountView = (TextView)view.findViewById(R.id.unread_notifications_count);
        mConversationsCountView = (TextView)view.findViewById(R.id.unread_conversations_count);
        mFabHelper = new FabHelper(view.findViewById(R.id.btn_tabbar_post), R.dimen.tabbar_size);

        for (int id: sItemIds) {
            View v = view.findViewById(id);
            v.setOnClickListener(mOnClickListener);
        }

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), PusherService.class);
        getActivity().bindService(intent, mPusherServiceConnection, Context.BIND_AUTO_CREATE);
        refreshActivated();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mBound) {
            getActivity().unbindService(mPusherServiceConnection);
            mBound = false;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_ARG_ACTIVATED_ELEMENT, mActivatedElement);
        outState.putInt(BUNDLE_ARG_NOTIFICATIONS_COUNT, mUnreadNotificationsCount);
        outState.putInt(BUNDLE_ARG_CONVERSATIONS_COUNT, mUnreadConversationsCount);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mNotificationsCountView = null;
        mConversationsCountView = null;
        mFabHelper = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public void onEventMainThread(MessagingStatusReceived event) {
        setMessagingStatus(event.data, true);
    }

    private void setMessagingStatus(@Nullable MessagingStatus status, boolean showSmoothly) {
        if (DBG) Log.v(TAG, "setMessagingStatus " + status);
        if (status == null) {
            boolean changed = (mUnreadNotificationsCount > 0)
                    || (mUnreadConversationsCount > 0);
            mUnreadNotificationsCount = 0;
            mUnreadConversationsCount = 0;
            if (changed) refreshNotificationIndicator(showSmoothly);
        } else {
            boolean changed = (mUnreadNotificationsCount != status.unreadNotificationsCount)
                    || (mUnreadConversationsCount != status.unreadConversationsCount);
            mUnreadNotificationsCount = status.unreadNotificationsCount;
            mUnreadConversationsCount = status.unreadConversationsCount;
            if (changed) refreshNotificationIndicator(showSmoothly);
        }
    }

    public void setActivated(@IdRes int activated) {
        mActivatedElement = activated;
        refreshActivated();
    }

    public int getActivatedViewId() {
        return mActivatedElement;
    }

    public void refreshActivated() {
        if (getView() == null) return;
        for (int id : sItemIds) {
            View v = getView().findViewById(id);
            v.setActivated(mActivatedElement == v.getId());
        }
    }

    public void showFab(boolean animate) {
        if (mFabHelper == null) return;
        mFabHelper.showFab(animate);
    }

    public void hideFab(boolean animate) {
        if (mFabHelper == null) return;
        mFabHelper.hideFab(animate);
    }

    public FabHelper getFab() {
        return mFabHelper;
    }

    private void refreshNotificationIndicator(boolean smoothly) {
        if (DBG) Log.v(TAG, "refreshNotificationIndicator");
        if (mNotificationsCountView == null) return;
        updateIndicatorCount(mNotificationsCountView, mUnreadNotificationsCount, smoothly);
        updateIndicatorCount(mConversationsCountView, mUnreadConversationsCount, smoothly);
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onTabbarButtonClicked(v);
        }
    };

    public interface onTabbarButtonListener {
        void onTabbarButtonClicked(View v);
    }

    private void updateIndicatorCount(TextView view, int count, boolean smoothly) {
        if (count <= 0) {
            if (view.getVisibility() == View.VISIBLE) {
                if (smoothly) hideIndicatorSmoothly(view); else view.setVisibility(View.INVISIBLE);
            }
        } else {
            String cntText;
            if (count >= 10000) {
                cntText = "1k+";
            } else if (count >= 1000) {
                int thousands = count / 1000;
                cntText = String.valueOf(thousands) + "k+";
            } else {
                cntText = String.valueOf(count);
            }
            view.setText(cntText);
            if (view.getVisibility() != View.VISIBLE) {
                if (smoothly) showIndicatorSmoothly(view); else view.setVisibility(View.VISIBLE);
            }
        }
    }

    private void showIndicatorSmoothly(final TextView view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
                .setDuration(getResources().getInteger(R.integer.longAnimTime));
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(1f);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setAlpha(1f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        animator.start();
    }

    private void hideIndicatorSmoothly(final TextView view) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
                .setDuration(getResources().getInteger(R.integer.longAnimTime));
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                view.setVisibility(View.VISIBLE);
                view.setAlpha(0f);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setAlpha(1f);
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                view.setAlpha(1f);
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {}
        });


        animator.start();
    }

    private final ServiceConnection mPusherServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PusherService.LocalBinder binder = (PusherService.LocalBinder) service;
            mPusherService = binder.getService();
            mBound = true;
            setMessagingStatus(mPusherService.getLastMessagingStatus(), false);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
            mPusherService = null;
        }
    };


}
