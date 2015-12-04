package ru.taaasty.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.TaaastyApplication;
import ru.taaasty.utils.CheckAppHelper;

/**
 * Created by alexey on 04.12.15.
 */
public class AppUnsupportedDialogActivity extends AppCompatActivity {

    public static final String ARG_MESSAGE = "ru.taaasty.ui.ARG_MESSAGE";

    public static void startActivity(Activity activity, String message) {
        Intent intent = new Intent(activity.getApplicationContext(), AppUnsupportedDialogActivity.class);
        intent.putExtra(AppUnsupportedDialogActivity.ARG_MESSAGE, message);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        activity.startActivity(intent);
        activity.finish();
        activity.overridePendingTransition(0, 0);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String message = getIntent().getStringExtra(ARG_MESSAGE);
        if (TextUtils.isEmpty(message)) {
            message = getString(R.string.app_version_not_supported);
        }

        if (savedInstanceState == null) {
            AppUnsupportedDialogFragment fragment = AppUnsupportedDialogFragment.newInstance(message);
            fragment.show(getSupportFragmentManager(), null);
        }
    }


    public static class AppUnsupportedDialogFragment extends DialogFragment {

        public static AppUnsupportedDialogFragment newInstance(String message) {
            AppUnsupportedDialogFragment frag = new AppUnsupportedDialogFragment();
            Bundle args = new Bundle();
            args.putString("message", message);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String message = getArguments().getString("message");

            AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setPositiveButton(R.string.update_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CheckAppHelper.openUpdateAppLink(getActivity());
                        }
                    });


            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            if (getActivity() != null) {
                ((TaaastyApplication)getActivity().getApplicationContext()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_APP_UPDATE,
                        "сообщение отклонено", BuildConfig.VERSION_NAME);
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            if (getActivity() != null) getActivity().finish();
        }
    }

}
