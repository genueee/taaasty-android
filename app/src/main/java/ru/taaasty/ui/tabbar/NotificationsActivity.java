package ru.taaasty.ui.tabbar;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.ui.messages.NotificationListFragment;
import ru.taaasty.utils.MessageHelper;

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

        if (savedInstanceState == null) {
            long markArReadIds[] = null;
            if (getIntent().hasExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ)) {
                markArReadIds = getIntent().getLongArrayExtra(ARK_KEY_MARK_NOTIFICATIONS_AS_READ);
            }
            Fragment fragment = NotificationListFragment.newInstance(markArReadIds);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        }

        // Используем background у фрагмента. Там стоит тот же background, что и у activity - так и должно быть,
        // иначе на nexus 5 в landscape справа граница неправильная из-за того, что там правее
        // системные кнопки и background на activity лежит под ними.
        getWindow().setBackgroundDrawable(null);
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
    public void notifyError(Fragment fragment, @Nullable Throwable exception, int fallbackResId) {
        MessageHelper.showError(this, R.id.main_container,  REQUEST_CODE_LOGIN, exception, fallbackResId);
    }
}
