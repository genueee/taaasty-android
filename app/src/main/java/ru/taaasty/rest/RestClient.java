package ru.taaasty.rest;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.UserManager;
import ru.taaasty.rest.model.ResponseError;
import ru.taaasty.rest.service.ApiApp;
import ru.taaasty.rest.service.ApiComments;
import ru.taaasty.rest.service.ApiDesignSettings;
import ru.taaasty.rest.service.ApiDevice;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.rest.service.ApiFeeds;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.rest.service.ApiMyFeeds;
import ru.taaasty.rest.service.ApiRelationships;
import ru.taaasty.rest.service.ApiSessions;
import ru.taaasty.rest.service.ApiTlog;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.utils.NetworkUtils;

// http://blog.robinchutaux.com/blog/a-smart-way-to-use-retrofit/
public final class RestClient {
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "NetworkUtils";

    private static volatile RestClient sInstance;

    private RestAdapter mRestAdapter; // http://stackoverflow.com/a/21250503/2971719

    private volatile ApiApp mApiApp;
    private volatile ApiComments mApiComments;
    private volatile ApiDesignSettings mApiDesignSettings;
    private volatile ApiDevice mApiDevice;
    private volatile ApiEntries mApiEntries;
    private volatile ApiFeeds mApiFeeds;
    private volatile ApiMessenger mApiMessenger;
    private volatile ApiMyFeeds mApiMyFeeds;
    private volatile ApiRelationships mApiRelationships;
    private volatile ApiSessions mApiSessions;
    private volatile ApiTlog mApiTlog;
    private volatile ApiUsers mApiUsers;

    private RestClient() {
        mRestAdapter = new RestAdapter.Builder()
                .setLogLevel(Constants.RETROFIT_LOG_LEVEL)
                .setEndpoint(BuildConfig.API_SERVER_ADDRESS + "/" + Constants.API_VERSION)
                .setConverter(new GsonConverter(NetworkUtils.getGson()))
                .setRequestInterceptor(new AddHeadersRequestInterceptor())
                .setErrorHandler(new ResponseErrorExceptionErrorHandler())
                .setClient(new OkClient(NetworkUtils.getInstance().getOkHttpClient()))
                .build();
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
                    instance.mApiApp = instance.mRestAdapter.create(ApiApp.class);
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
                    instance.mApiComments = instance.mRestAdapter.create(ApiComments.class);
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
                    instance.mApiDesignSettings = instance.mRestAdapter.create(ApiDesignSettings.class);
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
                    instance.mApiDevice = instance.mRestAdapter.create(ApiDevice.class);
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
                    instance.mApiEntries = instance.mRestAdapter.create(ApiEntries.class);
                }
            }
        }
        return instance.mApiEntries;
    }

    public static ApiFeeds getAPiFeeds() {
        RestClient instance = getInstance();
        if (instance.mApiFeeds == null) {
            synchronized (RestClient.class) {
                if (instance.mApiFeeds == null) {
                    instance.mApiFeeds = instance.mRestAdapter.create(ApiFeeds.class);
                }
            }
        }
        return instance.mApiFeeds;
    }

    public static ApiMessenger getAPiMessenger() {
        RestClient instance = getInstance();
        if (instance.mApiMessenger == null) {
            synchronized (RestClient.class) {
                if (instance.mApiMessenger == null) {
                    instance.mApiMessenger = instance.mRestAdapter.create(ApiMessenger.class);
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
                    instance.mApiMyFeeds = instance.mRestAdapter.create(ApiMyFeeds.class);
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
                    instance.mApiRelationships = instance.mRestAdapter.create(ApiRelationships.class);
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
                    instance.mApiSessions = instance.mRestAdapter.create(ApiSessions.class);
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
                    instance.mApiTlog = instance.mRestAdapter.create(ApiTlog.class);
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
                    instance.mApiUsers = instance.mRestAdapter.create(ApiUsers.class);
                }
            }
        }
        return instance.mApiUsers;
    }

    private static class AddHeadersRequestInterceptor implements RequestInterceptor {

        private final UserManager mUserManager;
        private final String mBasicAuth;

        AddHeadersRequestInterceptor(){
            mUserManager = UserManager.getInstance();
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

    private static class ResponseErrorExceptionErrorHandler implements ErrorHandler {
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
}