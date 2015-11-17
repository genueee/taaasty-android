package ru.taaasty.events;

import ru.taaasty.rest.model.Flow;
import ru.taaasty.rest.model.PostFlowForm;

/**
 * Created by alexey on 15.09.15.
 */
public class FlowUploadStatus {
    public final boolean successfully;

    public final PostFlowForm.AsHtml form;

    public final Throwable exception;

    public final int errorFallbackResId;

    public final Flow flow;

    public static FlowUploadStatus createCompleted(PostFlowForm.AsHtml form, Flow flow) {
        return new FlowUploadStatus(true, null, form, 0, flow);
    }

    public static FlowUploadStatus createFinishedWithError(PostFlowForm.AsHtml entry, Throwable ex, int errorResId) {
        return new FlowUploadStatus(false, ex, entry, errorResId, null);
    }

    private FlowUploadStatus(boolean successfully, Throwable exception, PostFlowForm.AsHtml form, int errorResId, Flow flow) {
        this.successfully = successfully;
        this.exception = exception;
        this.form = form;
        this.errorFallbackResId = errorResId;
        this.flow = flow;
    }

    @Override
    public String toString() {
        return "FlowUploadStatus{" +
                ", successfully=" + successfully +
                ", flow: " + flow +
                ", exception=" + exception +
                ", form=" + form +
                ", errorFallbackResId='" + errorFallbackResId + '\'' +
                '}';
    }

}
