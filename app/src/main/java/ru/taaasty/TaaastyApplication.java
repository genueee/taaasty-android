package ru.taaasty;

import android.app.Application;

import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends Application {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        CalligraphyConfig.initDefault(FontManager.FONT_SANS_PATH, R.attr.fontPath);

        UserManager.getInstance().onAppInit(this);
        NetworkUtils.getInstance().onAppInit(this);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        NetworkUtils.getInstance().onTrimMemory();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        NetworkUtils.getInstance().onTrimMemory();
    }
}
