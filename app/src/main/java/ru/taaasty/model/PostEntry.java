package ru.taaasty.model;

import android.os.Parcelable;

import ru.taaasty.service.ApiEntries;

/**
 * Created by alexey on 03.09.14.
 */
public abstract class PostEntry implements Parcelable {

    public String privacy = ApiEntries.PRIVACY_PUBLIC;

    public void setIsPrivate(boolean isPrivate) {
        privacy = isPrivate ? ApiEntries.PRIVACY_PRIVATE : ApiEntries.PRIVACY_PUBLIC;
    }

    public boolean isPrivate() {
        return ApiEntries.PRIVACY_PRIVATE.equals(privacy);
    }
}
