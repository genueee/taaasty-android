package ru.taaasty.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;

import com.trello.rxlifecycle.components.support.RxFragment;

import ru.taaasty.BuildConfig;
import ru.taaasty.RetainedFragmentCallbacks;

public abstract class FragmentWithWorkFragment<T extends Fragment> extends RxFragment implements RetainedFragmentCallbacks {

    private static final boolean DBG = BuildConfig.DEBUG;

    private static final String TAG = "FragmentWithWork";

    boolean onWorkFragmentActivityCalled;

    boolean onWorkFragmentResumeCalled;

    @Nullable
    public abstract T getWorkFragment();

    public abstract void initWorkFragment();


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (DBG) Log.v(TAG, "onViewCreated() fragment: " + getTag());
        initWorkFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated() fragment: " + getTag());
        if (getWorkFragment() != null && getWorkFragment().isResumed()) {
            onWorkFragmentActivityCreated();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() fragment: " + getTag());
        if (getWorkFragment() != null && getWorkFragment().isResumed()) {
            onWorkFragmentActivityCreated();
            onWorkFragmentResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() fragment: " + getTag());
        onWorkFragmentResumeCalled = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView() fragment: " + getTag());
        onWorkFragmentActivityCalled = false;
    }

    @Override
    public void onWorkFragmentActivityCreated() {
        if (onWorkFragmentActivityCalled) return;
        onWorkFragmentActivityCalled = true;
        onWorkFragmentActivityCreatedSafe();
    }

    public void onWorkFragmentActivityCreatedSafe() {
        Log.d(TAG, "onWorkFragmentActivityCreatedSafe() fragment: " + getTag());
    }

    @Override
    public void onWorkFragmentResume() {
        if (onWorkFragmentResumeCalled) return;
        onWorkFragmentResumeCalled = true;
        onWorkFragmentResumeSafe();
    }

    public void onWorkFragmentResumeSafe() {
        Log.d(TAG, "onWorkFragmentResumeSafe() fragment: " + getTag());
    }

}
