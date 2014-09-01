package ru.taaasty;


import android.app.Activity;
import android.content.Context;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public abstract class ActivityBase extends Activity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }


}
