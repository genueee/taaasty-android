package ru.taaasty.utils;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

import frenchtoast.FrenchToast;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.Session;
import ru.taaasty.rest.ApiErrorException;
import ru.taaasty.ui.login.LoginActivity;

/**
 * Created by alexey on 02.11.15.
 */
public class MessageHelper {
    private static final int HIDE_DELAY_MIN_MS = 2500;
    private static final int HIDE_DELAY_MS_PER_CHARACTER = 50;

    public static int getHideDelay(CharSequence text) {
        return HIDE_DELAY_MIN_MS + text.length() * HIDE_DELAY_MS_PER_CHARACTER;
    }

    /**
     * @param context
     * @param error
     * @return Toast - красное сообщение сверху
     */
    public static Toast createErrorToast(Context context, CharSequence error) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_layout_error, null, false);

        ((TextView)layout).setText(error);

        android.widget.Toast toast = new android.widget.Toast(context);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        return toast;
    }


    public static Toast createSuccessToast(Context context, CharSequence message) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View layout = inflater.inflate(R.layout.toast_layout_success, null, false);

        ((TextView)layout).setText(message);

        android.widget.Toast toast = new android.widget.Toast(context);
        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        return toast;
    }

    /**
     * Показ ошибки. В случае неавторизованного - snackbar с кнопкой "зарегистрироваться", иначе - toast
     */
    public static void showError(final Fragment fragment,
                                 @Nullable Throwable exception,
                                 final int fallbackErrResId,
                                 final int loginRequestCode) {
        if (!fragment.isResumed()) return;
        final Activity activity = fragment.getActivity();
        if (activity == null) return;

        if (exception instanceof ApiErrorException
                && (((ApiErrorException) exception)).isErrorAuthorizationRequired()
                && !Session.getInstance().isAuthorized()) {
            Snackbar.make(fragment.getView(), R.string.allowed_only_to_registered_users, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_sign_up, v -> {
                        AnalyticsHelper.getInstance().sendFeedsEvent("Открытие логина из сообщения ошибки");
                        LoginActivity.startActivityFromFragment(activity, fragment, loginRequestCode, null);
                    })
                    .show();
        } else {
            String error = UiUtils.getUserErrorText(fragment.getResources(), exception, fallbackErrResId);
            if (BuildConfig.DEBUG) {
                MessageHelper.showError(activity, error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
            } else {
                MessageHelper.showError(activity, error, exception);
            }
        }
    }

    /**
     * Показ ошибки. В случае неавторизованного - snackbar с кнопкой "зарегистрироваться", иначе - toast
     */
    public static void showError(final Activity activity,
                                 int rootViewResId,
                                 final int loginRequestCode,
                                 @Nullable Throwable exception,
                                 int fallbackResId) {
        showError(activity,
                activity.findViewById(rootViewResId),
                UiUtils.getUserErrorText(activity.getResources(), exception, fallbackResId),
                exception,
                loginRequestCode
                );
    }


    private static void showError(final Activity activity,
                                 View rootView,
                                 CharSequence error,
                                 @Nullable Throwable exception,
                                 final int loginRequestCode) {
        if (activity.isFinishing()) return;
        if (exception instanceof ApiErrorException
                && (((ApiErrorException) exception)).isErrorAuthorizationRequired()
                && !Session.getInstance().isAuthorized()) {
            Snackbar.make(rootView, R.string.allowed_only_to_registered_users, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_sign_up, v -> {
                        AnalyticsHelper.getInstance().sendFeedsEvent("Открытие логина из сообщения ошибки");
                        LoginActivity.startActivity(activity, loginRequestCode, null);
                    })
                    .show();
        } else {
            if (BuildConfig.DEBUG) {
                MessageHelper.showError(activity, error.toString() + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
            } else {
                MessageHelper.showError(activity, error, exception);
            }
        }
    }

    /**
     * Показ ошибки Toast'ом
     * @param context
     * @param error
     * @param exception
     */
    public static void showError(Context context, CharSequence error, @Nullable Throwable exception) {
        FrenchToast.with(context).clear();
        Toast toast = createErrorToast(context, error);
        FrenchToast.with(context)
                .length(getHideDelay(error), TimeUnit.MILLISECONDS)
                .showDipped(toast);
        AnalyticsHelper.getInstance().sendErrorAnalyticsEvent(error, exception);
    }

    public static void showSuccess(Context context, CharSequence message) {
        Toast toast = createSuccessToast(context, message);
        FrenchToast.with(context).length(getHideDelay(message), TimeUnit.MILLISECONDS).showDipped(toast);
    }
}
