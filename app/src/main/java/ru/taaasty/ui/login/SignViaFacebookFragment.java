package ru.taaasty.ui.login;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.internal.ImageRequest;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Random;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.UserManager;
import ru.taaasty.rest.ResponseErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.UnauthorizedException;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.service.ApiSessions;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.ui.CustomErrorView;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by alexey on 18.09.14.
 */
public class SignViaFacebookFragment extends DialogFragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SignFacebookFrag";

    private static final String FACEBOOK_PERMISSIONS[] = new String[] {"public_profile", "email" };

    private OnFragmentInteractionListener mListener;

    private Subscription mAuthSubscription = Subscriptions.unsubscribed();
    private Subscription mSignupSubscription = Subscriptions.unsubscribed();

    public static SignViaFacebookFragment createInstance() {
        return new SignViaFacebookFragment();
    }

    private CallbackManager mCallbackManager;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_sign_up_vkontakte_facebook, container, false);
        ((TextView)root.findViewById(R.id.title)).setText(R.string.sign_up_using_facebook_title);
        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.NoFrameDialog);
        mCallbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        if (DBG) Log.v(TAG, "facebook token:" + loginResult.getAccessToken());
                        // Токен добыт. Авторизуемся на сервере.
                        loginToTheServer(loginResult.getAccessToken());
                        // App code
                    }

                    @Override
                    public void onCancel() {
                        getDialog().dismiss();
                    }

                    @Override
                    public void onError(FacebookException exception) {
                        if (mListener != null)
                            mListener.notifyError(getText(R.string.error_facebook_failed), exception);
                        getDialog().dismiss();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AccessToken accessToken = AccessToken.getCurrentAccessToken();
            if (accessToken != null && !accessToken.isExpired()) {
                loginToTheServer(accessToken);
            } else {
                // Добываем токен
                LoginManager.getInstance().logInWithReadPermissions(getActivity(), Arrays.asList(FACEBOOK_PERMISSIONS));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAuthSubscription.unsubscribe();
        mSignupSubscription.unsubscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void loginToTheServer(AccessToken token) {
        mAuthSubscription.unsubscribe();

        ApiSessions sessionService = RestClient.getAPiSessions();

        Observable<CurrentUser> observableUser = AppObservable.bindFragment(this,
                sessionService.signInFacebook(token.getUserId(), token.getToken()));

        mAuthSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AuthObserver(token));
    }


    void signUp(final AccessToken token) {
        String requestFields = "id,name,first_name,last_name,gender,email";
         GraphRequest request = GraphRequest.newMeRequest(
                 token, new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject me, GraphResponse response) {
                            if (response.getError() != null) {
                                String msg = response.getError().getErrorUserMessage();
                                if (msg == null) msg = getString(R.string.error_facebook_failed);
                                if (mListener != null) mListener.notifyError(msg, response.getError().getException());
                                if (getActivity() != null) {
                                    ((TaaastyApplication)getActivity().getApplication())
                                            .sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_LOGIN,
                                                    "Ошибка Facebook",
                                                    response.getError().getErrorType() + " code/subcode:" + response.getError().getErrorCode()
                                                    + "/" + response.getError().getSubErrorCode());
                                }
                                getDialog().dismiss();
                            } else {
                                if (DBG) Log.v(TAG, "response: " + me);
                                signupFacebook(token, me);
                            }
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", requestFields);
            request.setParameters(parameters);
            GraphRequest.executeBatchAsync(request);
    }

    void signupFacebook(AccessToken token, JSONObject data) {
        mSignupSubscription.unsubscribe();
        ApiUsers usersService = RestClient.getAPiUsers();

        String fbId = data.optString("id");
        String name = data.optString("name");
        String firstName = data.optString("first_name");
        String secondName = data.optString("last_name");
        String gender = data.optString("gender");
        String email = data.optString("email");
        String avatarUrl = null;
        Integer sex;

        if (!TextUtils.isEmpty(fbId)) {
            ImageRequest.Builder requestBuilder = new ImageRequest.Builder(
                    getActivity(),
                    ImageRequest.getProfilePictureUri(fbId, 720, 720));

            avatarUrl = requestBuilder.build().getImageUri().toString();
        }

        if ("female".equals(gender)) sex = 1;
        else if ("male".equals(gender)) sex = 2;
        else sex = null;

        String nickname = "user" + String.valueOf(new Random().nextInt(Integer.MAX_VALUE));

        if (DBG) Log.v(TAG, "registerUserFacebook nickname: " + nickname + " avatarUrl: " + avatarUrl);
        Observable<CurrentUser> observableUser = AppObservable.bindFragment(this,
                usersService.registerUserFacebook(token.getToken(),
                        nickname,
                        avatarUrl,
                        TextUtils.isEmpty(name) ? null : name,
                        TextUtils.isEmpty(firstName) ? null : firstName,
                        TextUtils.isEmpty(secondName) ? null : secondName,
                        TextUtils.isEmpty(email) ? null : email,
                        sex
                ));

        mSignupSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SignupObserver());
    }

    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onSignViaFacebookSuccess(boolean newUserCreated);
    }

    final class AuthObserver implements Observer<CurrentUser> {

        private final AccessToken mToken;

        public AuthObserver(AccessToken token) {
            mToken = token;
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            CharSequence error = "";
            if (e instanceof ResponseErrorException) {
                error = ((ResponseErrorException)e).getUserError();
            } else if (e instanceof UnauthorizedException) {
                error = ((UnauthorizedException)e).getUserError();
            }
            if (TextUtils.isEmpty(error)) {
                error = getText(R.string.error_facebook_failed);
            }
            // По коду ошибки хрен что разберешь. В любой непонятной ситуации - регимся
            signUp(mToken);
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                UserManager.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaFacebookSuccess(false);
            }
        }
    }

    final class SignupObserver implements Observer<CurrentUser> {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            CharSequence error = "";
            if (e instanceof ResponseErrorException) {
                error = ((ResponseErrorException)e).getUserError();
            } else if (e instanceof UnauthorizedException) {
                error = ((UnauthorizedException)e).getUserError();
            }
            if (TextUtils.isEmpty(error)) {
                error = getText(R.string.error_facebook_failed);
            }

            if (mListener != null) mListener.notifyError(error, e);
            getDialog().dismiss();
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                UserManager.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaFacebookSuccess(true);
            }
        }
    }

}
