package ru.taaasty.ui.tabbar;

import android.os.Bundle;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.NotificationsFragment;
import ru.taaasty.widgets.Tabbar;

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
    int getCurrentTabId() {
        return R.id.btn_tabbar_notifications;
    }

    @Override
    void onCurrentTabButtonClicked() {
        // XXX
    }

}
