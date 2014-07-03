package ru.taaasty.model;

import android.support.annotation.Nullable;

/**
* Created by alexey on 10.07.14.
*/
public class Userpic {
    public static Userpic DUMMY = new Userpic();

    @Nullable
    public String largeUrl;

    @Nullable
    public String thumb128Url;

    @Nullable
    public String thumb64Url;

    @Nullable
    public String thumb32Url;

    @Nullable
    public String thumb16Url;

    @Nullable
    public String touchUrl;

}
