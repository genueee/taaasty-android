package ru.taaasty.ui.login;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.text.InputFilter;
import android.text.LoginFilter;
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
import ru.taaasty.model.RegisterUserResponse;
import ru.taaasty.service.Users;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.SlugTextInputFilter;
import ru.taaasty.utils.UserEmailLoader;
import ru.taaasty.widgets.ErrorTextView;
import rx.Observable;
import rx.Observer;
import rx.android.observables.AndroidObservable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;

public class SignUpFragment extends Fragment {

    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "SignViaEmailFragment";

    private OnFragmentInteractionListener mListener;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mSlugView;
    private View mProgressView;
    private View mSignUpButtonView;
    private ErrorTextView mErrorView;

    private Observable<RegisterUserResponse> mAuthTask;

    public static SignUpFragment newInstance() {
        return new SignUpFragment();
    }
    public SignUpFragment() {
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
        View root = inflater.inflate(R.layout.fragment_sign_up, container, false);

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) root.findViewById(R.id.email);
        mPasswordView = (EditText) root.findViewById(R.id.password);
        mSlugView = (EditText)root.findViewById(R.id.slug);
        mErrorView = (ErrorTextView)root.findViewById(R.id.error_text);
        mSignUpButtonView = root.findViewById(R.id.sign_up_button);
        mProgressView = root.findViewById(R.id.login_progress);

        mSlugView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.slug || id == EditorInfo.IME_NULL) {
                    attemptSignUp();
                    return true;
                }
                return false;
            }
        });
        mPasswordView.setFilters(new InputFilter[] {
                new LoginFilter.PasswordFilterGMail()
        });
        mSlugView.setFilters(new InputFilter[] {
                new SlugTextInputFilter()
        });

        mSignUpButtonView.setOnClickListener(mOnClickListener);

        root.findViewById(R.id.back_button).setOnClickListener(mOnClickListener);
        root.findViewById(R.id.button_i_have_registered).setOnClickListener(mOnClickListener);

        ((TextView)root.findViewById(R.id.window_title)).setText(R.string.title_fragment_sign_up);

        mEmailView.requestFocus();

        return root;
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.sign_up_button:
                    attemptSignUp();
                    break;
                case R.id.back_button:
                    if (mListener != null) mListener.onSignUpBackPressed();
                    break;
                case R.id.button_i_have_registered:
                    if (mListener != null) mListener.onIHaveRegisteredPressed();
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

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptSignUp() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mErrorView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String slug = mSlugView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mErrorView.setError(getString(R.string.error_email_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mErrorView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mErrorView.setError(getString(R.string.error_password_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mErrorView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check slug
        /*
        if (TextUtils.isEmpty(slug)) {
            mErrorView.setError(getString(R.string.error_slug_field_required));
            focusView = mSlugView;
            cancel = true;
        } else if (!isSlugValid(slug)) {
            mErrorView.setError(getString(R.string.error_invalid_slug));
            focusView = mSlugView;
            cancel = true;
        }
        */

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            Users service = NetworkUtils.getInstance().createRestAdapter().create(Users.class);
            mAuthTask = service.regiserUser(email, password, slug);
            AndroidObservable.bindFragment(this, mAuthTask);
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
                    .subscribe(new Observer<RegisterUserResponse>() {
                        @Override
                        public void onCompleted() {
                            if (DBG) Log.v(TAG, "onCompleted()");
                            if (mListener != null) mListener.onSignUpSuccess();
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (DBG) Log.e(TAG, "onError", e);
                            mErrorView.setError(getString(R.string.error_invalid_email_or_password));
                            mPasswordView.requestFocus();
                        }

                        @Override
                        public void onNext(RegisterUserResponse currentUser) {
                            if (DBG) Log.e(TAG, "onNext " + currentUser.toString());
                            // XXX
                            // UserManager.getInstance().setCurrentUser(currentUser);
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

    private boolean isSlugValid(String slug) {
        return slug.length() >= 3;
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

        mSignUpButtonView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
        mSignUpButtonView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mSignUpButtonView.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
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
    public interface OnFragmentInteractionListener {
        public void onSignUpSuccess();
        public void onSignUpBackPressed();
        public void onIHaveRegisteredPressed();
    }

}
