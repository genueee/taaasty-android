package ru.taaasty.events;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.vk.sdk.VKAccessToken;
import com.vk.sdk.api.VKError;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class VkGlobalEvent {
    @Retention(RetentionPolicy.CLASS)
    @IntDef({VK_ACCESS_DENIED, VK_RECEIVE_NEW_TOKEN, VK_ACCEPT_USER_TOKEN, VK_RENEW_USER_TOKEN, VK_CAPTCHA_ERROR, VK_TOKEN_EXPIRED})
    public @interface VkEventType {}

    public static final int VK_ACCESS_DENIED = 0;

    public static final int VK_RECEIVE_NEW_TOKEN = 1;

    public static final int VK_ACCEPT_USER_TOKEN = 2;

    public static final int VK_RENEW_USER_TOKEN = 3;

    public static final int VK_CAPTCHA_ERROR = 4;

    public static final int VK_TOKEN_EXPIRED = 5;

    @VkEventType
    public final int type;

    @Nullable
    public final VKError vkError;

    @Nullable
    public final VKAccessToken token;

    public VkGlobalEvent(int type, VKError error, VKAccessToken token) {
        this.type = type;
        this.vkError = error;
        this.token = token;
    }

    public static VkGlobalEvent captchaError(VKError captchaError) {
        return new VkGlobalEvent(VK_CAPTCHA_ERROR, captchaError, null);
    }

    public static VkGlobalEvent tokenExpired(VKAccessToken expiredToken) {
        return new VkGlobalEvent(VK_TOKEN_EXPIRED, null, expiredToken);
    }

    public static VkGlobalEvent accessDenied(VKError authorizationError) {
        return new VkGlobalEvent(VK_ACCESS_DENIED, authorizationError, null);
    }

    public static VkGlobalEvent acceptUserToken(VKAccessToken token) {
        return new VkGlobalEvent(VK_ACCEPT_USER_TOKEN, null, token);
    }

    public static VkGlobalEvent receiveNewToken(VKAccessToken token) {
        return new VkGlobalEvent(VK_RECEIVE_NEW_TOKEN, null, token);
    }

    public static VkGlobalEvent renewAccessToken(VKAccessToken token) {
        return new VkGlobalEvent(VK_RENEW_USER_TOKEN, null, token);
    }

    @Override
    public String toString() {
        return "VkGlobalEvent{" +
                "type=" + type +
                ", vkError=" + vkError +
                ", token=" + token +
                '}';
    }
}
