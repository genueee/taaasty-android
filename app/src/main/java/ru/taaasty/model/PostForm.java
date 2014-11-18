package ru.taaasty.model;

import android.os.Parcelable;

/**
 * Created by alexey on 03.09.14.
 */
public abstract class PostForm implements Parcelable {

    public String privacy = Entry.PRIVACY_PUBLIC;

    public void setIsPrivate(boolean isPrivate) {
        privacy = isPrivate ? Entry.PRIVACY_PRIVATE : Entry.PRIVACY_PUBLIC;
    }

    public boolean isPrivate() {
        return Entry.PRIVACY_PRIVATE.equals(privacy);
    }
}
