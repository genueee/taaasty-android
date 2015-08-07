package ru.taaasty.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.RecoveryPasswordResponse;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.UiUtils;
import ru.taaasty.utils.UserEmailLoader;
import rx.Observable;
import rx.Observer;
import rx.android.app.AppObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class RecoverPasswordFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "ForgotPasswordFragment";

    private OnFragmentInteractionListener mListener;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private View mProgressView;
    private View mSendPassword;

    private Observable<RecoveryPasswordResponse> mAuthTask;

    public static RecoverPasswordFragment newInstance() {
        return new RecoverPasswordFragment();
    }
    public RecoverPasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_restore_password, container, false);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) root.findViewById(R.id.email);

        mSendPassword = root.findViewById(R.id.send_password);
        mSendPassword.setOnClickListener(mOnClickListener);

        root.findViewById(R.id.back_button).setOnClickListener(mOnClickListener);

        mProgressView = root.findViewById(R.id.login_progress);

        ((TextView)root.findViewById(R.id.window_title)).setText(R.string.title_fragment_recover_password);
        mEmailView.requestFocus();

        return root;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.send_password:
                    sendRestorePasswordRequest();
                    break;
                case R.id.back_button:
                    if (mListener != null) mListener.onForgotPasswordBackPressed();
                    break;
            }
        }
    };

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, mEmailLoader);

        mEmailView.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                if (imm != null)
                    imm.showSoftInput(mEmailView, InputMethodManager.SHOW_IMPLICIT);
            }
        });

    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void sendRestorePasswordRequest() {
        if (mAuthTask != null) {
            return;
        }

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_email_field_required), null);
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_invalid_email), null);
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the author login attempt.
            showProgress(true);

            ApiUsers service = RestClient.getAPiUsers();
            mAuthTask = service.recoveryPassword(email);
            AppObservable.bindFragment(this, mAuthTask);
            mAuthTask
                    .observeOn(AndroidSchedulers.mainThread())
                    .finallyDo(new Action0() {
                        @Override
                        public void call() {
                            if (DBG) Log.v(TAG, "finallyDo()");
                            mAuthTask = null;
                            showProgress(false);
                        }
                    })
                    .subscribe(new Observer<RecoveryPasswordResponse>() {
                        @Override
                        public void onCompleted() {
                            if (DBG) Log.v(TAG, "onCompleted()");
                            if (mListener != null) mListener.onForgotPasswordRequestSent();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (DBG) Log.e(TAG, "onError", e);
                            if (mListener != null) mListener.notifyError(
                                    UiUtils.getUserErrorText(getResources(), e, R.string.error_invalid_email_or_password), e);
                            mEmailView.requestFocus();
                        }

                        @Override
                        public void onNext(RecoveryPasswordResponse success) {
                            if (DBG) Log.e(TAG, "onNext " + success.toString());
                        }
                    });
        }
    }
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.length() > 3;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mSendPassword.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        mSendPassword.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSendPassword.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    public final UserEmailLoader mEmailLoader = new UserEmailLoader(this) {
        @Override
        public void addEmailsToAutoComplete(List<String> emailAddressCollection) {
            //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
            ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(getActivity(),
                            android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

            mEmailView.setAdapter(adapter);
        }
    };

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener extends CustomErrorView {
        public void onForgotPasswordRequestSent();
        public void onForgotPasswordBackPressed();
    }

}
