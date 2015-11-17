package ru.taaasty.ui;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public interface CustomErrorView {
    void notifyError(Fragment fragment, @Nullable Throwable exception, int fallbackResId);
}
