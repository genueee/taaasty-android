package ru.taaasty.ui.login;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.taaasty.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SelectSignMethodFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 *
 */
public class SelectSignMethodFragment extends Fragment {

    private OnFragmentInteractionListener mListener;

    public static SelectSignMethodFragment newInstance() {
        return new SelectSignMethodFragment();
    }

    public SelectSignMethodFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_select_sign_method, container, false);

        root.findViewById(R.id.sign_via_vkontakte).setOnClickListener(mOnButtonClickListner);
        root.findViewById(R.id.sign_via_facebook).setOnClickListener(mOnButtonClickListner);
        root.findViewById(R.id.sign_via_email).setOnClickListener(mOnButtonClickListner);
        root.findViewById(R.id.register_button).setOnClickListener(mOnButtonClickListner);

        return root;
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    final View.OnClickListener mOnButtonClickListner = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mListener == null) return;
            switch (v.getId()) {
                case R.id.sign_via_vkontakte:
                    mListener.onSignViaVkontakteClicked();
                    break;
                case R.id.sign_via_facebook:
                    mListener.onSignViaFacebookClicked();
                    break;
                case R.id.sign_via_email:
                    mListener.onSignViaEmailClicked();
                    break;
                case R.id.register_button:
                    mListener.onRegisterClicked();
                    break;
                default:
                    throw new IllegalStateException();
            }
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
        void onSignViaVkontakteClicked();
        void onSignViaEmailClicked();
        void onSignViaFacebookClicked();
        void onRegisterClicked();
    }

}
