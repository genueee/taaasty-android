package ru.taaasty;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import de.greenrobot.event.EventBus;
import retrofit.client.Response;
import ru.taaasty.events.PostUploadStatus;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.model.PostEntry;
import ru.taaasty.model.PostImageEntry;
import ru.taaasty.model.PostQuoteEntry;
import ru.taaasty.model.PostTextEntry;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.Userpic;
import ru.taaasty.service.ApiDesignSettings;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.service.ApiUsers;
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

    private static final String ACTION_UPLOAD_USERPIC = "ru.taaasty.action.UPLOAD_USERPIC";
    private static final String ACTION_UPLOAD_BACKGROUND = "ru.taaasty.action.UPLOAD_BACKGROUND";

    // TODO: Rename parameters
    private static final String EXTRA_ENTRY = "ru.taaasty.extra.ENTRY";

    private static final String EXTRA_USER_ID = "ru.taaasty.extra.USER_ID";
    private static final String EXTRA_IMAGE_URI = "ru.taaasty.extra.IMAGE_URI";

    private final ApiEntries mApiEntriesService;
    private final ApiUsers mApiUsersService;
    private final ApiDesignSettings mApiDesignService;

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

    public static void startUploadUserpic(Context context, long userId, Uri imageUri) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_UPLOAD_USERPIC);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        context.startService(intent);
    }

    public static void startUploadBackground(Context context, long userId, Uri imageUri) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_UPLOAD_BACKGROUND);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        context.startService(intent);
    }

    public UploadService() {
        super("UploadService");
        mApiEntriesService = NetworkUtils.getInstance().createRestAdapter().create(ApiEntries.class);
        mApiUsersService = NetworkUtils.getInstance().createRestAdapter().create(ApiUsers.class);
        mApiDesignService = NetworkUtils.getInstance().createRestAdapter().create(ApiDesignSettings.class);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST_ENTRY.equals(action)) {
                PostEntry entry = intent.getParcelableExtra(EXTRA_ENTRY);
                handlePostEntry(entry);
            } else if (ACTION_UPLOAD_USERPIC.equals(action)) {
                Uri imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
                long userId = intent.getLongExtra(EXTRA_USER_ID, -1);
                handleUploadUserpic(userId, imageUri);
            } else if (ACTION_UPLOAD_BACKGROUND.equals(action)) {
                Uri imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI);
                long userId = intent.getLongExtra(EXTRA_USER_ID, -1);
                handleUploadUserBackground(userId, imageUri);
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
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = PostUploadStatus.createPostFinishedWithError(entry, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
    }

    private void handleUploadUserpic(long userId, Uri imageUri) {
        UserpicUploadStatus status = null;
        try {
            Userpic response = mApiUsersService.uploadUserpicSync(new ContentTypedOutput(this, imageUri, null));
            status = UserpicUploadStatus.createUploadCompleted(userId, imageUri, response);
            if (DBG) Log.v(TAG, "userpic response: " + response);
        }catch (NetworkUtils.ResponseErrorException ree) {
            status = UserpicUploadStatus.createUploadFinishedWithError(userId, imageUri, ree.error.error, ree);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = UserpicUploadStatus.createUploadFinishedWithError(userId, imageUri, getString(R.string.error_upload_userpic), ex);
        }
        EventBus.getDefault().post(status);
    }

    public void handleUploadUserBackground(long userId, Uri imageUri) {
        TlogBackgroundUploadStatus status = null;
        try {
            TlogDesign response = mApiDesignService.uploadBackgroundSync(
                    UserManager.getInstance().getCurrentUserSlug(), // XXX: избавиться, когда поправят API
                    new ContentTypedOutput(this, imageUri, null));
            status = TlogBackgroundUploadStatus.createUploadCompleted(userId, imageUri, response);
            if (DBG) Log.v(TAG, "userpic response: " + response);
        }catch (NetworkUtils.ResponseErrorException ree) {
            status = TlogBackgroundUploadStatus.createUploadFinishedWithError(userId, imageUri, ree.error.error, ree);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = TlogBackgroundUploadStatus.createUploadFinishedWithError(userId, imageUri, getString(R.string.error_upload_userpic), ex);
        }
        EventBus.getDefault().post(status);
    }
}
