package ru.taaasty.model;

import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by alexey on 01.08.14.
 */
public class Comments {

    public List<Comment> comments;

    @Nullable
    public long fromCommentId;

    @Nullable
    public long toCommentId;

    public String order;

    public int totalCount;
}
