package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.model.Entry;
import ru.taaasty.ui.feeds.SubscriptionsFeedFragment;
import ru.taaasty.ui.post.SharePostActivity;

/**
 * Избранное и скрытые записи
 */
public class SubscriptionsActivity extends TabbarActivityBase implements SubscriptionsFeedFragment.OnFragmentInteractionListener {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SubscriptionsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_subscriptions);

        if (savedInstanceState == null) {
            SubscriptionsFeedFragment feedFragment;
            feedFragment = SubscriptionsFeedFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, feedFragment)
                    .commit();
        }
    }

    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_subscriptions;
    }

    @Override
    void onCurrentTabButtonClicked() {
        SubscriptionsFeedFragment fragment = (SubscriptionsFeedFragment)getFragmentManager().findFragmentById(R.id.container);
        fragment.refreshData();
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }
}