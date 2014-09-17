package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.ui.feeds.SubscribtionsFeedFragment;
import ru.taaasty.ui.login.LoginActivity;
import ru.taaasty.ui.post.CreatePostActivity;
import ru.taaasty.widgets.ErrorTextView;
import ru.taaasty.widgets.Tabbar;

/**
 * Избранное и скрытые записи
 */
public class SubscribtionsActivity extends ActivityBase implements SubscribtionsFeedFragment.OnFragmentInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SubscribtionsActivity";

    public static final int CREATE_POST_ACTIVITY_REQUEST_CODE = 4;

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CREATE_POST_ACTIVITY_REQUEST_CODE:
                switch (resultCode) {
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_MY_FEED:
                        switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG);
                        break;
                    case CreatePostActivity.CREATE_POST_ACTIVITY_RESULT_SWITCH_TO_HIDDEN:
                        switchToMyFeed(MyFeedActivity.SECTION_HIDDEN);
                        break;
                }
                break;
        }
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
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
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

    void switchToMyFeed() { switchToMyFeed(MyFeedActivity.SECTION_MY_TLOG); }

    void switchToMyFeed(int initialSection) {
        Intent i = new Intent(this, MyFeedActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra(MyFeedActivity.ARG_KEY_SHOW_SECTION, initialSection);
        startActivity(i);
        finish();
        overridePendingTransition(0, 0);
    }

    void openCreatePost() {
        Intent i = new Intent(this, CreatePostActivity.class);
        startActivityForResult(i, CREATE_POST_ACTIVITY_REQUEST_CODE);
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
                case R.id.btn_tabbar_post:
                    openCreatePost();
                    break;
                default:
                    Toast.makeText(SubscribtionsActivity.this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
}