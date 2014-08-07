package ru.taaasty.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import ru.taaasty.R;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;

public class UserInfoActivity extends Activity implements UserInfoFragment.OnFragmentInteractionListener {

    public static final String ARG_USER = "ru.taaasty.ui.UserInfoActivity.user";
    public static final String ARG_TLOG_DESIGN = "ru.taaasty.ui.UserInfoActivity.tlog_design";

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

        if (savedInstanceState == null) {
            User user = getIntent().getParcelableExtra(ARG_USER);
            if (user == null) throw new IllegalArgumentException("no User");
            TlogDesign design = getIntent().getParcelableExtra(ARG_TLOG_DESIGN);

            Fragment userInfoFragment = UserInfoFragment.newInstance(user, design);
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, userInfoFragment)
                    .commit();
        }
    }

    @Override
    public void onEntriesCountClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubscribtionsCountClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSubscribersCountClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDaysCountClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }
}
