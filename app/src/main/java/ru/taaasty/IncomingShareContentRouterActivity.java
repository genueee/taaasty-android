package ru.taaasty;

import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import ru.taaasty.ui.messages.CreateMessageActivity;
import ru.taaasty.ui.post.CreateSharedPostActivity;

public class IncomingShareContentRouterActivity extends ActivityBase implements DialogInterface.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getFragmentManager().findFragmentByTag("dialog") == null) {
            new ChooseDialogFragment().show(getFragmentManager(), "dialog");
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0:

                Intent newIntent = new Intent();
                newIntent.setAction(Intent.ACTION_SEND);
                newIntent.setType(getIntent().getType());
                newIntent.setClass(this, CreateSharedPostActivity.class);
                newIntent.putExtras(getIntent().getExtras());
                startActivity(newIntent);

                break;
            case 1:
                Uri imageUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
                if (imageUri != null) {
                    newIntent = new Intent();
                    newIntent.setAction(Intent.ACTION_SEND);
                    newIntent.setType(getIntent().getType());
                    newIntent.setClass(this, CreateMessageActivity.class);
                    newIntent.putExtras(getIntent().getExtras());
                    startActivity(newIntent);
                }
                break;
            default:

        }
        finish();
    }

    public static class ChooseDialogFragment extends DialogFragment {
        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())

                    .setItems(
                            R.array.routes_for_incoming_share,
                            (dialog, which) -> ((DialogInterface.OnClickListener) getActivity()).onClick(dialog, which)
                    )
                    .create();
            alertDialog.setCanceledOnTouchOutside(false);
            return alertDialog;
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            getActivity().finish();
        }
    }
}
