package ru.taaasty;

import android.app.Application;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

@ReportsCrashes(
        formKey = "",
        formUri = BuildConfig.ACRA_FORM_URI,
        reportType = org.acra.sender.HttpSender.Type.JSON,
        httpMethod = org.acra.sender.HttpSender.Method.PUT,
        formUriBasicAuthLogin = BuildConfig.ACRA_FORM_URI_BASIC_AUTH_LOGIN,
        formUriBasicAuthPassword = BuildConfig.ACRA_FORM_URI_BASIC_AUTH_PASSWORD,
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = false,
        resToastText = R.string.crash_toast_text
)
public class TaaastyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ACRA мешает при отладке
        if (!BuildConfig.ENABLE_ACRA) ACRA.init(this);

    }
}
