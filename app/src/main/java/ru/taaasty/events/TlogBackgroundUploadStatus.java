package ru.taaasty.events;

import android.net.Uri;

import ru.taaasty.model.TlogDesign;

/**
 * Created by alexey on 11.09.14.
 */
public class TlogBackgroundUploadStatus {
    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public Throwable exception;

    public long userId;

    public Uri imageUri;

    public TlogDesign design;

    public String error = "";

    public TlogBackgroundUploadStatus() {
    }

    public static TlogBackgroundUploadStatus createUploadCompleted(long userId, Uri imageUri, TlogDesign design) {
        TlogBackgroundUploadStatus status = new TlogBackgroundUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = true;
        status.design = design;
        return status;
    }

    public static TlogBackgroundUploadStatus createUploadFinishedWithError(long userId, Uri imageUri, String error, Throwable ex) {
        TlogBackgroundUploadStatus status = new TlogBackgroundUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.error = error;
        status.exception = ex;
        return status;
    }

    public static TlogBackgroundUploadStatus createUploadStarted(long userId, Uri imageUri) {
        TlogBackgroundUploadStatus status = new TlogBackgroundUploadStatus();
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
