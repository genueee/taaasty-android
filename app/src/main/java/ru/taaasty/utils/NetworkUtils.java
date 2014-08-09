package ru.taaasty.utils;


import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.OkHttpClient;
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

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.UserManager;

public final class NetworkUtils {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";

    private static NetworkUtils mUtils;

    private final Gson mGson;

    private final GsonConverter mGsonConverter;

    private UserManager mUserManager = UserManager.getInstance();

    private OkHttpClient mOkHttpClient;

    private LruCache mPicassoCache;

    private Picasso mPicasso;

    private final Object mSyncObject = new Object();

    private NetworkUtils() {
        mGson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        mGsonConverter = new GsonConverter(mGson);
    }

    public static NetworkUtils getInstance() {
        if (mUtils == null) {
            mUtils = new NetworkUtils();
        }
        return mUtils;
    }

    public void onAppInit(Context context) {
        initOkHttpClient(context);
        initLruMemoryCache(context);
    }

    private void initOkHttpClient(Context context) {
        mOkHttpClient = new OkHttpClient();
        mOkHttpClient.setConnectTimeout(Constants.CONNECT_TIMEOUT_S, TimeUnit.SECONDS);
        mOkHttpClient.setReadTimeout(Constants.READ_TIMEOUT_S, TimeUnit.SECONDS);
        File httpCacheDir = NetworkUtils.getCacheDir(context);
        if (httpCacheDir != null) {
            long cacheSize = NetworkUtils.calculateDiskCacheSize(httpCacheDir);
            if (DBG) Log.v(TAG, "cache size, mb: " + cacheSize / 1024 / 1024);
            try {
                // HttpResponseCache.install(httpCacheDir, cacheSize);
                Cache cache = new Cache(httpCacheDir, cacheSize);
                mOkHttpClient.setCache(cache);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initLruMemoryCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        double memoryClass = am.getMemoryClass();
        int cacheSize = (int)(memoryClass * 1024.0 * 1024.0 * Constants.LRU_MEMORY_CACHE_PCT / 100.0);
        mPicassoCache = new LruCache(cacheSize);
    }

    public void onTrimMemory() {
        synchronized (mSyncObject) {
            if (mPicasso != null) {
                mPicasso.shutdown();
                mPicasso = null;
            }
            mPicassoCache = null;
        }
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
     }

    public Gson getGson() {
        return mGson;
    }

    public RestAdapter createRestAdapter() {
        RestAdapter.Builder b = new RestAdapter.Builder();

        if (BuildConfig.DEBUG) b.setLogLevel(RestAdapter.LogLevel.FULL);
        b.setEndpoint(BuildConfig.API_SERVER_ADDRESS + "/" + Constants.API_VERSION)
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mRequestInterceptor)
                .setClient(new OkClient(mOkHttpClient))
        ;
        return b.build();
    }

    public Picasso getPicasso(Context context) {
        synchronized (mSyncObject) {
            if (mPicasso == null) {
                initLruMemoryCache(context.getApplicationContext());
                mPicasso = new Picasso.Builder(context.getApplicationContext())
                        .memoryCache(mPicassoCache)
                        .downloader(new OkHttpDownloader(mOkHttpClient))
                        .build();
            }
            return mPicasso;
        }
    }

    @Nullable
    public static ThumborUrlBuilder createThumborUrl(String url) {
        Matcher m = Constants.THUMBOR_MATCHER_PATTERN.matcher(url);
        if (!m.matches()) return null;
        return createThumborUrlFromPath(m.group(1));
    }

    public static ThumborUrlBuilder createThumborUrlFromPath(String path) {
        return Thumbor.create(BuildConfig.THUMBOR_SERVER, BuildConfig.THUMBOR_KEY)
                .buildImage(path);
    }

    private final RequestInterceptor mRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestFacade request) {
            String token = mUserManager.getCurrentUserToken();
            if (token != null) {
                request.addHeader(Constants.HEADER_X_USER_TOKEN, token);
            }
        }
    };

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

}
