package ru.taaasty.ui;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.feeds.TlogActivity;
import ru.taaasty.ui.relationships.FollowingFollowersActivity;
import ru.taaasty.widgets.ErrorTextView;

public class UserInfoActivity extends ActivityBase implements UserInfoFragment.OnFragmentInteractionListener {
    private static final String TAG = "UserInfoActivity";
    private static final boolean DBG = BuildConfig.DEBUG;

    public static final String ARG_USER = "ru.taaasty.ui.UserInfoActivity.author";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.UserInfoActivity.tlog_design";

    private User mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mUser = getIntent().getParcelableExtra(ARG_USER);
        if (mUser == null) throw new IllegalArgumentException("no User");

        if (savedInstanceState == null) {
            TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);

            Fragment userInfoFragment = UserInfoFragment.newInstance(mUser);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        }
    }

    @Override
    public void onEntriesCountClicked() {
        if (DBG) Log.v(TAG, "onEntriesCountClicked");
        Intent i = new Intent(this, TlogActivity.class);
        i.putExtra(TlogActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSubscribtionsCountClicked() {
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSubscribersCountClicked() {
        Intent i = new Intent(this, FollowingFollowersActivity.class);
        i.putExtra(FollowingFollowersActivity.ARG_USER_ID, mUser.getId());
        startActivity(i);
    }

    @Override
    public void onSelectBackgroundClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_LONG).show();
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
}
