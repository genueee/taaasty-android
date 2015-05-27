package ru.taaasty.events;

import android.net.Uri;

import ru.taaasty.rest.model.Userpic;

/**
 * Created by alexey on 11.09.14.
 */
public class UserpicUploadStatus {
    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public Throwable exception;

    public long userId;

    public Uri imageUri;

    public Userpic newUserpic;

    public String error = "";

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

    public static UserpicUploadStatus createUploadFinishedWithError(long userId, Uri imageUri, String error, Throwable ex) {
        UserpicUploadStatus status = new UserpicUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.error = error;
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
