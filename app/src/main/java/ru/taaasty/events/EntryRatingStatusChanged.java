package ru.taaasty.events;

/**
 * Created by alexey on 18.01.15.
 */
public class EntryRatingStatusChanged {

    public static final int STATUS_START_UPDATE = 1;

    public static final int STATUS_UPDATE_DONE = 2;

    public static final int STATUS_UPDATE_ERROR = 3;

    public final long entryId;

    public final int newStatus;


    public final int errorResId;

    public final Throwable errorException;

    public static EntryRatingStatusChanged updateStarted(long entryId) {
        return new EntryRatingStatusChanged(entryId, STATUS_START_UPDATE, -1, null);
    }

    public static EntryRatingStatusChanged updateDone(long entryId) {
        return new EntryRatingStatusChanged(entryId, STATUS_UPDATE_DONE, -1, null);
    }

    public static EntryRatingStatusChanged updateError(long entryId, int errorResId, Throwable errorException) {
        return new EntryRatingStatusChanged(entryId, STATUS_UPDATE_ERROR, errorResId, errorException);
    }

    public EntryRatingStatusChanged(long entryId, int newStatus, int errorResId, Throwable errorException) {
        this.entryId = entryId;
        this.newStatus = newStatus;
        this.errorResId = errorResId;
        this.errorException = errorException;
    }

}
