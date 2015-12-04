package ru.taaasty.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import ru.taaasty.R;

public class AppUpdateAvailableDialogFragment extends DialogFragment {

    public interface OnFragmentInteractionListener {
        void onUpdateAppClicked();
        void onUpdateAvailableMessageDismissed();
    }

    public static AppUpdateAvailableDialogFragment newInstance(String message, boolean showDoUpdateButton) {
        AppUpdateAvailableDialogFragment frag = new AppUpdateAvailableDialogFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        args.putBoolean("showDoUpdateButton", showDoUpdateButton);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String message = getArguments().getString("message");
        boolean doShowUpdate = getArguments().getBoolean("showDoUpdateButton");

        AlertDialog.Builder builder =  new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        }
                );
        if (doShowUpdate) {
            builder.setPositiveButton(R.string.update_app, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ((OnFragmentInteractionListener)getActivity()).onUpdateAppClicked();
                }
            });
        }

        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (getActivity() != null) {
            ((OnFragmentInteractionListener)getActivity()).onUpdateAvailableMessageDismissed();
        }
    }
}
