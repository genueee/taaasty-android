package ru.taaasty.events;

import android.net.Uri;

import ru.taaasty.rest.model.TlogDesign;

/**
 * Created by alexey on 11.09.14.
 */
public class TlogBackgroundUploadStatus {
    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public long userId;

    public Uri imageUri;

    public TlogDesign design;

    public Throwable exception;

    public int errorFallbackResId;

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

    public static TlogBackgroundUploadStatus createUploadFinishedWithError(long userId, Uri imageUri, int errorFallbackResId, Throwable ex) {
        TlogBackgroundUploadStatus status = new TlogBackgroundUploadStatus();
        status.userId = userId;
        status.imageUri = imageUri;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.errorFallbackResId = errorFallbackResId;
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
