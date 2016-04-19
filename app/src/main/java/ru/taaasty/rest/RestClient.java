package ru.taaasty.rest;

import android.text.TextUtils;
import android.util.Base64;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.Session;
import ru.taaasty.rest.model.ResponseError;
import ru.taaasty.rest.service.ApiApp;
import ru.taaasty.rest.service.ApiComments;
import ru.taaasty.rest.service.ApiDesignSettings;
import ru.taaasty.rest.service.ApiDevice;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.rest.service.ApiFeeds;
import ru.taaasty.rest.service.ApiFlows;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.rest.service.ApiMyFeeds;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.rest.service.ApiReposts;
import ru.taaasty.rest.service.ApiSessions;
import ru.taaasty.rest.service.ApiTlog;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.utils.NetworkUtils;

// http://blog.robinchutaux.com/blog/a-smart-way-to-use-retrofit/
public final class RestClient {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";
    private static volatile RestClient sInstance;
    private volatile ApiApp mApiApp;
    private volatile ApiComments mApiComments;
    private volatile ApiDesignSettings mApiDesignSettings;
    private volatile ApiDevice mApiDevice;
    private volatile ApiEntries mApiEntries;
    private volatile ApiFeeds mApiFeeds;
    private volatile ApiFlows mApiFlows;
    private volatile ApiMessenger mApiMessenger;
    private volatile ApiMyFeeds mApiMyFeeds;
    private volatile ApiRelationships mApiRelationships;
    private volatile ApiSessions mApiSessions;
    private volatile ApiTlog mApiTlog;
    private volatile ApiUsers mApiUsers;
    private volatile ApiReposts mApiReposts;

