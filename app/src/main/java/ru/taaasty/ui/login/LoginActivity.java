package ru.taaasty.ui.login;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.util.Log;
import android.util.TimingLogger;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import ru.taaasty.BuildConfig;
import ru.taaasty.ui.MainActivity;
import ru.taaasty.NdkStackBlur;
import ru.taaasty.R;
import ru.taaasty.utils.ImageUtils;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * A login screen that offers login via email/password.

 */
public class LoginActivity extends Activity implements
        SelectSignMethodFragment.OnFragmentInteractionListener,
        SignViaEmailFragment.OnFragmentInteractionListener,
        RecoverPasswordFragment.OnFragmentInteractionListener,
        SignUpFragment.OnFragmentInteractionListener
{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LoginActivity";

    private static final String FRAGMENT_BACK_STACK_TAG1 = "fragment_back_stack_Tag1";

    private static final String BUNDLE_CURRENT_BACKGROUND_ID = "current_background_id";
    public static final int PRIMARY_BACKGROUND_IN_SAMPLE_SIZE = 0;
    public static final int SECONDARY_BACKGROUND_IN_SAMPLE_SIZE = 1;
    public static final int SECONDARY_BACKGROUND_BLUR_KERNEL = 24;

    private final Background mBackground = new Background();

    @DrawableRes
    private int mCurrentBackgroundId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (savedInstanceState == null) {
            mCurrentBackgroundId = choseRandomBackground();
        } else {
            mCurrentBackgroundId = savedInstanceState.getInt(BUNDLE_CURRENT_BACKGROUND_ID);
        }
        mBackground.setBackgroundPrimary();

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, SelectSignMethodFragment.newInstance())
                    .commit();
        }

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                if (getFragmentManager().getBackStackEntryCount() == 0) {
                    mBackground.setBackgroundPrimary();
                }
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CURRENT_BACKGROUND_ID, mCurrentBackgroundId);
    }

    @DrawableRes
    private int choseRandomBackground() {
        int resId;
        Random r = new Random();
        switch (r.nextInt(3)) {
            case 0:
                resId = R.drawable.back_01_test;
                break;
            case 1:
                resId = R.drawable.back_02_test;
                break;
            default:
                resId = R.drawable.back_03_test;
        }
        return resId;
    }

    @Override
    public void onSignViaVkontakteClicked() {
        Toast.makeText(this, R.string.not_ready_yet, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSignViaEmailClicked() {
        SignViaEmailFragment sf = SignViaEmailFragment.newInstance();
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit,
                        R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit)
                .replace(R.id.container, sf)
                .addToBackStack(FRAGMENT_BACK_STACK_TAG1)
                .commit();
    }

    @Override
    public void onRegisterClicked() {
        SignUpFragment sf = SignUpFragment.newInstance();
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit,
                        R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit)
                .replace(R.id.container, sf)
                .addToBackStack(FRAGMENT_BACK_STACK_TAG1)
                .commit();
    }

    private void switchToMainScreen() {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void hideIme() {
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        View v=this.getCurrentFocus();
        if(v==null)
            return;

        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public Animator createBlurOutAnimator() {
        return mBackground.createBlurOutAnimator();
    }

    public Animator createBlurInAnimator() {
        return mBackground.createBlurInAnimator();
    }

    private void closeAllFragments() {
        hideIme();
        getFragmentManager().popBackStack(FRAGMENT_BACK_STACK_TAG1, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onSignSuccess() {
        switchToMainScreen();
    }

    @Override
    public void onSignViaEmailBackPressed() {
        closeAllFragments();
    }

    @Override
    public void onIForgotPasswordPressed() {
        RecoverPasswordFragment sf = RecoverPasswordFragment.newInstance();
        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_left_exit,
                        R.animator.fragment_slide_right_enter, R.animator.fragment_slide_right_exit)
                .replace(R.id.container, sf)
                .addToBackStack(FRAGMENT_BACK_STACK_TAG1)
                .commit();
    }

    @Override
    public void onIHaveNotRegisteredPressed() {
        closeAllFragments();
        onRegisterClicked();
    }

    @Override
    public void onForgotPasswordRequestSent() {
        closeAllFragments();
        Toast.makeText(this, "Спасибо. Мы отправили вам письмо с инструкциями.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onForgotPasswordBackPressed() {
        closeAllFragments();
    }

    @Override
    public void onSignUpSuccess() {
        switchToMainScreen();
    }

    @Override
    public void onSignUpBackPressed() {
        closeAllFragments();
    }

    @Override
    public void onIHaveRegisteredPressed() {
        closeAllFragments();
        onSignViaEmailClicked();
    }


    public class Background {

        private final float maxScaleFactor = 1.5F;
        private final float minScaleFactor = 2.2F;
        private int mSwapMs = 10000;

        private final NdkStackBlur mBlur = NdkStackBlur.create();
        private final Random random = new Random();
        private Bitmap mCurrentBackgroundBitmap;

        private Animator mKenBurnsAnimator;

        private float mCurrentScale = 1.0f;
        private float mTranslationX;
        private float mTranslationY;

        public void setBackgroundPrimary() {
            TimingLogger timings = null;

            if (DBG) timings = new TimingLogger("Taaasty", "refreshBackgroundPrimary");

            mCurrentBackgroundBitmap = decodeBackgroundBitmap(mCurrentBackgroundId, PRIMARY_BACKGROUND_IN_SAMPLE_SIZE);
            if (mCurrentBackgroundBitmap == null) {
                Log.e(TAG, "b is null");
                return;
            }
            refreshPrimaryBackground();
            // startKenBurnsAnimator();
            if (DBG) timings.dumpToLog();
        }

        public void setBackgroundSecondary() {
            stopKenBurnsAnimator();
            mCurrentBackgroundBitmap = decodeBackgroundBitmap(mCurrentBackgroundId, SECONDARY_BACKGROUND_IN_SAMPLE_SIZE);
            if (mCurrentBackgroundBitmap == null) {
                Log.e(TAG, "b is null");
                return;
            }

            mBlur.blur(SECONDARY_BACKGROUND_BLUR_KERNEL, mCurrentBackgroundBitmap);

            BitmapDrawable bm = new BitmapDrawable(getResources(), mCurrentBackgroundBitmap);
            bm.setGravity(Gravity.FILL);
            Drawable darkenLayer = new ColorDrawable(getResources().getColor(R.color.login_activity_darken_layer));
            LayerDrawable ld = new LayerDrawable(new Drawable[] {bm, darkenLayer});
            setBackgroundDrawable(ld);
        }

        private Bitmap decodeBackgroundBitmap(int backgroundResId, int inSampleSizeAdd) {
            float scale, scaleX, scaleY;
            TimingLogger timings = null;

            if (DBG) timings = new TimingLogger("Taaasty", "decodeBackgroundBitmap");

            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);

            InputStream is = null;
            try {
                is = getResources().openRawResource(backgroundResId);
                BitmapRegionDecoder bre = BitmapRegionDecoder.newInstance(is, true);

                Rect region = new Rect(0, 0, bre.getWidth(), bre.getHeight());

                scaleY = displaySize.y / (float)bre.getHeight();
                scaleX = displaySize.x / (float)bre.getWidth();
                scale = Math.max(scaleX, scaleY);

                float scaledDisplayWidth = displaySize.x / scale;
                float scaledDisplayHeight = displaySize.y / scale;

                int leftTop = (int)(region.centerX() - (scaledDisplayWidth / 2));
                region.intersect(leftTop, 0, (int)Math.ceil(leftTop + scaledDisplayWidth), (int)Math.ceil(scaledDisplayHeight));
                if (DBG) Log.v(TAG, "region: " + region);

                final BitmapFactory.Options options;
                options = new BitmapFactory.Options();
                options.inJustDecodeBounds = false;
                options.inMutable = false;

                int samplesize = ImageUtils.calculateInSampleSize(
                        bre.getHeight(),
                        bre.getWidth(),
                        (int)(bre.getHeight() * scale),
                        (int)(bre.getWidth() * scale));

                if (inSampleSizeAdd == 0 ) {
                    options.inSampleSize = samplesize;
                } else {
                    options.inSampleSize = 1 << (int)( inSampleSizeAdd + Math.log(samplesize) / Math.log(2));
                }

                return bre.decodeRegion(region, options);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (DBG) timings.dumpToLog();
            }
            return null;
        }

        private void refreshPrimaryBackground() {
            Matrix m = new Matrix();
            m.postScale(mCurrentScale, mCurrentScale);
            m.postTranslate(mTranslationX, mTranslationY);

            int w = mCurrentBackgroundBitmap.getWidth();
            int h = mCurrentBackgroundBitmap.getHeight();

            Bitmap b = Bitmap.createBitmap(mCurrentBackgroundBitmap, 0, 0, w, h, m, true);

            BitmapDrawable bm = new BitmapDrawable(getResources(), b);
            bm.setGravity(Gravity.FILL);
            Drawable darkenLayer = getResources().getDrawable(R.drawable.darkening_layer_gradient);

            LayerDrawable ld = new LayerDrawable(new Drawable[] {bm,  darkenLayer});
            setBackgroundDrawable(ld);
        }

        void setBackgroundDrawable(Drawable dw) {
            getWindow().setBackgroundDrawable(dw);
        }

        private float pickScale() {
            return this.minScaleFactor + this.random.nextFloat() * (this.maxScaleFactor - this.minScaleFactor);
        }

        private float pickTranslation(int value, float ratio) {
            return value * (ratio - 1.0f) * (this.random.nextFloat() - 0.5f);
        }

        private void startKenBurnsAnimator() {
            if (mKenBurnsAnimator != null) {
                mKenBurnsAnimator.cancel();
            }
            float fromScale = pickScale();
            float toScale = pickScale();
            float fromTranslationX = pickTranslation(mCurrentBackgroundBitmap.getWidth(), fromScale);
            float fromTranslationY = pickTranslation(mCurrentBackgroundBitmap.getHeight(), fromScale);
            float toTranslationX = pickTranslation(mCurrentBackgroundBitmap.getWidth(), toScale);
            float toTranslationY = pickTranslation(mCurrentBackgroundBitmap.getHeight(), toScale);

            PropertyValuesHolder pvhScale = PropertyValuesHolder.ofFloat("scale", fromScale, toScale);
            PropertyValuesHolder pvhX = PropertyValuesHolder.ofFloat("x", fromTranslationX, toTranslationX);
            PropertyValuesHolder pvhY = PropertyValuesHolder.ofFloat("y", fromTranslationY, toTranslationY);

            ValueAnimator va = ValueAnimator.ofPropertyValuesHolder(pvhScale, pvhX, pvhY);
            va.setDuration(mSwapMs);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentScale = (Float)animation.getAnimatedValue("scale");
                    mTranslationX = (Float)animation.getAnimatedValue("x");
                    mTranslationY = (Float)animation.getAnimatedValue("y");
                    refreshPrimaryBackground();
                }
            });
            mKenBurnsAnimator = va;
            va.start();
        }

        private void stopKenBurnsAnimator() {
            if (mKenBurnsAnimator != null) {
                mKenBurnsAnimator.cancel();
                mKenBurnsAnimator = null;
            }
        }

        public Animator createBlurOutAnimator() {
            BlurAnimationListener l = new UnblurAnimationListener();
            ValueAnimator animation = ValueAnimator.ofInt(SECONDARY_BACKGROUND_BLUR_KERNEL, 0);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.setDuration(getResources().getInteger(R.integer.longAnimTime));

            animation.addListener(l);
            animation.addUpdateListener(l);
            return animation;
        }

        public Animator createBlurInAnimator() {
            BlurAnimationListener l = new BlurAnimationListener();
            ValueAnimator animation = ValueAnimator.ofInt(0, SECONDARY_BACKGROUND_BLUR_KERNEL);
            animation.setInterpolator(new AccelerateInterpolator());
            animation.setDuration(getResources().getInteger(R.integer.longAnimTime));

            animation.addListener(l);
            animation.addUpdateListener(l);
            return animation;
        }

        private class BlurAnimationListener implements ObjectAnimator.AnimatorListener, ObjectAnimator.AnimatorUpdateListener {
            private final Bitmap mOriginalBitmap;
            private final Drawable mDarkenLayer;

            public BlurAnimationListener() {
                mOriginalBitmap = decodeBackgroundBitmap(mCurrentBackgroundId, SECONDARY_BACKGROUND_IN_SAMPLE_SIZE);
                mDarkenLayer = new ColorDrawable(getResources().getColor(R.color.login_activity_darken_layer));
            }

            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBackground.setBackgroundSecondary();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }

            Drawable getDarkenLayer() {
                return mDarkenLayer;
            }

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Drawable dw;
                int blur = (Integer)animation.getAnimatedValue();
                Bitmap mWorkingBitmap = mOriginalBitmap.copy(mOriginalBitmap.getConfig(), true);
                if (blur != 0) mBlur.blur(blur, mWorkingBitmap);
                dw = new BitmapDrawable(getResources(), mWorkingBitmap);
                LayerDrawable lw = new LayerDrawable(new Drawable[]{dw, getDarkenLayer()});
                setBackgroundDrawable(lw);
            }
        }

        private class UnblurAnimationListener extends BlurAnimationListener {

            private Drawable mDarkenLayer = getResources().getDrawable(R.drawable.darkening_layer_gradient);

            @Override
            public void onAnimationEnd(Animator animation) {
                mBackground.setBackgroundPrimary();
            }

            @Override
            Drawable getDarkenLayer() {
                return mDarkenLayer;
            }
        }
    }


}



