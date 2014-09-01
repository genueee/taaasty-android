package ru.taaasty.ui.feeds;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.User;
import ru.taaasty.ui.UserInfoActivity;
import ru.taaasty.widgets.ErrorTextView;

/**
 * Избранное и скрытые записи
 */
public class MyAdditionalFeedActivity extends ActivityBase implements MyAdditionalFeedFragment.OnFragmentInteractionListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "MyAdditionalFeedActivity";

    public static final String ARG_KEY_FEED_TYPE = "ru.taaasty.ui.feeds.feed_type";

    @Retention(RetentionPolicy.CLASS)
    @IntDef({FEED_TYPE_MAIN, FEED_TYPE_FRIENDS, FEED_TYPE_FAVORITES, FEED_TYPE_PRIVATE})
    public @interface FeedType {}
    public static final int FEED_TYPE_MAIN = 0;
    public static final int FEED_TYPE_FRIENDS = 1;
    public static final int FEED_TYPE_FAVORITES = 2;
    public static final int FEED_TYPE_PRIVATE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_additional_feed);

        if (savedInstanceState == null) {
            @FeedType int feedType;

            MyAdditionalFeedFragment feedFragment;
            //noinspection ResourceType
            feedType = getIntent().getIntExtra(ARG_KEY_FEED_TYPE, FEED_TYPE_MAIN);
            feedFragment = MyAdditionalFeedFragment.newInstance(feedType, 0, 0);
            getFragmentManager().beginTransaction()
                    .add(R.id.container, feedFragment)
                    .commit();
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

    @Override
    public void onAvatarClicked(User user, TlogDesign design) {
        if (user == null) return;
        Intent i = new Intent(this, UserInfoActivity.class);
        i.putExtra(UserInfoActivity.ARG_USER, user);
        i.putExtra(UserInfoActivity.ARG_TLOG_DESIGN, design);
        startActivity(i);
    }
}