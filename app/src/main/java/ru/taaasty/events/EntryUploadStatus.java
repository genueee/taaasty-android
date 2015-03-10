package ru.taaasty.events;

import ru.taaasty.model.PostForm;

/**
 * Created by alexey on 04.09.14.
 */
public class EntryUploadStatus {

    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public Throwable exception;

    public PostForm.PostFormHtml entry;

    public String error = "";

    public EntryUploadStatus() {
    }

    public static EntryUploadStatus createPostCompleted(PostForm.PostFormHtml entry) {
        EntryUploadStatus status = new EntryUploadStatus();
        status.entry = entry;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = true;
        return status;
    }

    public static EntryUploadStatus createPostFinishedWithError(PostForm.PostFormHtml entry, String error, Throwable ex) {
        EntryUploadStatus status = new EntryUploadStatus();
        status.entry = entry;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = false;
        status.error = error;
        status.exception = ex;
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
                ", error='" + error + '\'' +
                '}';
    }
}
