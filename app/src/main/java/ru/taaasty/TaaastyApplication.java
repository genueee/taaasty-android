package ru.taaasty;

import android.app.Application;
import android.os.StrictMode;

import ru.taaasty.utils.FontManager;
import ru.taaasty.utils.NetworkUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class TaaastyApplication extends Application {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "TaaastyApplication";

    @Override
    public void onCreate() {
        if ("debug".equals(BuildConfig.BUILD_TYPE)) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyFlashScreen()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    // .penaltyDeath()
                    .build());
        }
        super.onCreate();

        CalligraphyConfig.initDefault(FontManager.FONT_SYSTEM_DEFAULT_PATH, R.attr.fontPath);

        UserManager.getInstance().onAppInit(this);
        NetworkUtils.getInstance().onAppInit(this);
        VkontakteHelper.getInstance().onAppInit();
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
