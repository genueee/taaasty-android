package ru.taaasty;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.model.Entry;
import ru.taaasty.model.PostAnonymousTextForm;
import ru.taaasty.model.PostForm;
import ru.taaasty.model.PostImageForm;
import ru.taaasty.model.PostQuoteForm;
import ru.taaasty.model.PostTextForm;
import ru.taaasty.model.TlogDesign;
import ru.taaasty.model.Userpic;
import ru.taaasty.service.ApiDesignSettings;
import ru.taaasty.service.ApiEntries;
import ru.taaasty.service.ApiUsers;
import ru.taaasty.utils.ContentTypedOutput;
import ru.taaasty.utils.NetworkUtils;
import ru.taaasty.utils.UiUtils;

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
    private static final String ACTION_EDIT_ENTRY = "ru.taaasty.action.EDIT_ENTRY";

    private static final String ACTION_UPLOAD_USERPIC = "ru.taaasty.action.UPLOAD_USERPIC";
    private static final String ACTION_UPLOAD_BACKGROUND = "ru.taaasty.action.UPLOAD_BACKGROUND";


    private static final String EXTRA_FORM = "ru.taaasty.extra.FORM";

    private static final String EXTRA_ENTRY_ID = "ru.taaasty.extra.ENTRY_ID";
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
    public static void startPostEntry(Context context, PostForm form) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_POST_ENTRY);
        intent.putExtra(EXTRA_FORM, form);
        context.startService(intent);
    }

    public static void startEditEntry(Context context, long entryId, PostForm form) {
        Intent intent = new Intent(context, UploadService.class);
        intent.setAction(ACTION_EDIT_ENTRY);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_FORM, form);
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
                PostForm entry = intent.getParcelableExtra(EXTRA_FORM);
                handlePostEntry(entry);
            } else if (ACTION_EDIT_ENTRY.equals(action)) {
                PostForm entry = intent.getParcelableExtra(EXTRA_FORM);
                long postId = intent.getLongExtra(EXTRA_ENTRY_ID, -1);
                handleEditEntry(postId, entry);
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
    private void handlePostEntry(PostForm form) {
        EntryUploadStatus status;
        Entry response = null;

        try {
            if (form instanceof  PostAnonymousTextForm) {
                PostAnonymousTextForm postText = (PostAnonymousTextForm) form;
                response = mApiEntriesService.createAnonymousPostSync(
                        UiUtils.safeToHtml(postText.title),
                        UiUtils.safeToHtml(postText.text));
            } else if (form instanceof PostTextForm) {
                PostTextForm postText = (PostTextForm) form;
                response = mApiEntriesService.createTextPostSync(
                        UiUtils.safeToHtml(postText.title),
                        UiUtils.safeToHtml(postText.text), form.privacy);
            } else if (form instanceof PostQuoteForm) {
                PostQuoteForm postQuote = (PostQuoteForm)form;
                response = mApiEntriesService.createQuoteEntrySync(
                        UiUtils.safeToHtml(postQuote.text),
                        UiUtils.safeToHtml(postQuote.source), form.privacy);
            } else if (form instanceof PostImageForm) {
                PostImageForm postImage = (PostImageForm)form;
                response = mApiEntriesService.createImagePostSync(
                        UiUtils.safeToHtml(postImage.title),
                        form.privacy,
                        postImage.imageUri == null ? null : new ContentTypedOutput(this, postImage.imageUri, null)
                );
            } else {
                throw new IllegalStateException();
            }
            status = EntryUploadStatus.createPostCompleted(form);
        } catch (NetworkUtils.ResponseErrorException ree) {
            status = EntryUploadStatus.createPostFinishedWithError(form, ree.error.error, ree);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = EntryUploadStatus.createPostFinishedWithError(form, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
        if (response != null) EventBus.getDefault().post(new EntryChanged(response));
    }

    private void handleEditEntry(long entryId, PostForm form) {
        EntryUploadStatus status;
        Entry response = null;

        try {
            if (form instanceof PostAnonymousTextForm) {
                PostAnonymousTextForm postText = (PostAnonymousTextForm)form;
                response = mApiEntriesService.updateAnonymousPostSync(String.valueOf(entryId),
                        postText.title == null ? null : UiUtils.safeToHtml(postText.title),
                        postText.text == null ? null : UiUtils.safeToHtml(postText.text));
            } else if (form instanceof PostTextForm) {
                PostTextForm postText = (PostTextForm)form;
                response = mApiEntriesService.updateTextPostSync(String.valueOf(entryId),
                        postText.title == null ? null : UiUtils.safeToHtml(postText.title),
                        postText.text == null ? null : UiUtils.safeToHtml(postText.text), form.privacy);
            } else if (form instanceof PostQuoteForm) {
                PostQuoteForm postQuote = (PostQuoteForm)form;
                response = mApiEntriesService.updateQuoteEntrySync(String.valueOf(entryId),
                        postQuote.text == null ? null : UiUtils.safeToHtml(postQuote.text),
                        postQuote.source == null ? null : UiUtils.safeToHtml(postQuote.source), form.privacy);
            } else if (form instanceof PostImageForm) {
                PostImageForm postImage = (PostImageForm)form;
                response = mApiEntriesService.updateImagePostSync(String.valueOf(entryId),
                        postImage.title == null ? null : UiUtils.safeToHtml(postImage.title),
                        form.privacy,
                        postImage.imageUri == null ? null : new ContentTypedOutput(this, postImage.imageUri, null)
                );
            } else {
                throw new IllegalStateException();
            }
            status = EntryUploadStatus.createPostCompleted(form);
        } catch (NetworkUtils.ResponseErrorException ree) {
            status = EntryUploadStatus.createPostFinishedWithError(form, ree.error.error, ree);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = EntryUploadStatus.createPostFinishedWithError(form, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
        if (response != null) EventBus.getDefault().post(new EntryChanged(response));
    }

    private void handleUploadUserpic(long userId, Uri imageUri) {
        UserpicUploadStatus status;
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
        TlogBackgroundUploadStatus status;
        try {
            TlogDesign response = mApiDesignService.uploadBackgroundSync(
                    String.valueOf(UserManager.getInstance().getCurrentUserId()),
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
