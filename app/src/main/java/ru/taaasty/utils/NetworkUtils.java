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
import com.jakewharton.disklrucache.DiskLruCache;
import com.squareup.okhttp.Cache;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;
import com.vk.sdk.VKSdk;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import retrofit.RetrofitError;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.PusherService;
import ru.taaasty.Session;
import ru.taaasty.StatusBarNotifications;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.DateTypeAdapter;

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
                    sGson = new GsonBuilder()
                            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                            .registerTypeAdapter(Date.class, new DateTypeAdapter())
                            .create();
                }
            }
        }
        return sGson;
    }

    public static String getUrlApiPath(RetrofitError error) {
        String url = error.getUrl();
        if (url.startsWith(BuildConfig.API_SERVER_ADDRESS)) {
            try {
                Uri uri = Uri.parse(error.getUrl());
                return uri.getPath();
            } catch (Throwable ignore) {}
        }
        return url;

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
        BuildConfig.STETHO.configureInterceptor(mOkHttpClient);
        //if (DBG) mOkHttpClient.networkInterceptors().add(new OkLoggingInterceptor());
        // mOkHttpClient.interceptors().add(new OkLoggingInterceptorInfo());
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
                .downloader(new OkHttpDownloader(mOkHttpClient) {

                    private final CacheControl ALLOW_STALE_CACHE_RESPONSE = new CacheControl.Builder()
                            .maxStale(Integer.MAX_VALUE, TimeUnit.SECONDS)
                            .build();

                    private final OkHttpClient client = mOkHttpClient;

                    @Override
                    public Response load(Uri uri, int networkPolicy) throws IOException {
                        CacheControl cacheControl = null;
                        if(networkPolicy != 0) {
                            if(NetworkPolicy.isOfflineOnly(networkPolicy)) {
                                cacheControl = CacheControl.FORCE_CACHE;
                            } else {
                                CacheControl.Builder builder = new CacheControl.Builder();
                                if(!NetworkPolicy.shouldReadFromDiskCache(networkPolicy)) {
                                    builder.noCache();
                                }

                                if(!NetworkPolicy.shouldWriteToDiskCache(networkPolicy)) {
                                    builder.noStore();
                                }

                                cacheControl = builder.build();
                            }
                        } else {
                            cacheControl = ALLOW_STALE_CACHE_RESPONSE; // Хуякс
                        }

                        com.squareup.okhttp.Request.Builder builder1 = (new com.squareup.okhttp.Request.Builder()).url(uri.toString());
                        if(cacheControl != null) {
                            builder1.cacheControl(cacheControl);
                        }

                        com.squareup.okhttp.Response response = this.client.newCall(builder1.build()).execute();
                        int responseCode = response.code();
                        if(responseCode >= 300) {
                            response.body().close();
                            throw new ResponseException(responseCode + " " + response.message(), networkPolicy, responseCode);
                        } else {
                            boolean fromCache = response.cacheResponse() != null;
                            ResponseBody responseBody = response.body();
                            return new Response(responseBody.byteStream(), fromCache, responseBody.contentLength());
                        }
                    }
                })
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
        StatusBarNotifications.getInstance().onLogout();
        ((TaaastyApplication) context.getApplicationContext()).endIntercomSession();
        Session.getInstance().logout();
        LoginManager.getInstance().logOut();
        VKSdk.logout();
        PusherService.stopPusher(context);
        try {
            mOkHttpClient.getCache().delete();
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
    public static ThumborUrlBuilder createThumborUrl(String url) {
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

    private class OkLoggingInterceptorInfo implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            Response response = chain.proceed(chain.request());
            if (DBG) Log.i("Ok", String.format("RESP %d %s, %s",
                    response.code(), response.request().url(), response.cacheResponse() != null ? "from cache" : "from network"));
            return response;
        }
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
