package ru.taaasty.ui.post;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import ru.taaasty.FragmentActivityBase;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.ui.CustomErrorView;

public class PostActionActivity extends FragmentActivityBase implements CustomErrorView {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.PostActionActivity.entry";

    public static final String ACTION_DELETE = "ru.taaasty.ui.post.PostActionActivity.delete";
    public static final String ACTION_REPORT = "ru.taaasty.ui.post.PostActionActivity.report";

    private static final String FRAGMENT_TAG_DELETE_REPORT_POST = "FRAGMENT_TAG_DELETE_REPORT_POST";

    private Entry mEntry;
    private String mAction;

    private static final String TAG = "PostActionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_action);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        mAction = getIntent().getAction();

        if( ACTION_DELETE.equals(mAction) ) {
            // show delete post dialog
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_POST) != null) return;
            DialogFragment f = DeletePostFragment.newInstance(mEntry.getId());
            f.show(fm, FRAGMENT_TAG_DELETE_REPORT_POST);
        }
        else if( ACTION_REPORT.equals(mAction) ) {
            // show report post dialog
            FragmentManager fm = getSupportFragmentManager();
            if (fm.findFragmentByTag(FRAGMENT_TAG_DELETE_REPORT_POST) != null) return;
            DialogFragment f = ReportPostFragment.newInstance(mEntry.getId());
            f.show(fm, FRAGMENT_TAG_DELETE_REPORT_POST);
        }
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
    }
}
