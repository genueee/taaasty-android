package ru.taaasty.events;

import android.net.Uri;

import ru.taaasty.rest.model2.Userpic;

/**
 * Created by alexey on 11.09.14.
 */
public class UserpicUploadStatus {
    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public long userId;

    public Uri imageUri;

    public Userpic newUserpic;

    public Throwable exception;

    public int errorResId;

    public UserpicUploadStatus() {
    }

    public static UserpicUploadStatus createUploadCompleted(long userId, Uri imageUri, Userpic newUserpic) {
        UserpicUploadStatus status = new UserpicUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = true;
        status.newUserpic = newUserpic;
        return status;
    }

    public static UserpicUploadStatus createUploadFinishedWithError(long userId, Uri imageUri, int errorResId, Throwable ex) {
        UserpicUploadStatus status = new UserpicUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.errorResId = errorResId;
        status.exception = ex;
        return status;
    }

    public static UserpicUploadStatus createUploadStarted(long userId, Uri imageUri) {
        UserpicUploadStatus status = new UserpicUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_STARTED;
        status.successfully = true;
        return status;
    }

    public boolean isFinished() {
        return status == STATUS_UPLOAD_FINISHED;
    }

}
