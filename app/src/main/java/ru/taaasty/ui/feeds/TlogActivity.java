package ru.taaasty.ui.feeds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.widgets.ErrorTextView;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class TlogActivity extends Activity implements MyFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyFeedActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_feed);
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

    @Override
    public void onShowAdditionalmenuClicked() {
        // XXX
    }

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        if (user == null) return;
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }
}