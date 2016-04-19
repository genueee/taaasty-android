package ru.taaasty.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.login.LoginManager;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.jakewharton.disklrucache.DiskLruCache;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;
import com.vk.sdk.VKSdk;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.gsonfire.GsonFireBuilder;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.PusherService;
import ru.taaasty.Session;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.rest.DateTypeAdapter;
import ru.taaasty.rest.model.conversations.Conversation;

public final class NetworkUtils {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";

    private static NetworkUtils mUtils;

    private static volatile Gson sGson; //XXX: отдельный класс?

    private OkHttpClient mOkHttpClient;

    private LruCache mPicassoCache;

    private DiskLruCache mGifCache;

    private static final Thumbor sThumbor = Thumbor.create(BuildConfig.THUMBOR_SERVER, BuildConfig.THUMBOR_KEY);

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
                    GsonFireBuilder builder = new GsonFireBuilder()
                            .registerTypeSelector(Conversation.class, Conversation.GSON_TYPE_SELECTOR);
                    sGson = builder.createGsonBuilder()
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
        initGifCache(context);
    }

    public LruCache getImageCache() {
        return mPicassoCache;
    }

    public DiskLruCache getGifCache() {
        return mGifCache;
    }

    private void initOkHttpClient(Context context) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(Constants.CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT_S, TimeUnit.SECONDS);

        if (DBG) BuildConfig.STETHO.configureInterceptor(builder);

        File httpCacheDir = NetworkUtils.getCacheDir(context);
        if (httpCacheDir != null) {
            long cacheSize = NetworkUtils.calculateDiskCacheSize(httpCacheDir);
            if (DBG) Log.v(TAG, "cache size, mb: " + cacheSize / 1024 / 1024);
            Cache cache = new Cache(httpCacheDir, cacheSize);
            builder.cache(cache);
        }

        mOkHttpClient = builder.build();
    }

    private void initLruMemoryCache(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        double memoryClass = am.getMemoryClass();
        int cacheSize = (int)(memoryClass * 1024.0 * 1024.0 * Constants.LRU_MEMORY_CACHE_PCT / 100.0);
        if (DBG) Log.v(TAG, "LRU memory cache size: " + cacheSize);
        mPicassoCache = new LruCache(cacheSize);
    }

    private void initGifCache(Context context) {
        File cacheDir = NetworkUtils.getGifCacheDir(context);
        if (cacheDir != null) {
            long cacheSize = Constants.GIF_DISK_CACHE_SIZE * 1024L * 1024L;
            try {
                mGifCache = DiskLruCache.open(cacheDir, 1, 1, cacheSize);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initPicasso(Context context) {
        Picasso picasso = new Picasso.Builder(context.getApplicationContext())
                .memoryCache(mPicassoCache)
                .downloader(new OkHttp3Downloader(mOkHttpClient))
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
        StatusBarNotifications.getInstance().onLogout();
        Session.getInstance().logout();
        LoginManager.getInstance().logOut();
        VKSdk.logout();
        PusherService.stopPusher(context);
        try {
            mOkHttpClient.cache().delete();
        } catch (Exception ignore) {}

        try {
            mGifCache.delete();
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

    // TODO придумать, почему мы этого не делаем интерцептором к picasso
    @Nullable
    public static ThumborUrlBuilder createThumborUrl(@NonNull String url) {
        return sThumbor.buildImage(url)
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

    public static File getGifCacheDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        if (cacheDir == null) return null;

        return new File(cacheDir, "gifcache");
    }

    public static String hashUrlMurmur3(String url) {
        MurmurHash3.LongPair longpair = new MurmurHash3.LongPair();
        byte bytes[] = url.getBytes();
        MurmurHash3.murmurhash3_x64_128(url.getBytes(), 0, bytes.length, 104729, longpair);
        return unsignedLongToString(longpair.val1, 36) + unsignedLongToString(longpair.val2, 36);
    }

    public static String unsignedLongToString(long x, int radix) {
        if (x == 0) {
            // Simply return "0"
            return "0";
        } else {
            char[] buf = new char[64];
            int i = buf.length;
            if (x < 0) {
                // Split x into high-order and low-order halves.
                // Individual digits are generated from the bottom half into which
                // bits are moved continously from the top half.
                long top = x >>> 32;
                long bot = (x & 0xffffffffl) + ((top % radix) << 32);
                top /= radix;
                while ((bot > 0) || (top > 0)) {
                    buf[--i] = Character.forDigit((int) (bot % radix), radix);
                    bot = (bot / radix) + ((top % radix) << 32);
                    top /= radix;
                }
            } else {
                // Simple modulo/division approach
                while (x > 0) {
                    buf[--i] = Character.forDigit((int) (x % radix), radix);
                    x /= radix;
                }
            }
            // Generate string
            return new String(buf, i, buf.length - i);
        }
    }
}
