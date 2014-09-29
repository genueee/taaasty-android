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
import ru.taaasty.utils.ImageUtils;
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
    private static final String ARG_PREVIEW_URL = "preview_bitmap";

    private String mUrl;
    private String mPreviewBitmap;
    private PhotoView mPhotoView;

    private PhotoViewAttacher mPhotoViewAttacher;
    private OnFragmentInteractionListener mListener;

    private boolean mPreviewLoaded = false;

    public static ShowPhotoFragment newInstance(String url, String preview) {
        ShowPhotoFragment fragment = new ShowPhotoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_PREVIEW_URL, preview);
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
            mPreviewBitmap = getArguments().getString(ARG_PREVIEW_URL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root =  inflater.inflate(R.layout.fragment_show_photo_item, container, false);
        mPhotoView = (PhotoView)root.findViewById(R.id.picturePhotoView);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Picasso picasso = NetworkUtils.getInstance().getPicasso(getActivity());

        if (mPreviewBitmap != null) {
            picasso.load(mPreviewBitmap)
                   .into(mPicassoPreviewTarget);
        }
        picasso.load(mUrl).skipMemoryCache().into(mPicassoTarget);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPhotoView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPreviewBitmap = null;
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
    }

    private void recreateProtoAttacher() {
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
        mPhotoViewAttacher = new PhotoViewAttacher(mPhotoView);
        mPhotoViewAttacher.setOnPhotoTapListener(mOnTapListener);
    }

    private final Target mPicassoPreviewTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (mPhotoView == null) return;
            mPhotoView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            recreateProtoAttacher();
            mPreviewLoaded = true;
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    private final Target mPicassoTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            NetworkUtils.getInstance().getPicasso(getActivity()).cancelRequest(mPicassoPreviewTarget);
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.GONE);
            if (mPhotoView == null) return;
            int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
            if (bitmap.getHeight() >= maxTextureSize || bitmap.getWidth() > maxTextureSize) {
                mPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            mPhotoView.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
            recreateProtoAttacher();
            if (mListener != null) mListener.onBitmapLoaded();
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {
            NetworkUtils.getInstance().getPicasso(getActivity()).cancelRequest(mPicassoPreviewTarget);
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.GONE);
            if (mPreviewLoaded) {
                if (mListener != null) mListener.onBitmapLoaded();
            } else {
                if (mListener != null) mListener.onLoadBitmapFailed();
            }
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
