package ru.taaasty.utils;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.concurrent.TimeUnit;

import frenchtoast.FrenchToast;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;

/**
 * Created by alexey on 02.11.15.
 */
public class MessageHelper {
    private static final int HIDE_DELAY_MIN_MS = 5000;
    private static final int HIDE_DELAY_MS_PER_CHARACTER = 200;

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

    public static void showError(Context context, CharSequence error, @Nullable Throwable exception) {
        Toast toast = createErrorToast(context, error);
        FrenchToast.with(context).length(getHideDelay(error), TimeUnit.MILLISECONDS).showDipped(toast);
        sendErrorAnalyticsEvent(context, error, exception);
    }

    public static void showSuccess(Context context, CharSequence message) {
        Toast toast = createSuccessToast(context, message);
        FrenchToast.with(context).length(getHideDelay(message), TimeUnit.MILLISECONDS).showDipped(toast);
    }

    public static void sendErrorAnalyticsEvent(Context context, CharSequence error, @Nullable Throwable exception) {
        if (exception == null) return;
        if (context.getApplicationContext() instanceof TaaastyApplication) {
            Tracker t = ((TaaastyApplication) context.getApplicationContext()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                            .setDescription(
                                    new AnalyticsExceptionParser(context, null)
                                            .getDescription(Thread.currentThread().getName(), exception))
                            .setFatal(false)
                            .build()
            );
        }
    }
}
