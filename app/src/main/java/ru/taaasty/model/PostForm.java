package ru.taaasty.model;

import android.os.Parcelable;

/**
 * Created by alexey on 03.09.14.
 */
public abstract class PostForm implements Parcelable {

    @Entry.EntryPrivacy
    public String privacy = Entry.PRIVACY_PUBLIC;

}
