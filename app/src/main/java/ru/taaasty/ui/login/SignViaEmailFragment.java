package ru.taaasty.ui.login;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UserManager;
import ru.taaasty.model.CurrentUser;
import ru.taaasty.service.ApiSessions;
import ru.taaasty.ui.CustomErrorView;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UserEmailLoader;
import rx.Observable;
import rx.Observer;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class SignViaEmailFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SignViaEmailFragment";

    private OnFragmentInteractionListener mListener;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mSignInButtonView;

    private Observable<CurrentUser> mAuthTask;

    public static SignViaEmailFragment newInstance() {
        return new SignViaEmailFragment();
    }
    public SignViaEmailFragment() {
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
        View root = inflater.inflate(R.layout.fragment_sign_via_email, container, false);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) root.findViewById(R.id.email);
        mPasswordView = (EditText) root.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mSignInButtonView = root.findViewById(R.id.email_sign_in_button);
        mSignInButtonView.setOnClickListener(mOnClickListener);

        root.findViewById(R.id.back_button).setOnClickListener(mOnClickListener);
        root.findViewById(R.id.button_i_forgot_password).setOnClickListener(mOnClickListener);
        root.findViewById(R.id.button_i_have_not_registered).setOnClickListener(mOnClickListener);

        mProgressView = root.findViewById(R.id.login_progress);

        ((TextView)root.findViewById(R.id.window_title)).setText(R.string.title_fragment_sign_in);
        mEmailView.requestFocus();

        return root;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.email_sign_in_button:
                    attemptLogin();
                    break;
                case R.id.back_button:
                    if (mListener != null) mListener.onSignViaEmailBackPressed();
                    break;
                case R.id.button_i_forgot_password:
                    if (mListener != null) mListener.onIForgotPasswordPressed();
                    break;
                case R.id.button_i_have_not_registered:
                    if (mListener != null) mListener.onIHaveNotRegisteredPressed();
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
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        AnimatorSet a= null;
        if (nextAnim != 0) {
            Animator backgroundAnimator;
            if (enter) {
                backgroundAnimator = ((LoginActivity)getActivity()).createBlurInAnimator();
            } else {
                backgroundAnimator = ((LoginActivity)getActivity()).createBlurOutAnimator();
            }
            if (backgroundAnimator != null) {
                Animator old = AnimatorInflater.loadAnimator(getActivity(), nextAnim);
                a = new AnimatorSet();
                a.play(old).with(backgroundAnimator)
                ;
            }
        }
        if (DBG) Log.v(TAG, "onCreateAnimator. " + transit + " enter: " + enter + " nextAnim: " + nextAnim + " a: " + a + "aa: " + getActivity());
        return a;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the author entered one.
        if (TextUtils.isEmpty(password)) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_password_field_required), null);
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            if (mListener != null) mListener.notifyError(getText(R.string.error_invalid_password), null);
            focusView = mPasswordView;
            cancel = true;
        }

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
            ApiSessions service = NetworkUtils.getInstance().createRestAdapter().create(ApiSessions.class);
            mAuthTask = AndroidObservable.bindFragment(this, service.signIn(email, password));
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
                    .subscribe(new Observer<CurrentUser>() {
                        @Override
                        public void onCompleted() {
                            if (DBG) Log.v(TAG, "onCompleted()");
                            if (mListener != null) mListener.onSignSuccess();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (DBG) Log.e(TAG, "onError", e);
                            if (mListener != null) mListener.notifyError(getText(R.string.error_invalid_email_or_password), null);
                            mPasswordView.requestFocus();
                        }

                        @Override
                        public void onNext(CurrentUser currentUser) {
                            if (DBG) Log.e(TAG, "onNext " + currentUser.toString());
                            UserManager.getInstance().setCurrentUser(currentUser);
                        }
                    });
        }
    }
    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.length() > 3;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 3;
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

        mSignInButtonView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        mSignInButtonView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSignInButtonView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
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
        public void onSignSuccess();
        public void onSignViaEmailBackPressed();
        public void onIForgotPasswordPressed();
        public void onIHaveNotRegisteredPressed();
    }

}
