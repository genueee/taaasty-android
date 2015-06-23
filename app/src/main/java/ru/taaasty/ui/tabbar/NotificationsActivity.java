package ru.taaasty.ui.tabbar;

import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import ru.taaasty.BuildConfig;
import ru.taaasty.IntentService;
import ru.taaasty.R;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.ui.messages.NotificationListFragment;

public class NotificationsActivity extends TabbarActivityBase implements
        NotificationListFragment.OnFragmentInteractionListener
{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NotificationsActivity";

    /**
     * При открытии, пометить уведомление как прочитанное
     */
    public static final String ARK_KEY_MARK_NOTIFICATIONS_AS_READ = "ru.taaasty.ui.tabbar.NotificationsActivity.ARK_KEY_MARK_NOTIFICATIONS_AS_READ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        getActionBar().setIcon(android.R.color.transparent);


        if (savedInstanceState == null) {
            Fragment fragment = NotificationListFragment.newInstance();
            getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
            if (getIntent().hasExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ)) {
                long ids[] = getIntent().getLongArrayExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ);
                if (ids.length != 0) {
                    Intent intent = IntentService.getMarkNotificationAsReadIntent(this, ids, false);
                    startService(intent);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        StatusBarNotifications.getInstance().disableStatusBarNotifications();
    }

    @Override
    protected void onStop() {
        super.onStop();
        StatusBarNotifications.getInstance().enableStatusBarNotifications();
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_notifications;
    }

    @Override
    void onCurrentTabButtonClicked() {
    }

    @Override
    public void onListScrolled(int scrollY, boolean atTop) {
        ActionBar ab = getActionBar();
        if (ab == null) return;
        if (!atTop) {
            ab.hide();
        } else {
            ab.show();
        }
    }

    @Override
    public void onListScrollStateChanged(int state) {
    }
}