    private static final Retrofit.Builder retrofitBuilder =
            new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_SERVER_ADDRESS + "/" + Constants.API_VERSION+"/")
                    .addConverterFactory(new ScalarRequestBodyConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create(NetworkUtils.getGson()))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create());

    private static final Retrofit.Builder retrofitBuilderV2 =
            new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_SERVER_ADDRESS + "/" + Constants.API_VERSION_V2+"/")
                    .addConverterFactory(new ScalarRequestBodyConverterFactory())
                    .addConverterFactory(GsonConverterFactory.create(NetworkUtils.getGson()))
                    .addCallAdapterFactory(RxJavaCallAdapterFactory.create());

    public static Retrofit getRetrofit(){
        return retrofitBuilder
                .client(getHttpClient())
                .build();
    }
    public static Retrofit getRetrofitV2(){
        return retrofitBuilderV2
                .client(getHttpClient())
                .build();
    }
    private  static <S> S createService(Class<S> serviceClass) {
        return getRetrofit().create(serviceClass);
    }

    private  static <S> S createServiceV2(Class<S> serviceClass) {
        return getRetrofitV2().create(serviceClass);
    }

    private static OkHttpClient getHttpClient() {
        OkHttpClient template = NetworkUtils.getInstance().getOkHttpClient();
        if (template == null) { // Возможно, если по какой-то причине вызвались раньше onAppInit()
            template = new OkHttpClient();
        }

        OkHttpClient.Builder builder = template
                .newBuilder()
                .addInterceptor(new AddHeadersRequestInterceptor())
                .addInterceptor(new ErrorInterceptor());
        if (BuildConfig.OKHTTP_LOG_LEVEL != HttpLoggingInterceptor.Level.NONE) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
                    .setLevel(BuildConfig.OKHTTP_LOG_LEVEL);
            builder.addNetworkInterceptor(loggingInterceptor);
        }
        if (DBG) BuildConfig.STETHO.configureInterceptor(builder);
        return builder.build();
    }

    private RestClient() {
    }

    public static RestClient getInstance() {
        if (sInstance == null) {
            synchronized (RestClient.class) {
                if (sInstance == null) { sInstance = new RestClient(); }
            }
        }
        return sInstance;
    }

    public static ApiApp getAPiApp() {
        RestClient instance = getInstance();
        if (instance.mApiApp == null) {
            synchronized (RestClient.class) {
                if (instance.mApiApp == null) {
                    instance.mApiApp = createService(ApiApp.class);
                }
            }
        }
        return instance.mApiApp;
    }

    public static ApiComments getAPiComments() {
        RestClient instance = getInstance();
        if (instance.mApiComments == null) {
            synchronized (RestClient.class) {
                if (instance.mApiComments == null) {
                    instance.mApiComments = createService(ApiComments.class);
                }
            }
        }
        return instance.mApiComments;
    }

    public static ApiDesignSettings getAPiDesignSettings() {
        RestClient instance = getInstance();
        if (instance.mApiDesignSettings == null) {
            synchronized (RestClient.class) {
                if (instance.mApiDesignSettings == null) {
                    instance.mApiDesignSettings = createService(ApiDesignSettings.class);
                }
            }
        }
        return instance.mApiDesignSettings;
    }

    public static ApiDevice getAPiDevice() {
        RestClient instance = getInstance();
        if (instance.mApiDevice == null) {
            synchronized (RestClient.class) {
                if (instance.mApiDevice == null) {
                    instance.mApiDevice = createService(ApiDevice.class);
                }
            }
        }
        return instance.mApiDevice;
    }

    public static ApiEntries getAPiEntries() {
        RestClient instance = getInstance();
        if (instance.mApiEntries == null) {
            synchronized (RestClient.class) {
                if (instance.mApiEntries == null) {
                    instance.mApiEntries = createService(ApiEntries.class);
                }
            }
        }
        return instance.mApiEntries;
    }

    public static ApiReposts getApiReposts() {
        RestClient instance = getInstance();
        if (instance.mApiReposts == null) {
            synchronized (RestClient.class) {
                if (instance.mApiReposts == null) {
                    instance.mApiReposts = createService(ApiReposts.class);
                }
            }
        }
        return instance.mApiReposts;
    }


    public static ApiFeeds getAPiFeeds() {
        RestClient instance = getInstance();
        if (instance.mApiFeeds == null) {
            synchronized (RestClient.class) {
                if (instance.mApiFeeds == null) {
                    instance.mApiFeeds = createService(ApiFeeds.class);
                }
            }
        }
        return instance.mApiFeeds;
    }

    public static ApiFlows getAPiFlows() {
        RestClient instance = getInstance();
        if (instance.mApiFlows == null) {
            synchronized (RestClient.class) {
                if (instance.mApiFlows == null) {
                    instance.mApiFlows = createService(ApiFlows.class);
                }
            }
        }
        return instance.mApiFlows;
    }

    public static ApiMessenger getAPiMessenger() {
        RestClient instance = getInstance();
        if (instance.mApiMessenger == null) {
            synchronized (RestClient.class) {
                if (instance.mApiMessenger == null) {
                    instance.mApiMessenger = createServiceV2(ApiMessenger.class);
                }
            }
        }
        return instance.mApiMessenger;
    }

    public static ApiMyFeeds getAPiMyFeeds() {
        RestClient instance = getInstance();
        if (instance.mApiMyFeeds == null) {
            synchronized (RestClient.class) {
                if (instance.mApiMyFeeds == null) {
                    instance.mApiMyFeeds = createService(ApiMyFeeds.class);
                }
            }
        }
        return instance.mApiMyFeeds;
    }

    public static ApiRelationships getAPiRelationships() {
        RestClient instance = getInstance();
        if (instance.mApiRelationships == null) {
            synchronized (RestClient.class) {
                if (instance.mApiRelationships == null) {
                    instance.mApiRelationships = createService(ApiRelationships.class);
                }
            }
        }
        return instance.mApiRelationships;
    }

    public static ApiSessions getAPiSessions() {
        RestClient instance = getInstance();
        if (instance.mApiSessions == null) {
            synchronized (RestClient.class) {
                if (instance.mApiSessions == null) {
                    instance.mApiSessions = createService(ApiSessions.class);
                }
            }
        }
        return instance.mApiSessions;
    }

    public static ApiTlog getAPiTlog() {
        RestClient instance = getInstance();
        if (instance.mApiTlog == null) {
            synchronized (RestClient.class) {
                if (instance.mApiTlog == null) {
                    instance.mApiTlog = createService(ApiTlog.class);
                }
            }
        }
        return instance.mApiTlog;
    }

    public static ApiUsers getAPiUsers() {
        RestClient instance = getInstance();
        if (instance.mApiUsers == null) {
            synchronized (RestClient.class) {
                if (instance.mApiUsers == null) {
                    instance.mApiUsers = createService(ApiUsers.class);
                }
            }
        }
        return instance.mApiUsers;
    }

    private static class AddHeadersRequestInterceptor implements Interceptor {

        private final String mBasicAuth;

        private static final String VERSION_NAME = BuildConfig.VERSION_NAME.replace('–', '-');

        AddHeadersRequestInterceptor(){
            if (!TextUtils.isEmpty(BuildConfig.API_SERVER_LOGIN) && !TextUtils.isEmpty(BuildConfig.API_SERVER_PASSWORD)) {
                final String credentials = BuildConfig.API_SERVER_LOGIN + ":" + BuildConfig.API_SERVER_PASSWORD;
                mBasicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
            } else {
                mBasicAuth = null;
            }
        }


        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder()
                    .header(Constants.HEADER_X_TASTY_CLIENT, Constants.HEADER_X_TASTY_CLIENT_VALUE)
                    .header(Constants.HEADER_X_TASTY_CLIENT_VERSION, VERSION_NAME);
            String token = Session.getInstance().getCurrentUserToken();
            if (token != null) {
                requestBuilder.header(Constants.HEADER_X_USER_TOKEN, token);
            }
            if (mBasicAuth != null) {
                requestBuilder.header("Authorization", mBasicAuth);
            }

            Request request = requestBuilder.build();
            return chain.proceed(request);
        }
    }

    private static class ErrorInterceptor implements Interceptor {
        private static ResponseError parseError(Response response) throws IOException {
            Converter<ResponseBody, ResponseError> converter =
                    getRetrofit().responseBodyConverter(ResponseError.class, new Annotation[0]);
            return converter.convert(response.body());
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response response;
            try {
                response = chain.proceed(chain.request());
            } catch (Throwable e) {
                throw new ApiErrorException(e);
            }

            int code = response.code();
            ResponseError parsedResponseError;
            if (code >= 400) {
                try {
                    parsedResponseError = parseError(response);
                } catch (Exception e) {
                    throw new ApiErrorException(e);
                }
                throw new ApiErrorException("HTTP error", parsedResponseError);
            }
            return response;
        }
    }

    /**
     * Контент Multipart-запросов сервер принимает в binary, а не в JSON, не сериализуем его.
     */
    private static class ScalarRequestBodyConverterFactory extends Converter.Factory {

        @Override
        public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            if (type == String.class
                    || type == boolean.class
                    || type == Boolean.class
                    || type == byte.class
                    || type == Byte.class
                    || type == char.class
                    || type == Character.class
                    || type == double.class
                    || type == Double.class
                    || type == float.class
                    || type == Float.class
                    || type == int.class
                    || type == Integer.class
                    || type == long.class
                    || type == Long.class
                    || type == short.class
                    || type == Short.class) {
                return ScalarRequestBodyConverter.INSTANCE;
            }
            return null;
        }

        private static class ScalarRequestBodyConverter<T> implements Converter<T, RequestBody> {
            static final ScalarRequestBodyConverter<Object> INSTANCE = new ScalarRequestBodyConverter<>();
            private static final MediaType MEDIA_TYPE = MediaType.parse("text/plain; charset=UTF-8");

            private ScalarRequestBodyConverter() {
            }

            @Override
            public RequestBody convert(T value) throws IOException {
                return RequestBody.create(MEDIA_TYPE, String.valueOf(value));
            }
        }
    }
}
