package ru.taaasty;


import android.util.Log;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKScope;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKSdkListener;
import com.vk.sdk.api.VKError;
import com.vk.sdk.dialogs.VKCaptchaDialog;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.VkGlobalEvent;

public class VkontakteHelper extends VKSdkListener {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "VkontakteHelper";


    private static VkontakteHelper sInstance;

    public static final String[] VK_SCOPE = new String[]{
            VKScope.FRIENDS,
            VKScope.WALL,
            VKScope.PHOTOS,
            VKScope.NOHTTPS
    };


    public static VkontakteHelper getInstance() {
        if (sInstance == null) {
            sInstance = new VkontakteHelper();
        }
        return sInstance;
    }

    public void onAppInit() {
        VKSdk.initialize(this, BuildConfig.VKONTAKTE_APP_ID);
    }

    @Override
    public void onCaptchaError(VKError captchaError) {
        new VKCaptchaDialog(captchaError).show();
        EventBus.getDefault().post(VkGlobalEvent.captchaError(captchaError));
    }

    @Override
    public void onTokenExpired(VKAccessToken expiredToken) {
        VKSdk.authorize(VK_SCOPE);
        EventBus.getDefault().post(VkGlobalEvent.tokenExpired(expiredToken));
    }

    @Override
    public void onAccessDenied(VKError authorizationError) {
        /* new AlertDialog.Builder(VKUIHelper.getTopActivity())
                .setMessage(authorizationError.toString())
                .show(); */
        EventBus.getDefault().post(VkGlobalEvent.accessDenied(authorizationError));
    }

    @Override
    public void onReceiveNewToken(VKAccessToken newToken) {
        Log.v(TAG, "onReceiveNewToken userId:" + newToken.userId +
                " token:" + newToken.accessToken +
                " secret: " + newToken.secret + " ");
        EventBus.getDefault().post(VkGlobalEvent.receiveNewToken(newToken));
    }

    @Override
    public void onAcceptUserToken(VKAccessToken token) {
        super.onAcceptUserToken(token);
        Log.v(TAG, "onAcceptUserToken userId:" + token.userId +
                " token:" + token.accessToken +
                " secret: " + token.secret + " ");
        EventBus.getDefault().post(VkGlobalEvent.acceptUserToken(token));
    }

    @Override
    public void onRenewAccessToken(VKAccessToken token) {
        super.onRenewAccessToken(token);
        Log.v(TAG, "onRenewAccessToken userId:" + token.userId +
                " token:" + token.accessToken +
                " secret: " + token.secret + " ");
        EventBus.getDefault().post(VkGlobalEvent.renewAccessToken(token));
    }

    public static String vkTokenToString(VKAccessToken token) {
        if (token == null) return "null";
        return "VKAccessToken{" +
                "accessToken='" + token.accessToken + '\'' +
                ", expiresIn=" + token.expiresIn +
                ", userId='" + token.userId + '\'' +
                ", secret='" + token.secret + '\'' +
                ", httpsRequired=" + token.httpsRequired +
                ", created=" + token.created +
                '}';
    }

}
