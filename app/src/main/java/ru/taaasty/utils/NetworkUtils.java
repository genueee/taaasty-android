package ru.taaasty.utils;


import android.support.annotation.Nullable;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.pollexor.Thumbor;
import com.squareup.pollexor.ThumborUrlBuilder;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;
import ru.taaasty.BuildConfig;
import ru.taaasty.UserManager;

public final class NetworkUtils {

    public static final String HEADER_X_USER_TOKEN = "X-User-Token";

    private final GsonConverter mGsonConverter;

    private static NetworkUtils mUtils;

    private static final Pattern THUMBOR_MATCHER_PATTERN = Pattern.compile("http\\://a0\\.tcdn\\.ru/assets/(.+)$");

    private UserManager mUserManager = UserManager.getInstance();

    private NetworkUtils() {
        Gson builder = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Date.class, new DateTypeAdapter())
                .create();
        mGsonConverter = new GsonConverter(builder);
    }

    public static NetworkUtils getInstance() {
        if (mUtils == null) {
            mUtils = new NetworkUtils();
        }
        return mUtils;
    }

    public RestAdapter createRestAdapter() {
        RestAdapter.Builder b = new RestAdapter.Builder();

        if (BuildConfig.DEBUG) b.setLogLevel(RestAdapter.LogLevel.FULL);
        b.setEndpoint(BuildConfig.API_SERVER_ADDRESS)
                .setConverter(mGsonConverter)
                .setRequestInterceptor(mRequestInterceptor);
        return b.build();
    }

    @Nullable
    public static ThumborUrlBuilder createThumborUrl(String url) {
        Matcher m = THUMBOR_MATCHER_PATTERN.matcher(url);
        if (!m.matches()) return null;
        return Thumbor.create(BuildConfig.THUMBOR_SERVER, BuildConfig.THUMBOR_KEY)
            .buildImage(m.group(1));

    }

    private final RequestInterceptor mRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestFacade request) {
            String token = mUserManager.getCurrentUserToken();
            if (token != null) {
                request.addHeader(HEADER_X_USER_TOKEN, token);
            }
        }
    };

}
