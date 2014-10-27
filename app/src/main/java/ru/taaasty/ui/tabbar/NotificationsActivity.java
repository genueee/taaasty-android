package ru.taaasty.ui.tabbar;

import android.os.Bundle;

import ru.taaasty.BuildConfig;
import ru.taaasty.PusherService;
import ru.taaasty.R;
import ru.taaasty.ui.NotificationsFragment;

public class NotificationsActivity extends TabbarActivityBase implements NotificationsFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        if (savedInstanceState == null) {
            NotificationsFragment fragment;
            fragment = NotificationsFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        PusherService.disableStatusBarNotifications(this);
        PusherService.refreshNotifications(this);
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
        // XXX
    }

}
