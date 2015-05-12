package ru.taaasty.utils;


import android.app.ActivityManager;
import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.StatusBarNotification;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.UserManager;
import ru.taaasty.model.ResponseError;
import ru.taaasty.ui.login.LoginActivity;

public final class NetworkUtils {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";

    private static NetworkUtils mUtils;

    private final Gson mGson;

    private final GsonConverter mGsonConverter;

    private UserManager mUserManager = UserManager.getInstance();

    private OkHttpClient mOkHttpClient;

    private OkClient mRetrofitClient;

    private LruCache mPicassoCache;

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
        mRetrofitClient = new OkClient(mOkHttpClient);
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

    public Gson getGson() {
        return mGson;
    }

    public RestAdapter createRestAdapter() {
        RestAdapter.Builder b = new RestAdapter.Builder();

        b.setLogLevel(Constants.RETROFIT_LOG_LEVEL);
        b.setEndpoint(BuildConfig.API_SERVER_ADDRESS + "/" + Constants.API_VERSION)
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mRequestInterceptor)
                .setErrorHandler(mErrorHandler)
                .setClient(mRetrofitClient)
        ;
        return b.build();
    }

    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    public void factoryReset(Context context) {
        GcmUtils.getInstance(context).onLogout();
        StatusBarNotification.getInstance().onLogout();
        ((TaaastyApplication) context.getApplicationContext()).endIntercomSession();
        mUserManager.logout();
        LoginActivity.logout(context);
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

    private final RequestInterceptor mRequestInterceptor = new RequestInterceptor() {

        final String mBasicAuth;

        {
            if (!TextUtils.isEmpty(BuildConfig.API_SERVER_LOGIN) && !TextUtils.isEmpty(BuildConfig.API_SERVER_PASSWORD)) {
                final String credentials = BuildConfig.API_SERVER_LOGIN + ":" + BuildConfig.API_SERVER_PASSWORD;
                mBasicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            } else {
                mBasicAuth = null;
            }
        }

        @Override
        public void intercept(RequestFacade request) {
            request.addHeader(Constants.HEADER_X_TASTY_CLIENT, Constants.HEADER_X_TASTY_CLIENT_VALUE);
            request.addHeader(Constants.HEADER_X_TASTY_CLIENT_VERSION, BuildConfig.VERSION_NAME);
            String token = mUserManager.getCurrentUserToken();
            if (token != null) {
                request.addHeader(Constants.HEADER_X_USER_TOKEN, token);
            }
            if (mBasicAuth != null) {
                request.addHeader("Authorization", mBasicAuth);
            }
        }


    };

    public final ErrorHandler mErrorHandler = new ErrorHandler() {
        @Override
        public Throwable handleError(RetrofitError cause) {
            ResponseError responseError = null;
            try {
                responseError = (ResponseError) cause.getBodyAs(ResponseError.class);
            } catch (Exception ignore) {
                if (DBG) Log.v(TAG, "ignore exception", ignore);
            }

            Response r = cause.getResponse();
            if (r != null) {
                switch (r.getStatus()) {
                    case 401:
                        return new UnauthorizedException(cause, responseError);
                    case 417:
                        if (responseError != null && "no_token".equals(responseError.errorCode)) {
                            return new UnauthorizedException(cause, responseError);
                        }
                }
            }
            if (responseError != null) {
                return new ResponseErrorException(cause, responseError);
            } else {
                return cause;
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

    /**
     * 401 код
     */
    public static class UnauthorizedException extends RuntimeException {

        @Nullable
        public final ResponseError error;

        public UnauthorizedException(Throwable throwable, ResponseError error) {
            super(throwable);
            this.error = error;
        }

        @Nullable
        public String getUserError() {
            return error == null ? null : error.error;
        }
    }

    /**
     * Ошибка, которая парсится по ResponseError
     */
    public static class ResponseErrorException extends  RuntimeException {

        public final ResponseError error;

        public ResponseErrorException(Throwable throwable, ResponseError error) {
            super(throwable);
            this.error = error;
        }

        @Nullable
        public String getUserError() {
            return error == null ? null : error.error;
        }
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
