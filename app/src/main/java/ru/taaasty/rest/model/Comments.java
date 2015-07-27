package ru.taaasty.rest.model;

import android.support.annotation.Nullable;

/**
 * Created by alexey on 01.08.14.
 */
public class Comments {

    public Comment comments[];

    @Nullable
    public long fromCommentId;

    @Nullable
    public long toCommentId;

    public String order;

    public int totalCount;
}
