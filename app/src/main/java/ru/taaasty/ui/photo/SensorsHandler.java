package ru.taaasty.ui.photo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Surface;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import ru.taaasty.BuildConfig;
import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Created by alexey on 05.10.14.
 */
public abstract class SensorsHandler implements SensorEventListener {
    private static final String TAG = "SensorsHandler";
    private static final boolean DBG = BuildConfig.DEBUG;

    private static final long ANIMATION_SPEED = 100; /* mm/s */
    private static final float PITCH_THRESHOLD = (float)Math.toRadians(12d);

    private final Activity mActivity;
    private float mLastRoll;
    private final int mAnimationSpeed;
    private int mRotation;

    private final float[] mAzumuthPitchRoll = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mMappedMatrix = new float[9];

    @Nullable
    private Animator mAnimator;

    public SensorsHandler(Activity activity) {
        mActivity = activity;
        mAnimationSpeed = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, ANIMATION_SPEED, activity.getResources().getDisplayMetrics());
    }

    public void onResume() {
        mLastRoll = 0;
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        stopScroll();
        mRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        if (DBG) Log.v(TAG, "Rotation: " + mRotation + " " + (mRotation == Surface.ROTATION_0 ? "ROTATION_0" :
                mRotation == Surface.ROTATION_90 ? "ROTATION_90" :
                        mRotation == Surface.ROTATION_180 ? "ROTATION_180" : "ROTATION_270"
        ));
    }

    public void onPause() {
        stopScroll();
    }

    public void onDestroy() {
        stopScroll();
    }

    public abstract PhotoViewAttacher getPhotoAttacher();

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            // convert the rotation-vector to a 4x4 matrix. the matrix
            // is interpreted by Open GL as the inverse of the
            // rotation-vector, which is what we want.
            SensorManager.getRotationMatrixFromVector(
                    mRotationMatrix, event.values);

            switch (mRotation) {
                case Surface.ROTATION_0:
                    System.arraycopy(mRotationMatrix, 0, mMappedMatrix, 0, mRotationMatrix.length);
                    break;
                case Surface.ROTATION_90:
                    SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Y,
                            SensorManager.AXIS_MINUS_X,mMappedMatrix);
                    break;
                case Surface.ROTATION_180:
                    SensorManager.remapCoordinateSystem(mRotationMatrix,
                            SensorManager.AXIS_MINUS_X,
                            SensorManager.AXIS_MINUS_Y,
                            mMappedMatrix);
                    break;
                case Surface.ROTATION_270:
                    SensorManager.remapCoordinateSystem(mRotationMatrix,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X, mMappedMatrix);
                    break;
                default:
                    throw new IllegalStateException();
            }

            SensorManager.getOrientation(mMappedMatrix, mAzumuthPitchRoll);

            float roll = mAzumuthPitchRoll[2];
            int currentScrollStatus = getScrollingStatus(mLastRoll);
            int newScrollStatus = getScrollingStatus(roll);
            if (currentScrollStatus != newScrollStatus) {
                mLastRoll = roll;
                if (newScrollStatus > 0) {
                    startScrollLeft();
                } else if (newScrollStatus == 0) {
                    stopScroll();
                } else {
                    startScrollRight();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    /**
     * > 0 - железяка нклонена вправо
     * = 0 - железяка не наклонена
     * < 0 - жедезяка наклонена влево
     * @param val угол поворота устройства
     * @return
     */
    private int getScrollingStatus(float val) {
        if (val <= -PITCH_THRESHOLD) {
            return -1;
        } else if (val >= PITCH_THRESHOLD) {
            return 1;
        } else {
            return 0;
        }
    }

    private void startScrollLeft() {
        final PhotoViewAttacher viewAttacher;
        ImageView imageView;
        int viewLeft;
        int imageLeft;
        long duration;
        final Matrix matrix;
        ValueAnimator va;

        if (DBG) Log.v(TAG, "Scroll left");
        stopScroll();
        viewAttacher = getPhotoAttacher();
        if (viewAttacher == null || viewAttacher.getImageView() == null || viewAttacher.getDisplayRect() == null) return;
        imageView = viewAttacher.getImageView();
        viewLeft = 0;
        imageLeft = (int)viewAttacher.getDisplayRect().left;
        if (viewLeft <= imageLeft) {
            return;
        }

        duration = 1000 * (viewLeft - imageLeft) / mAnimationSpeed;
        if (duration <= 16) duration = 16;

        va = ValueAnimator.ofInt(0, viewLeft - imageLeft);
        va.setInterpolator(new DecelerateInterpolator());
        va.setDuration(duration);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int lastval= 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int newVal = (Integer)animation.getAnimatedValue();
                int dx = newVal - lastval;
                lastval = newVal;
                PhotoViewAttacher pa = getPhotoAttacher();
                if (pa == null || pa.getImageView() == null) return;
                pa.onDrag(dx, 0);
            }
        });
        mAnimator = va;
        mAnimator.start();
    }

    private void startScrollRight() {
        if (DBG) Log.v(TAG, "Scroll right");
        stopScroll();
        final PhotoViewAttacher viewAttacher;
        ImageView imageView;
        int viewRight;
        int imageRight;
        long duration;
        final Matrix matrix;
        ValueAnimator va;

        if (DBG) Log.v(TAG, "Scroll left");
        stopScroll();
        viewAttacher = getPhotoAttacher();
        if (viewAttacher == null) return;
        imageView = viewAttacher.getImageView();
        viewRight = imageView.getWidth();
        imageRight = (int)viewAttacher.getDisplayRect().right;
        if (viewRight >= imageRight) {
            return;
        }

        duration = 1000 * (imageRight - viewRight) / mAnimationSpeed;
        if (duration <= 16) duration = 16;

        va = ValueAnimator.ofInt(0, viewRight - imageRight);
        va.setInterpolator(new DecelerateInterpolator());
        va.setDuration(duration);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            int lastval= 0;

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int newVal = (Integer)animation.getAnimatedValue();
                int dx = newVal - lastval;
                lastval = newVal;
                PhotoViewAttacher pa = getPhotoAttacher();
                if (pa == null || pa.getImageView() == null) return;
                pa.onDrag(dx, 0);
            }
        });
        mAnimator = va;
        mAnimator.start();
    }

    public void stopScroll() {
        if (DBG) Log.v(TAG, "Scroll stop");
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
    }

}
