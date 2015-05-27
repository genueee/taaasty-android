package ru.taaasty.utils;


import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.login.LoginManager;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.StatusBarNotification;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.UserManager;
import ru.taaasty.rest.DateTypeAdapter;
import ru.taaasty.ui.login.LoginActivity;

public final class NetworkUtils {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";

    private static NetworkUtils mUtils;

    private static volatile Gson sGson; //XXX: отдельный класс?

    private OkHttpClient mOkHttpClient;

    private LruCache mPicassoCache;

    private NetworkUtils() {
    }

    public static NetworkUtils getInstance() {
        if (mUtils == null) {
            mUtils = new NetworkUtils();
        }
        return mUtils;
    }

    public static Gson getGson() {
        if (sGson == null) {
            synchronized (Gson.class) {
                if (sGson == null) {
                    sGson = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .registerTypeAdapter(Date.class, new DateTypeAdapter())
                            .create();
                }
            }
        }
        return sGson;
    }

    public void onAppInit(Context context) {
        initOkHttpClient(context);
        initLruMemoryCache(context);
        initPicasso(context);
    }

    public LruCache getImageCache() {
        return mPicassoCache;
    }

    private void initOkHttpClient(Context context) {
        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.setConnectTimeout(Constants.CONNECT_TIMEOUT_S, TimeUnit.SECONDS);
        mOkHttpClient.setReadTimeout(Constants.READ_TIMEOUT_S, TimeUnit.SECONDS);
        File httpCacheDir = NetworkUtils.getCacheDir(context);
        if (httpCacheDir != null) {
            long cacheSize = NetworkUtils.calculateDiskCacheSize(httpCacheDir);
            if (DBG) Log.v(TAG, "cache size, mb: " + cacheSize / 1024 / 1024);
            Cache cache = new Cache(httpCacheDir, cacheSize);
            mOkHttpClient.setCache(cache);
        }
        //if (DBG) mOkHttpClient.networkInterceptors().add(new OkLoggingInterceptor());
    }

    private void initLruMemoryCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        double memoryClass = am.getMemoryClass();
        int cacheSize = (int)(memoryClass * 1024.0 * 1024.0 * Constants.LRU_MEMORY_CACHE_PCT / 100.0);
        if (DBG) Log.v(TAG, "LRU memory cache size: " + cacheSize);
        mPicassoCache = new LruCache(cacheSize);
    }

    private void initPicasso(Context context) {
        Picasso picasso = new Picasso.Builder(context.getApplicationContext())
                .memoryCache(mPicassoCache)
                .downloader(new OkHttpDownloader(mOkHttpClient) {
                                @Override
                                public Response load(Uri uri, int networkPolicy) throws IOException {
                                    if (DBG)
                                        Log.v(TAG, "Load uri: " + uri + " net policy: " + networkPolicy);
                                    return super.load(uri, networkPolicy);
                                }
                            }
                )
                .listener(new Picasso.Listener() {
                    @Override
                    public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
                        Log.i(TAG, "onImageLoadFailed() uri: " + uri, exception);
                        if (exception instanceof RuntimeException && (exception.getCause() instanceof OutOfMemoryError)) {
                            mPicassoCache.clear();
                        }
                    }
                })
                .build();
        Picasso.setSingletonInstance(picasso);
    }

    public void onTrimMemory() {
        mPicassoCache.clear();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public void factoryReset(Context context) {
        GcmUtils.getInstance(context).onLogout();
        StatusBarNotification.getInstance().onLogout();
        ((TaaastyApplication) context.getApplicationContext()).endIntercomSession();
        UserManager.getInstance().logout();
        LoginActivity.logout(context);
        LoginManager.getInstance().logOut();
        try {
            mOkHttpClient.getCache().delete();
        } catch (Exception ignore) {}

        try {
            File extCacheDir = context.getExternalCacheDir();
            if (extCacheDir != null) deleteDir(extCacheDir);
        } catch (Exception ex) { Log.e(TAG, "clear external cache error", ex); }

        try {
            File cacheDir = context.getCacheDir();
            if (cacheDir != null) deleteDir(cacheDir);
        } catch (Exception ex) {Log.e(TAG, "clear internal cache error", ex); }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    @Nullable
    public static ThumborUrlBuilder createThumborUrl(String url) {
        Matcher m = Constants.THUMBOR_MATCHER_PATTERN.matcher(url);
        if (!m.matches()) return null;
        return createThumborUrlFromPath(m.group(1));
    }

    public static ThumborUrlBuilder createThumborUrlFromPath(String path) {
        return Thumbor.create(BuildConfig.THUMBOR_SERVER, BuildConfig.THUMBOR_KEY)
                .buildImage(path)
                .filter(ThumborUrlBuilder.stripicc());
    }

    public static long calculateDiskCacheSize(File dir) {
        long size = Constants.MIN_DISK_CACHE_SIZE;

        try {
            StatFs statFs = new StatFs(dir.getAbsolutePath());
            long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
            // Target 2% of the total space.
            size = available / 50;
        } catch (IllegalArgumentException ignored) {
        }

        // Bound inside min/max size for disk cache.
        return Math.max(Math.min(size, Constants.MAX_DISK_CACHE_SIZE), Constants.MIN_DISK_CACHE_SIZE);
    }

    @Nullable
    public static File getCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        if (cacheDir == null) return null;

        return new File(cacheDir, "taaasty");
    }

    private class OkLoggingInterceptor implements Interceptor {
        @Override public com.squareup.okhttp.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            Log.i("Ok", String.format("REQ     %s on %n%s%n",
                    request.url(), request.headers()));

            com.squareup.okhttp.Response response = chain.proceed(request);

            Log.i("Ok", String.format("RESP %d %s, %n%s%n",
                    response.code(), response.request().url(), response.headers()));

            return response;
        }
    }

}
