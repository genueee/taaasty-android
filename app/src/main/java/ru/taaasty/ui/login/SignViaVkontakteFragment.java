package ru.taaasty.ui.login;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.CurrentUser;
import ru.taaasty.rest.service.ApiSessions;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.UiUtils;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.Subscriptions;

/**
 * Created by alexey on 18.09.14.
 */
public class SignViaVkontakteFragment extends DialogFragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SignVkontakteFrag";

    private OnFragmentInteractionListener mListener;

    private Subscription mAuthSubscription = Subscriptions.unsubscribed();
    private Subscription mSignupSubscription = Subscriptions.unsubscribed();

    public static SignViaVkontakteFragment createInstance() {
        return new SignViaVkontakteFragment();
    }

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
        return inflater.inflate(R.layout.fragment_sign_up_vkontakte_facebook, container, false);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.NoFrameDialog);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            // Добываем токен
            VKSdk.login(this, Constants.VK_SCOPE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        VKCallback<VKAccessToken> callback = new VKCallback<VKAccessToken>() {
            @Override
            public void onResult(VKAccessToken res) {
                // User passed Authorization
                if (DBG) Log.v(TAG, "vkontakte token:" + UiUtils.vkTokenToString(res));
                loginToTheServer(res);
            }

            @Override
            public void onError(VKError vkError) {
                // User didn't pass Authorization
                if (vkError.errorCode != VKError.VK_CANCELED) {
                    CharSequence errorText;
                    if (TextUtils.isEmpty(vkError.errorMessage)) {
                        errorText = getText(R.string.error_vkontakte_failed);
                    } else {
                        errorText = vkError.errorMessage;
                    }
                    if (mListener != null) mListener.notifyError(errorText, null);
                    if (getActivity() != null) {
                        AnalyticsHelper.getInstance().sendAccountEvent("Ошибка Вконтакте", vkError.toString());
                    }
                }
                getDialog().dismiss();
            }
        };

        if (!VKSdk.onActivityResult(requestCode, resultCode, data, callback)) {
            super.onActivityResult(requestCode, resultCode, data);
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

    public void loginToTheServer(VKAccessToken token) {
        mAuthSubscription.unsubscribe();

        ApiSessions sessionService = RestClient.getAPiSessions();

        Observable<CurrentUser> observableUser = sessionService.signInVkontakte(token.userId,
                token.accessToken);

        mAuthSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new AuthObserver(token));
    }

    void signupVkontakte(VKAccessToken token, JSONObject data) {
        mSignupSubscription.unsubscribe();
        ApiUsers usersService = RestClient.getAPiUsers();

        String nickname = null;
        String avatarUrl = null;
        String name = null;
        String firstName = null;
        String secondName = null;
        Integer sex = null;

        try {
            JSONArray array = data.getJSONArray("response");
            JSONObject object = (JSONObject) array.get(0);
            try {
                nickname = object.getString("domain");
            } catch (JSONException ex) {
                Log.e(TAG, "no domain", ex);
            }
            try {
                avatarUrl = object.getString("photo_max");
            } catch (JSONException ex) {
                Log.e(TAG, "no photo_max", ex);
            }
            try {
                firstName =   object.getString("first_name");
            } catch (JSONException ex) {
                Log.e(TAG, "no nickname", ex);
            }
            try {
                secondName =   object.getString("last_name");
            } catch (JSONException ex) {
                Log.e(TAG, "no nickname", ex);
            }
            try {
                name =   object.getString("nickname");
            } catch (JSONException ex) {
                Log.e(TAG, "no nickname", ex);
            }
            try {
                sex =   object.getInt("sex");
            } catch (JSONException ex) {
                Log.e(TAG, "no nickname", ex);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(nickname)) nickname = "user" + String.valueOf(new Random().nextInt(Integer.MAX_VALUE));

        Observable<CurrentUser> observableUser =
                usersService.registerUserVkontakte(token.accessToken,
                        nickname,
                        avatarUrl,
                        name,
                        firstName,
                        secondName,
                        sex
                );
        mSignupSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SignupObserver());
    }

    void signUp(final VKAccessToken token) {
        VKRequest request = VKApi.users().get(VKParameters.from(VKApiConst.FIELDS,
                "id,sex,photo_max,domain,nickname,first_name,last_name"));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                if (DBG) Log.v(TAG, "response: " + response.responseString);
                signupVkontakte(token, response.json);
            }

            @Override
            public void onError(VKError error) {
                String msg = getString(R.string.error_vkontakte_failed);
                if (mListener != null) mListener.notifyError(msg, new Exception(error.toString()));
                getDialog().dismiss();
            }
        });
    }

    public interface OnFragmentInteractionListener {
        void onSignViaVkontakteSuccess(boolean newUserCreated);
        void notifyError(CharSequence error, @Nullable Throwable exception);
    }

    final class AuthObserver implements Observer<CurrentUser> {

        private final VKAccessToken mToken;

        public AuthObserver(VKAccessToken token) {
            mToken = token;
        }

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            // По коду ошибки хрен что разберешь. В любой непонятной ситуации - регимся
            signUp(mToken);
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                Session.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaVkontakteSuccess(false);
            }
        }
    }

    final class SignupObserver implements Observer<CurrentUser> {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            if (mListener != null) mListener.notifyError(
                    ((ApiErrorException)e).getErrorUserMessage(getResources(), R.string.error_vkontakte_failed),
                    e);
            getDialog().dismiss();
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                // TODO: заливать аватарку на сервер?
                Session.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaVkontakteSuccess(true);
            }
        }
    }

}
