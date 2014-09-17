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
import com.vk.sdk.api.VKError;

import de.greenrobot.event.EventBus;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.VkontakteHelper;
import ru.taaasty.events.VkGlobalEvent;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.service.ApiSessions;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SubscriptionHelper;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by alexey on 18.09.14.
 */
public class SignViaVkontakteFragment extends DialogFragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SignViaVkontakteFragment";

    private OnFragmentInteractionListener mListener;

    private Subscription mAuthSubscription = SubscriptionHelper.empty();

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
        return inflater.inflate(R.layout.fragment_sign_up_vkontakte, null, false);
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
                if (DBG) Log.v(TAG, "vokntakte token:" + VkontakteHelper.vkTokenToString(event.token));
                // Токен добыт. Авторизуемся на сервере.
                loginToTheServer(event.token);
                break;
            default:
                break;
        }
    }

    public void loginToTheServer(VKAccessToken token) {
        mAuthSubscription.unsubscribe();

        ApiSessions sessionService = NetworkUtils.getInstance().createRestAdapter().create(ApiSessions.class);

        Observable<CurrentUser> observableUser = AndroidObservable.bindFragment(this,
                sessionService.signInVkontakte(token.userId, token.accessToken));

        mAuthSubscription = observableUser
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mAuthObserver);
    }


    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onSignViaVkontakteSuccess();
    }

    private final Observer<CurrentUser> mAuthObserver = new Observer<CurrentUser>() {

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            mListener.notifyError(getString(R.string.error_vkontakte_failed), e);
            getDialog().dismiss();
        }

        @Override
        public void onNext(CurrentUser info) {
            if (info == null) {
                // XXX
            } else {
                UserManager.getInstance().setCurrentUser(info);
                if (mListener != null) mListener.onSignViaVkontakteSuccess();
            }
        }
    };


}
