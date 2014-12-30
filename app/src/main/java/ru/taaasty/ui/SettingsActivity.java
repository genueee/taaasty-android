package ru.taaasty.ui;

import android.app.ActionBar;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.utils.ActionbarUserIconLoader;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.widgets.ErrorTextView;

/**
 * Created by alexey on 30.12.14.
 */
public class SettingsActivity extends ActivityBase implements SettingsFragment.OnFragmentInteractionListener {
    private static final String TAG = "SettingsActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    private ActionbarUserIconLoader mAbIconLoader; // Anti picasso weakref

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        requestWindowFeature(Window.FEATURE_PROGRESS);

        setContentView(R.layout.activity_settings);

        if (savedInstanceState == null) {
            Fragment userInfoFragment = SettingsFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        }

        if (getActionBar() != null) {
            ActionBar ab = getActionBar();
            ab.setDisplayHomeAsUpEnabled(true);

            Drawable dummyAvatar = getResources().getDrawable(R.drawable.ic_user_stub);
            ab.setIcon(dummyAvatar);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        ErrorTextView ert = (ErrorTextView) findViewById(R.id.error_text);
        if (exception != null) Log.e(TAG, error.toString(), exception);

        CharSequence text;
        if (exception != null && exception instanceof  NetworkUtils.ResponseErrorException) {
            text = ((NetworkUtils.ResponseErrorException)exception).error.longMessage;
            if (text == null) text = ((NetworkUtils.ResponseErrorException)exception).error.error;
        } else {
            text = error;
        }

        ert.setError(text);

    }

    @Override
    public void onErrorRefreshUser(Throwable e) {
        Toast.makeText(this, R.string.user_error, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onCurrentUserLoaded(CurrentUser user) {
        if (getActionBar() != null) {
            mAbIconLoader = new ActionbarUserIconLoader(this, getActionBar()) {
                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    notifyError(getString(R.string.error_loading_image), null);
                }
            };
            mAbIconLoader.loadIcon(user.getUserpic(), user.getName());
        }
    }
}
