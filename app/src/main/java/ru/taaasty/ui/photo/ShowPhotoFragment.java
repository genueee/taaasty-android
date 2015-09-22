package ru.taaasty.ui.photo;


import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.ImageViewState;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.utils.ImageUtils;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ShowPhotoFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
// TODO prefiew
// TODO свыход по свайпу вверх
// TODO индикатор
public class ShowPhotoFragment extends Fragment {
    private static final String TAG = "ShowPhotoFragment";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String ARG_URL = "url";
    private static final String ARG_PREVIEW_URL = "preview_bitmap";

    private static final String BUNDLE_STATE = "ImageViewState";

    private String mUrl;
    private String mPreviewBitmap;
    private SubsamplingScaleImageView mPhotoView;

    private OnFragmentInteractionListener mListener;

    private SensorManager mSensorManager;
    @Nullable
    private Sensor mSensor;
    private SensorsHandler mSensorHandler;

    private boolean mPreviewLoaded = false;

    private Target mTarget;

    private GestureDetector mGestureDetector;

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
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnFragmentInteractionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
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
        View root = inflater.inflate(R.layout.fragment_show_photo_item, container, false);
        mPhotoView = (SubsamplingScaleImageView)root.findViewById(R.id.image_view);

        mGestureDetector = createGestureDetector();
        mPhotoView.setMinimumDpi(160/2);
        mPhotoView.setDebug(DBG);
        mPhotoView.setDoubleTapZoomDpi(160 / 2);
        mPhotoView.setOnImageEventListener(new SubsamplingScaleImageView.OnImageEventListener() {
            @Override
            public void onReady() {
                if (mListener != null) mListener.onBitmapLoaded();
            }

            @Override
            public void onImageLoaded() {

            }

            @Override
            public void onPreviewLoadError(Exception e) {
                if (DBG) Log.v(TAG, "onPreviewLoadError", e);
            }

            @Override
            public void onImageLoadError(Exception e) {
                if (DBG) Log.v(TAG, "onImageLoadError", e);
                if (getView() != null) {
                    getView().findViewById(R.id.progressView).setVisibility(View.GONE);
                }
                if (mPreviewLoaded) {
                    if (mListener != null) mListener.onBitmapLoaded();
                } else {
                    if (mListener != null) mListener.onLoadBitmapFailed();
                }
            }

            @Override
            public void onTileLoadError(Exception e) {
                if (DBG) Log.v(TAG, "onTileLoadError", e);
            }
        });

        mPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) mListener.onPhotoClicked();
            }
        });
        mPhotoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // В любой непонятной ситуации останавливаем скролл
                if (mSensorHandler != null) mSensorHandler.stopScroll();
                // return mGestureDetector.onTouchEvent(event);
                return false;
            }
        });

        mSensorHandler = new SensorsHandler(getActivity(), mPhotoView);
        mSensorManager = (SensorManager)getActivity().getSystemService(Activity.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (mSensor == null) Log.i(TAG, "no ROTATION VECTOR sensor");

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Picasso picasso = Picasso.with(getActivity());

        /*
        if (mPreviewBitmap != null) {
            picasso.load(mPreviewBitmap)
                   .into(mPicassoPreviewTarget);
        }
        */

        ImageViewState imageViewState ;
        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_STATE)) {
            imageViewState = (ImageViewState)savedInstanceState.getSerializable(BUNDLE_STATE);
        } else {
            imageViewState = null;
        }

        mTarget = new PicassoTarget(imageViewState);
        picasso.load(mUrl).skipMemoryCache().into(mTarget);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPhotoView != null) {
            ImageViewState state = mPhotoView.getState();
            if (state != null) {
                outState.putSerializable(BUNDLE_STATE, mPhotoView.getState());
            }
        }
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
        mTarget = null;
        mSensorHandler.stopScroll();
        mSensorHandler.onDestroy();
        mSensorHandler = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPreviewBitmap = null;
    }

    /*
    private void recreateProtoAttacher() {
        boolean changeAttacher = false;
        float lastScale = 0;
        if (mPhotoViewAttacher != null) {
            changeAttacher = true;
            lastScale = mPhotoViewAttacher.getScale();
            mPhotoViewAttacher.cleanup();
        }
        mPhotoViewAttacher = new OutPhotoViewAttacher(mPhotoView);
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
    */


    /**
     * MyGestureDetector с перехватом свайпа вверх
     */
    private GestureDetector createGestureDetector() {
        Context context = getContext();
        final float velYThreshold;
        final float minDistance;
        float density = context.getResources().getDisplayMetrics().density;
        velYThreshold = 3000 * density;
        minDistance = 20 * density;
        GestureDetector detector = new GestureDetector(context, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (e1 == null || e2 == null) return false; // Какого-то хрена на android > 4 оно иногда срабатывает
                    if (e1.getY() - e2.getY() < minDistance) return false;
                    /*
                    if (getDisplayRect().bottom > mPhotoView.getHeight()) {
                        // Низ изображения ниже нижней границы mPhotoView
                        return false;
                    }
                    */
                    getActivity().finish();
                    return false;
                }
        });
        detector.setIsLongpressEnabled(false);

        return detector;
    }

    private final Target mPicassoPreviewTarget = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if (mPhotoView == null) return;
            mPhotoView.setImage(ImageSource.cachedBitmap(bitmap).tilingDisabled());
            //recreateProtoAttacher();
            mPreviewLoaded = true;
        }

        @Override
        public void onBitmapFailed(Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    private class PicassoTarget implements Target {

        @Nullable
        private final ImageViewState mSavedState;

        public PicassoTarget(@Nullable ImageViewState savedState) {
            mSavedState = savedState;
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
            Picasso.with(getActivity()).cancelRequest(mPicassoPreviewTarget);
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.GONE);
            if (mPhotoView == null) return;

            int maxTextureSize = ImageUtils.getInstance().getMaxTextureSize();
            if (bitmap.getHeight() >= maxTextureSize || bitmap.getWidth() > maxTextureSize) {
                mPhotoView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            
            mPhotoView.setImage(ImageSource.cachedBitmap(bitmap).tilingEnabled(), mSavedState);
            //recreateProtoAttacher();
        }

        @Override
        public void onBitmapFailed(Drawable drawable) {
            Picasso.with(getActivity()).cancelRequest(mPicassoPreviewTarget);
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
        public void onPrepareLoad(Drawable drawable) {
            View v = getView();
            if (v == null) return;
            v.findViewById(R.id.progressView).setVisibility(View.VISIBLE);
        }
    }

    public interface OnFragmentInteractionListener {
        void onBitmapLoaded();
        void onPhotoClicked();
        void onLoadBitmapFailed();
        void onMatrixChanged(int photoViewWidth, RectF imageRect);
    }

}
