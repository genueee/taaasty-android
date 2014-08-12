package ru.taaasty.ui.photo;



import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.R;
import ru.taaasty.utils.NetworkUtils;
import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShowPhotoFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ShowPhotoFragment extends Fragment {
    private static final String ARG_URL = "url";

    private String mUrl;

    private PhotoViewAttacher mPhotoViewAttacher;
    private OnFragmentInteractionListener mListener;

    public static ShowPhotoFragment newInstance(String url) {
        ShowPhotoFragment fragment = new ShowPhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }
    public ShowPhotoFragment() {
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
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mUrl = getArguments().getString(ARG_URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_photo_item, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Picasso picasso = NetworkUtils.getInstance().getPicasso(getActivity());
        picasso.load(mUrl).into(mPicassoTarget);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
    }

    private final Target mPicassoTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.GONE);
            PhotoView pv = (PhotoView) v.findViewById(R.id.picturePhotoView);
            pv.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            mPhotoViewAttacher = new PhotoViewAttacher(pv);
            mPhotoViewAttacher.setOnPhotoTapListener(mOnTapListener);
            if (mListener != null) mListener.onBitmapLoaded();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.GONE);
            if (mListener != null) mListener.onLoadBitmapFailed();
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.VISIBLE);
        }
    };


    private final PhotoViewAttacher.OnPhotoTapListener mOnTapListener = new PhotoViewAttacher.OnPhotoTapListener() {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            if (mListener != null) mListener.onPhotoTap();
        }
    };

    public interface OnFragmentInteractionListener {
        public void onBitmapLoaded();
        public void onPhotoTap();
        public void onLoadBitmapFailed();
    }

}
