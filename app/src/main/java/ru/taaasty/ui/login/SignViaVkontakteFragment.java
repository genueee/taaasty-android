package ru.taaasty.ui.login;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.vk.sdk.VKAccessToken;
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

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.VkontakteHelper;
import ru.taaasty.events.VkGlobalEvent;
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
        EventBus.getDefault().register(this);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState == null) {
            // Добываем токен
            VKSdk.authorize(VkontakteHelper.VK_SCOPE, true, false);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAuthSubscription.unsubscribe();
        mSignupSubscription.unsubscribe();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void onEventMainThread(VkGlobalEvent event) {
        switch (event.type) {
            case VkGlobalEvent.VK_ACCESS_DENIED:
                // Доступ запрещен.
                assert event.vkError != null;
                if (event.vkError.errorCode != VKError.VK_CANCELED) {
                    CharSequence errorText;
                    if (TextUtils.isEmpty(event.vkError.errorMessage)) {
                        errorText = getText(R.string.error_vkontakte_failed);
                    } else {
                        errorText = event.vkError.errorMessage;
                    }
                    if (mListener != null) mListener.notifyError(errorText, null);
                }
                getDialog().dismiss();
                break;
            case VkGlobalEvent.VK_ACCEPT_USER_TOKEN:
            case VkGlobalEvent.VK_RECEIVE_NEW_TOKEN:
                if (DBG) Log.v(TAG, "vkontakte token:" + VkontakteHelper.vkTokenToString(event.token));
                // Токен добыт. Авторизуемся на сервере.
                loginToTheServer(event.token);
                break;
            default:
                break;
        }
    }

    public void loginToTheServer(VKAccessToken token) {
        mAuthSubscription.unsubscribe();

        ApiSessions sessionService = RestClient.getAPiSessions();

        Observable<CurrentUser> observableUser = AppObservable.bindFragment(this,
                sessionService.signInVkontakte(token.userId, token.accessToken));

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

        Observable<CurrentUser> observableUser = AppObservable.bindFragment(this,
                usersService.registerUserVkontakte(token.accessToken,
                        nickname,
                        avatarUrl,
                        name,
                        firstName,
                        secondName,
                        sex
                ));
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

    public interface OnFragmentInteractionListener extends CustomErrorView {
        void onSignViaVkontakteSuccess(boolean newUserCreated);
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
            CharSequence error = "";
            if (e instanceof ResponseErrorException) {
                error = ((ResponseErrorException)e).getUserError();
            } else if (e instanceof UnauthorizedException) {
                error = ((UnauthorizedException)e).getUserError();
            }
            if (TextUtils.isEmpty(error)) {
                error = getText(R.string.error_vkontakte_failed);
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
            CharSequence error = "";
            if (e instanceof ResponseErrorException) {
                error = ((ResponseErrorException)e).getUserError();
            } else if (e instanceof UnauthorizedException) {
                error = ((UnauthorizedException)e).getUserError();
            }
            if (TextUtils.isEmpty(error)) {
                error = getText(R.string.error_vkontakte_failed);
            }

            if (mListener != null) mListener.notifyError(error, e);
            getDialog().dismiss();
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                // TODO: заливать аватарку на сервер?
                UserManager.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaVkontakteSuccess(true);
            }
        }
    }

}
