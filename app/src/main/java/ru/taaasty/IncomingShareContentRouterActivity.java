package ru.taaasty;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import ru.taaasty.ui.messages.CreateMessageActivity;
import ru.taaasty.ui.post.CreateSharedPostActivity;

public class IncomingShareContentRouterActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getFragmentManager().findFragmentByTag("dialog") == null) {
            ChooseDialogFragment.create(getIntent()).show(getFragmentManager(), "dialog");
        }
    }

    public static class ChooseDialogFragment extends DialogFragment {

        public static final String INTENT_ARGUMENT = "INTENT_ARGUMENT";

        public static ChooseDialogFragment create(Intent intent) {
            ChooseDialogFragment fragment = new ChooseDialogFragment();
            Bundle bundle = new Bundle(1);
            bundle.putParcelable(INTENT_ARGUMENT, intent);
            fragment.setArguments(bundle);
            return fragment;
        }

        @Override
        public android.app.Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setItems(
                            R.array.routes_for_incoming_share,
                            (dialog, which) -> {
                                switch (which) {
                                    case 0:
                                        Intent oldIntent = getArguments().getParcelable(INTENT_ARGUMENT);
                                        Intent intent = new Intent();
                                        intent.setAction(Intent.ACTION_SEND);
                                        intent.setType(oldIntent.getType());
                                        intent.setClass(getActivity(), CreateSharedPostActivity.class);
                                        intent.putExtras(oldIntent.getExtras());
                                        startActivity(intent);
                                        getActivity().finish();
                                        break;
                                    case 1:
                                        CreateMessageActivity.startActivity(getActivity());
                                        getActivity().finish();
                                }
                            }
                    )
                    .create();
        }
    }
}
