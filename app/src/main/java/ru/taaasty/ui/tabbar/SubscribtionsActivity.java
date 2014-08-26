package ru.taaasty.ui.tabbar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.feeds.SubscribtionsFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Избранное и скрытые записи
 */
public class SubscribtionsActivity extends Activity implements SubscribtionsFeedFragment.OnFragmentInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SubscribtionsActivity";

    private UserManager mUserManager = UserManager.getInstance();
    private Tabbar mTabbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // XXX
        if (mUserManager.getCurrentUserToken() == null) {
            switchToLoginForm();
            return;
        }

        setContentView(R.layout.activity_subscribtions);

        mTabbar = (Tabbar) findViewById(R.id.tabbar);
        mTabbar.setOnTabbarButtonListener(mTabbarListener);

        mTabbar.setActivated(R.id.btn_tabbar_subscribtions);

        if (savedInstanceState == null) {
            SubscribtionsFeedFragment feedFragment;
            feedFragment = SubscribtionsFeedFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, feedFragment)
                    .commit();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
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

    private void switchToLoginForm() {
        Intent i = new Intent(this, LoginActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY
                        | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToLiveFeed() {
        Intent i = new Intent(this, LiveFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void switchToMyFeed() {
        Intent i = new Intent(this, MyFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    private Tabbar.onTabbarButtonListener mTabbarListener = new Tabbar.onTabbarButtonListener() {
        @Override
        public void onTabbarButtonClicked(View v) {
            switch (v.getId()) {
                case R.id.btn_tabbar_subscribtions:
                    SubscribtionsFeedFragment fragment = (SubscribtionsFeedFragment)getFragmentManager().findFragmentById(R.id.container);
                    fragment.refreshData();
                    break;
                case R.id.btn_tabbar_my_feed:
                    switchToMyFeed();
                    break;
                case R.id.btn_tabbar_live:
                    switchToLiveFeed();
                    break;
                default:
                    Toast.makeText(SubscribtionsActivity.this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}