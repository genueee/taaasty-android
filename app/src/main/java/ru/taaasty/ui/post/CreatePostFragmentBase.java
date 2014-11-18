package ru.taaasty.ui.post;

import android.app.Activity;
import android.app.Fragment;
import android.util.Log;

import ru.taaasty.BuildConfig;
import ru.taaasty.model.PostForm;

public abstract class CreatePostFragmentBase extends Fragment {
    private static final String TAG = "CreatePostFragmentBase";
    private static final boolean DBG = BuildConfig.DEBUG;

    OnCreatePostInteractionListener mListener;

    boolean mFormValid = false;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCreatePostInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        validateFormIfVisible();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (DBG) Log.d(TAG, "setUserVisibleHint: " + isVisibleToUser);
        if (isVisibleToUser && isAdded()) validateForm(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public abstract PostForm getForm();

    void validateFormIfVisible() {
        if (getUserVisibleHint()) validateForm();
    }

    void validateForm() { validateForm(false); }

    void validateForm(boolean forceCallListener) {
        boolean formValid = false;
        boolean statusChanged;
        formValid = isFormValid();
        statusChanged = formValid != mFormValid;
        mFormValid = formValid;
        if ((statusChanged || forceCallListener) && mListener != null) {
            mListener.onValidationStatusChanged(mFormValid);
        }
    }

    public abstract boolean isFormValid();

}
