package ru.taaasty.events;

import ru.taaasty.model.PostEntry;

/**
 * Created by alexey on 04.09.14.
 */
public class PostUploadStatus {

    public static final int STATUS_UPLOAD_STARTED = 1;
    public static final int STATUS_UPLOAD_FINISHED = 2;

    public int status = STATUS_UPLOAD_FINISHED;

    public boolean successfully = true;

    public Throwable exception;

    public PostEntry entry;

    public String error = "";

    public PostUploadStatus() {
    }

    public static PostUploadStatus createPostCompleted(PostEntry entry) {
        PostUploadStatus status = new PostUploadStatus();
        status.entry = entry;
        status.status = STATUS_UPLOAD_FINISHED;
        status.successfully = true;
        return status;
    }

    public static PostUploadStatus createPostFinishedWithError(PostEntry entry, String error, Throwable ex) {
        PostUploadStatus status = new PostUploadStatus();
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
                "status=" + status +
                ", successfully=" + successfully +
                ", exception=" + exception +
                ", entry=" + entry +
                ", error='" + error + '\'' +
                '}';
    }
}
