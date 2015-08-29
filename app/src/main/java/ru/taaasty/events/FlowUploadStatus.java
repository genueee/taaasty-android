package ru.taaasty.events;

import android.support.annotation.Nullable;

import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.PostFlowForm;
import ru.taaasty.rest.model.PostForm;

/**
 * Created by alexey on 15.09.15.
 */
public class FlowUploadStatus {
    public final boolean successfully;

    public final Throwable exception;

    public final PostFlowForm.AsHtml form;

    @Nullable
    public final String error;

    public final Flow flow;

    public static FlowUploadStatus createCompleted(PostFlowForm.AsHtml form, Flow flow) {
        return new FlowUploadStatus(true, null, form, null, flow);
    }

    public static FlowUploadStatus createFinishedWithError(PostFlowForm.AsHtml entry, String error, Throwable ex) {
        return new FlowUploadStatus(false, ex, entry, error, null);
    }

    private FlowUploadStatus(boolean successfully, Throwable exception, PostFlowForm.AsHtml form, String error, Flow flow) {
        this.successfully = successfully;
        this.exception = exception;
        this.form = form;
        this.error = error;
        this.flow = flow;
    }

    @Override
    public String toString() {
        return "FlowUploadStatus{" +
                ", successfully=" + successfully +
                ", flow: " + flow +
                ", exception=" + exception +
                ", form=" + form +
                ", error='" + error + '\'' +
                '}';
    }

}
