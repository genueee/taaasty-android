package ru.taaasty.ui;


import android.support.annotation.Nullable;

public interface CustomErrorView {

    public void notifyError(CharSequence error, @Nullable Throwable exception);

}
