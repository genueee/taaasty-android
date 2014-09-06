package ru.taaasty;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;
import retrofit.client.Response;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.model.PostEntry;
import ru.taaasty.model.PostImageEntry;
import ru.taaasty.model.PostQuoteEntry;
import ru.taaasty.model.PostTextEntry;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.utils.ContentTypedOutput;
import ru.taaasty.utils.NetworkUtils;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class UploadService extends IntentService {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "UploadService";

    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_POST_ENTRY = "ru.taaasty.action.POST_ENTRY";

    // TODO: Rename parameters
    private static final String EXTRA_ENTRY = "ru.taaasty.extra.ENTRY";

    private final ApiEntries mApiEntriesService;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startPostEntry(Context context, PostEntry entry) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_POST_ENTRY);
        intent.putExtra(EXTRA_ENTRY, entry);
        context.startService(intent);
    }

    public UploadService() {
        super("UploadService");
        mApiEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST_ENTRY.equals(action)) {
                PostEntry entry = intent.getParcelableExtra(EXTRA_ENTRY);
                handlePostEntry(entry);
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handlePostEntry(PostEntry entry) {
        PostUploadStatus status;
        Response response;

        try {
            if (entry instanceof PostTextEntry) {
                PostTextEntry pte = (PostTextEntry)entry;
                response = mApiEntriesService.createTextPostSync(pte.title, pte.text, entry.privacy);
            } else if (entry instanceof PostQuoteEntry) {
                PostQuoteEntry pqe = (PostQuoteEntry)entry;
                response = mApiEntriesService.createQuoteEntrySync(pqe.text, pqe.source, entry.privacy);
            } else if (entry instanceof PostImageEntry) {
                PostImageEntry pie = (PostImageEntry)entry;
                response = mApiEntriesService.createImagePostSync(
                        pie.title,
                        entry.privacy,
                        pie.imageUri == null ? null : new ContentTypedOutput(this, pie.imageUri, null)
                );
            } else {
                throw new IllegalStateException();
            }
            status = PostUploadStatus.createPostCompleted(entry);
        } catch (NetworkUtils.ResponseErrorException ree) {
            status = PostUploadStatus.createPostFinishedWithError(entry, ree.error.error, ree);
        } catch (RetrofitError re) {
            if (DBG) throw re;
            status = PostUploadStatus.createPostFinishedWithError(entry, getString(R.string.error_vote), re);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = PostUploadStatus.createPostFinishedWithError(entry, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
    }
}
