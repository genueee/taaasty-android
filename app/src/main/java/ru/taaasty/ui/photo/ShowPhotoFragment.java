package ru.taaasty.ui.photo;


import android.app.Activity;
import android.app.Fragment;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import ru.taaasty.BuildConfig;
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
    private static final String TAG = "ShowPhotoFragment";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String ARG_URL = "url";
    private static final String ARG_PREVIEW_URL = "preview_bitmap";

    private String mUrl;
    private String mPreviewBitmap;
    private PhotoView mPhotoView;

    private PhotoViewAttacher mPhotoViewAttacher;
    private OnFragmentInteractionListener mListener;

    private SensorManager mSensorManager;
    @Nullable
    private Sensor mSensor;
    private SensorsHandler mSensorHandler;

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
        mSensorHandler = new SensorsHandler(getActivity()) {
            @Override
            public PhotoViewAttacher getPhotoAttacher() {
                return mPhotoViewAttacher;
            }
        };
        mSensorManager = (SensorManager)getActivity().getSystemService(Activity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (mSensor == null) Log.i(TAG, "no ROTATION VECTOR sensor");
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
    public void onResume() {
        super.onResume();
        if (mSensor != null) mSensorManager.registerListener(mSensorHandler, mSensor, SensorManager.SENSOR_DELAY_UI);
        mSensorHandler.onResume();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mSensorHandler.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSensor != null) mSensorManager.unregisterListener(mSensorHandler);
        mSensorHandler.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPhotoView = null;
        mSensorHandler.stopScroll();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPreviewBitmap = null;
        if (mPhotoViewAttacher != null) {
            mPhotoViewAttacher.cleanup();
        }
        mSensorHandler.onDestroy();
        mSensorHandler = null;
    }

    private void recreateProtoAttacher() {
        boolean changeAttacher = false;
        float lastScale = 0;
        if (mPhotoViewAttacher != null) {
            changeAttacher = true;
            lastScale = mPhotoViewAttacher.getScale();
            mPhotoViewAttacher.cleanup();
        }
        mPhotoViewAttacher = new OutPhotoViewAttacher(mPhotoView);
        mPhotoViewAttacher.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mPhotoViewAttacher.setOnPhotoTapListener(mOnTapListener);
        if (changeAttacher) {
            mPhotoViewAttacher.setScale(lastScale);
        }
        mPhotoViewAttacher.setOnMatrixChangeListener(new PhotoViewAttacher.OnMatrixChangedListener() {
            @Override
            public void onMatrixChanged(RectF rect) {
                if (mListener != null) mListener.onMatrixChanged(mPhotoView.getWidth(), rect);
            }
        });
    }

    /**
     * PhotoViewAttacher с перехватом свайпа вверх
     */
    private final class OutPhotoViewAttacher extends PhotoViewAttacher {

        final float velYThreshold;
        final float minDistance;

        final GestureDetector mGestureDetector;

        public OutPhotoViewAttacher(ImageView imageView) {
            super(imageView);
            float density = getResources().getDisplayMetrics().density;
            velYThreshold = 3000 * density;
            minDistance = 20 * density;

            if (DBG) Log.v(TAG, "velYThreshold: " + velYThreshold);
            mGestureDetector = new GestureDetector(imageView.getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false; // Какого-то хрена на android > 4 оно иногда срабатывает
                    if (e1.getY() - e2.getY() < minDistance) return false;
                    // if (Math.abs(velocityY) < velYThreshold) return false;
                    if (getDisplayRect().bottom > mPhotoView.getHeight()) {
                        // Низ изображения ниже нижней границы mPhotoView
                        return false;
                    }
                    getActivity().finish();
                    return false;
                }
            });
            mGestureDetector.setIsLongpressEnabled(false);
        }

        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            // В любой непонятной ситуации останавливаем скролл
            if (mSensorHandler != null) mSensorHandler.stopScroll();
            if (mGestureDetector.onTouchEvent(ev)) return true;
            return super.onTouch(v, ev);
        }
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
        public void onMatrixChanged(int photoViewWidth, RectF imageRect);
    }

}
