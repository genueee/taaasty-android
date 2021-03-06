package ru.taaasty.ui.login;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.util.TimingLogger;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import com.facebook.FacebookSdk;

import java.util.Random;

import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.Constants;
import ru.taaasty.R;
import ru.taaasty.utils.AnalyticsHelper;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.ImeUtils;
import ru.taaasty.utils.MessageHelper;
import ru0xdc.NdkStackBlur;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends ActivityBase implements
        SelectSignMethodFragment.OnFragmentInteractionListener,
        SignViaEmailFragment.OnFragmentInteractionListener,
        RecoverPasswordFragment.OnFragmentInteractionListener,
        SignUpFragment.OnFragmentInteractionListener,
        SignViaVkontakteFragment.OnFragmentInteractionListener,
        SignViaFacebookFragment.OnFragmentInteractionListener
{
    private static final boolean DBG = BuildConfig.DEBUG;
    private static final String TAG = "LoginActivity";

    private static final String FRAGMENT_BACK_STACK_TAG1 = "fragment_back_stack_Tag1";

    private static final String BUNDLE_CURRENT_BACKGROUND_ID = "current_background_id";

    public static final int PRIMARY_BACKGROUND_IN_SAMPLE_SIZE = 0;
    public static final int SECONDARY_BACKGROUND_IN_SAMPLE_SIZE = 1;
    public static final int SECONDARY_BACKGROUND_BLUR_KERNEL = 24;
    private static final String DIALOG_LOGIN_VKONTAKTE_FACEBOOK = "DIALOG_LOGIN_VKONTAKTE_FACEBOOK";

    private final Background mBackground = new Background();

    @DrawableRes
    private int mCurrentBackgroundId;

    public static void startActivity(Activity source, int requestCode, View animateFrom) {
        Intent intent = new Intent(source, LoginActivity.class);
        if (animateFrom != null) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeScaleUpAnimation(
                    animateFrom, 0, 0, animateFrom.getWidth(), animateFrom.getHeight());
            ActivityCompat.startActivityForResult(source, intent, requestCode, options.toBundle());
        } else {
            source.startActivityForResult(intent, requestCode);
        }
    }

    public static void startActivityFromFragment(Context source, android.support.v4.app.Fragment fragment, int requestCode, View animateFrom) {
        Intent intent = new Intent(source, LoginActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if(!getResources().getBoolean(R.bool.is_tablet)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

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

        getFragmentManager().addOnBackStackChangedListener(() -> {
            if (getFragmentManager().getBackStackEntryCount() == 0) {
                mBackground.setBackgroundPrimary();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (FacebookSdk.isFacebookRequestCode(requestCode)) {
            Fragment facebookFragment = getFragmentManager().findFragmentByTag(DIALOG_LOGIN_VKONTAKTE_FACEBOOK);
            if (facebookFragment != null) {
                facebookFragment.onActivityResult(requestCode, resultCode, data);
            } else {
                if (DBG) Log.e(TAG, "onActivityResult with facebook requestCode but no fragment");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CURRENT_BACKGROUND_ID, mCurrentBackgroundId);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
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
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(DIALOG_LOGIN_VKONTAKTE_FACEBOOK) != null) return;
        DialogFragment dialog = SignViaVkontakteFragment.createInstance();
        dialog.show(getFragmentManager(), DIALOG_LOGIN_VKONTAKTE_FACEBOOK);
    }

    @Override
    public void onSignViaFacebookClicked() {
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag(DIALOG_LOGIN_VKONTAKTE_FACEBOOK) != null) return;
        DialogFragment dialog = SignViaFacebookFragment.createInstance();
        dialog.show(getFragmentManager(), DIALOG_LOGIN_VKONTAKTE_FACEBOOK);
    }

    @Override
    public void notifyError(CharSequence error, @Nullable Throwable exception) {
        if (exception != null) Log.e(TAG, error.toString(), exception);
        if (DBG) {
            MessageHelper.showError(this, error + " " + (exception == null ? "" : exception.getLocalizedMessage()), exception);
        } else {
            MessageHelper.showError(this, error, exception);
        }
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

    public Animator createBlurOutAnimator() {
        return mBackground.createBlurOutAnimator();
    }

    public Animator createBlurInAnimator() {
        return mBackground.createBlurInAnimator();
    }

    private void closeAllFragments() {
        ImeUtils.hideIme(getCurrentFocus());
        getFragmentManager().popBackStack(FRAGMENT_BACK_STACK_TAG1, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onSignSuccess() {
        AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_LOGIN, Constants.ANALYTICS_LABEL_EMAIL);
        setResult(Activity.RESULT_OK);
        finish();
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
        AnalyticsHelper.getInstance().sendAccountEvent("Восстановление пароля - отправлено на емайл");
    }

    @Override
    public void onForgotPasswordBackPressed() {
        closeAllFragments();
    }

    @Override
    public void onSignUpSuccess() {
        Toast.makeText(this, R.string.sign_up_success, Toast.LENGTH_LONG).show();
        AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_REGISTER, Constants.ANALYTICS_LABEL_EMAIL);
        setResult(Activity.RESULT_OK);
        finish();
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

    @Override
    public void onSignViaVkontakteSuccess(boolean newUserCreated) {
        if (newUserCreated) {
            AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_REGISTER, Constants.ANALYTICS_LABEL_VK);
        } else {
            AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_LOGIN, Constants.ANALYTICS_LABEL_VK);
        }
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onSignViaFacebookSuccess(boolean newUserCreated) {
        if (newUserCreated) {
            AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_REGISTER, Constants.ANALYTICS_LABEL_FACEBOOK);
        } else {
            AnalyticsHelper.getInstance().sendAccountEvent(Constants.ANALYTICS_ACTION_ACCOUNT_LOGIN, Constants.ANALYTICS_LABEL_FACEBOOK);
        }
        setResult(Activity.RESULT_OK);
        finish();
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
            if (DBG) {
                timings.addSplit("refreshBackgroundPrimary done");
                timings.dumpToLog();
            }
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
            Point displaySize = new Point();
            getWindowManager().getDefaultDisplay().getSize(displaySize);
            return ImageUtils.decodeBackgroundBitmap(LoginActivity.this, backgroundResId, displaySize, inSampleSizeAdd);
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
            va.addUpdateListener(animation -> {
                mCurrentScale = (Float)animation.getAnimatedValue("scale");
                mTranslationX = (Float)animation.getAnimatedValue("x");
                mTranslationY = (Float)animation.getAnimatedValue("y");
                refreshPrimaryBackground();
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



