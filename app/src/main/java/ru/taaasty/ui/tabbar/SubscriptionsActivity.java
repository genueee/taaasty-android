package ru.taaasty.ui.tabbar;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.ui.feeds.IRereshable;
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

        getWindow().getDecorView().setBackgroundDrawable(null); // Используем background у RecyclerView

        if (savedInstanceState == null) {
            SubscriptionsFeedFragment feedFragment;
            feedFragment = SubscriptionsFeedFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .add(R.id.container, feedFragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean hasMenu = ViewConfiguration.get(this).hasPermanentMenuKey();

        if (!hasMenu) {
            return super.onCreateOptionsMenu(menu);
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshData();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    @Override
    int getCurrentTabId() {
        return R.id.btn_tabbar_subscriptions;
    }

    @Override
    void onCurrentTabButtonClicked() {
        refreshData();
    }

    @Override
    public void onSharePostMenuClicked(Entry entry) {
        Intent intent = new Intent(this, SharePostActivity.class);
        intent.putExtra(SharePostActivity.ARG_ENTRY, entry);
        startActivity(intent);
    }

    void refreshData() {
        IRereshable fragment = (IRereshable)getFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) fragment.refreshData(true);
    }
}