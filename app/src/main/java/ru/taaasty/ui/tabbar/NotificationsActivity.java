package ru.taaasty.ui.tabbar;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import ru.taaasty.BuildConfig;
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

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        // Используем background у фрагмента. Там стоит тот же background, что и у activity - так и должно быть,
        // иначе на nexus 5 в landscape справа граница неправильная из-за того, что там правее
        // системные кнопки и background на activity лежит под ними.
        getWindow().setBackgroundDrawable(null);

        if (savedInstanceState == null) {
            long markArReadIds[] = null;
            if (getIntent().hasExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ)) {
                markArReadIds = getIntent().getLongArrayExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ);
            }
            Fragment fragment = NotificationListFragment.newInstance(markArReadIds);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
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
        ActionBar ab = getSupportActionBar();
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
