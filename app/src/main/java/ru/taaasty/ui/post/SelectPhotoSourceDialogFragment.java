package ru.taaasty.ui.post;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import ru.taaasty.R;

/**
* Created by alexey on 05.09.14.
*/
public class SelectPhotoSourceDialogFragment extends DialogFragment {

    private boolean mHasPicture;

    private final static String ARG_HAS_PICTURE = "has_picture";

    public static SelectPhotoSourceDialogFragment createInstance(boolean hasPicture) {
        SelectPhotoSourceDialogFragment fragment = new SelectPhotoSourceDialogFragment();
        Bundle arguments = new Bundle(1);
        arguments.putBoolean(ARG_HAS_PICTURE, hasPicture);
        fragment.setArguments(arguments);
        return fragment;
    }

    public SelectPhotoSourceDialogFragment() {
    }

    public interface SelectPhotoSourceDialogListener {
        public void onPickPhotoSelected();
        public void onMakePhotoSelected();
        public void onDeletePhotoSelected();
    }

    SelectPhotoSourceDialogListener mListener;

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (SelectPhotoSourceDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments != null) {
            mHasPicture = arguments.getBoolean(ARG_HAS_PICTURE);
        } else {
            mHasPicture = false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
            .setItems(mHasPicture ? R.array.photo_sources_array_with_image : R.array.photo_sources_array, mOnClickListener);
        return builder.create();
    }

    private final DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (mListener == null) return;
            switch (which) {
                case 0:
                    mListener.onPickPhotoSelected();
                    break;
                case 1:
                    mListener.onMakePhotoSelected();
                    break;
                case 2:
                    mListener.onDeletePhotoSelected();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    };
}
