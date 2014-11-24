package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.events.NotificationsCountStatus;


public class TabbarFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TabbarFragment";

    private static final String BUNDLE_ARG_ACTIVATED_ELEMENT = "ru.taaasty.ui.tabbar.BUNDLE_ARG_ACTIVATED_ELEMENT";

    private static final String BUNDLE_ARG_NOTIFICATIONS_COUNT = "ru.taaasty.ui.tabbar.BUNDLE_ARG_NOTIFICATIONS_COUNT";

    private static final int[] sItemIds = new int[] {
            R.id.btn_tabbar_subscriptions,
            R.id.btn_tabbar_live,
            R.id.btn_tabbar_post,
            R.id.btn_tabbar_notifications,
            R.id.btn_tabbar_my_feed
    };

    private View mNotificationsIndicator;

    private onTabbarButtonListener mListener;

    private int mActivatedElement;

    private int mUnreadNotificationsCount;

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
        }
        PusherService.requestCountStatus(getActivity());
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
        mNotificationsIndicator = view.findViewById(R.id.tabbar_notification_indicator);
        for (int id: sItemIds) {
            View v = view.findViewById(id);
            v.setOnClickListener(mOnClickListener);
        }

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        refreshActivated();
        refreshNotificationIndicator();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_ARG_ACTIVATED_ELEMENT, mActivatedElement);
        outState.putInt(BUNDLE_ARG_NOTIFICATIONS_COUNT, mUnreadNotificationsCount);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mNotificationsIndicator = null;
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

    public void onEventMainThread(NotificationsCountStatus event) {
        if (event.status.code == PusherService.UPDATE_NOTIFICATIONS_STATUS_READY) {
            mUnreadNotificationsCount = event.unreadCount;
        } else {
            mUnreadNotificationsCount = -1;
        }
        refreshNotificationIndicator();
    }

    public void setUnreadNotificationsCount(int count) {
        mUnreadNotificationsCount = count;
        refreshNotificationIndicator();
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

    private void refreshNotificationIndicator() {
        if (mNotificationsIndicator == null) return;
        if (mUnreadNotificationsCount <= 0) {
            mNotificationsIndicator.setVisibility(View.INVISIBLE);
        } else {
            mNotificationsIndicator.setVisibility(View.VISIBLE);
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener != null) mListener.onTabbarButtonClicked(v);
        }
    };


    public interface onTabbarButtonListener {
        public void onTabbarButtonClicked(View v);
    }


}
