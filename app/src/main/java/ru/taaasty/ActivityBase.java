package ru.taaasty;


import android.content.Context;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public abstract class ActivityBase extends RxAppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    /*
    @Override
    public boolean onMenuItemSelected(int featureId, android.view.MenuItem item) {
        // http://stackoverflow.com/a/25674354/2971719/
        // IAE Возникает в TlogActivity на LG 4.1, так как там span'ы в экшнбаре
        // fix android formatted title bug
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                && item.getTitleCondensed() != null) {
            item.setTitleCondensed(item.getTitleCondensed().toString());
        }

        if (super.onMenuItemSelected(featureId, item)) {
            return true;
        }

        final ActionBar ab = getSupportActionBar();
        if (item.getItemId() == android.R.id.home && ab != null &&
                (ab.getDisplayOptions() & ActionBar.DISPLAY_HOME_AS_UP) != 0) {
            return onSupportNavigateUp();
        }
        return false;
    }
    */
}
