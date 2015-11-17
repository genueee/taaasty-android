package ru.taaasty.events;

import ru.taaasty.rest.model.PostForm;

/**
 * Created by alexey on 04.09.14.
 */
public class EntryUploadStatus {

    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public PostForm.PostFormHtml entry;

    public Throwable exception;

    public int errorFallbackResId;

    public EntryUploadStatus() {
    }

    public static EntryUploadStatus createPostCompleted(PostForm.PostFormHtml entry) {
        EntryUploadStatus status = new EntryUploadStatus();
        status.entry = entry;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = true;
        return status;
    }

    public static EntryUploadStatus createPostFinishedWithError(PostForm.PostFormHtml entry, Throwable ex, int errorFallbackResId) {
        EntryUploadStatus status = new EntryUploadStatus();
        status.entry = entry;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.exception = ex;
        status.errorFallbackResId = errorFallbackResId;
        return status;
    }

    public boolean isFinished() {
        return status == STATUS_UPLOAD_FINISHED;
    }

    @Override
    public String toString() {
        return "PostUploadStatus{" +
                "newStatus=" + status +
                ", successfully=" + successfully +
                ", exception=" + exception +
                ", entry=" + entry +
                ", errorFallbackResId='" + errorFallbackResId + '\'' +
                '}';
    }
}
