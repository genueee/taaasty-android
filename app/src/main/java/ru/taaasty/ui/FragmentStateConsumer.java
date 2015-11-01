package ru.taaasty.ui;

import android.support.v4.app.Fragment;


public interface FragmentStateConsumer {

    void onFragmentAttached(Fragment fragment);

    void onFragmentDetached(Fragment fragment);

}
