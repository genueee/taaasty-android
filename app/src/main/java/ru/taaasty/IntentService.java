package ru.taaasty;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.greenrobot.event.EventBus;
import ru.taaasty.events.EntryChanged;
import ru.taaasty.events.EntryUploadStatus;
import ru.taaasty.events.MarkAllAsReadRequestCompleted;
import ru.taaasty.events.MessageChanged;
import ru.taaasty.events.NotificationMarkedAsRead;
import ru.taaasty.events.NotificationReceived;
import ru.taaasty.events.TlogBackgroundUploadStatus;
import ru.taaasty.events.UserpicUploadStatus;
import ru.taaasty.rest.ResponseErrorException;
import ru.taaasty.rest.RestClient;
import ru.taaasty.rest.model.Conversation;
import ru.taaasty.rest.model.Entry;
import ru.taaasty.rest.model.MarkNotificationsAsReadResponse;
import ru.taaasty.rest.model.Notification;
import ru.taaasty.rest.model.PostAnonymousTextForm;
import ru.taaasty.rest.model.PostEmbeddForm;
import ru.taaasty.rest.model.PostForm;
import ru.taaasty.rest.model.PostImageForm;
import ru.taaasty.rest.model.PostQuoteForm;
import ru.taaasty.rest.model.PostTextForm;
import ru.taaasty.rest.model.TlogDesign;
import ru.taaasty.rest.model.Userpic;
import ru.taaasty.rest.service.ApiDesignSettings;
import ru.taaasty.rest.service.ApiEntries;
import ru.taaasty.rest.service.ApiMessenger;
import ru.taaasty.rest.service.ApiUsers;
import ru.taaasty.utils.ContentTypedOutput;
import ru.taaasty.utils.ImageUtils;
import ru.taaasty.utils.NetworkUtils;

/**
 * An {@link android.app.IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class IntentService extends android.app.IntentService {
    public static final boolean DBG = BuildConfig.DEBUG;
    public static final String TAG = "IntentService";

    private static final String ACTION_POST_ENTRY = "ru.taaasty.IntentService.action.POST_ENTRY";
    private static final String ACTION_EDIT_ENTRY = "ru.taaasty.IntentService.action.EDIT_ENTRY";

    private static final String ACTION_VOICE_REPLY_TO_CONVERSATION = "ru.taaasty.IntentService.action.ACTION_VOICE_REPLY_TO_CONVERSATION";

    private static final String ACTION_UPLOAD_USERPIC = "ru.taaasty.IntentService.action.UPLOAD_USERPIC";
    private static final String ACTION_UPLOAD_BACKGROUND = "ru.taaasty.IntentService.action.UPLOAD_BACKGROUND";

    private static final String ACTION_DOWNLOAD_IMAGES = "ru.taaasty.IntentService.action.DOWNLOAD_IMAGES";

    private static final String ACTION_MARK_NOTIFICATION_AS_READ = "ru.taaasty.IntentService.action.MARK_AS_READ";
    private static final String ACTION_MARK_ALL_NOTIFICATIONS_AS_READ = "ru.taaasty.IntentService.action.MARK_ALL_AS_READ";

    public static final String ACTION_NOTIFY_CONVERSATION_NOTIFICATION_CANCELLED = "ru.taaasty.IntentService.action.ACTION_NOTIFY_CONVERSATION_NOTIFICATION_CANCELLED";

    private static final String EXTRA_FORM = "ru.taaasty.extra.FORM";

    private static final String EXTRA_ENTRY_ID = "ru.taaasty.extra.ENTRY_ID";
    private static final String EXTRA_USER_ID = "ru.taaasty.extra.USER_ID";
    private static final String EXTRA_IMAGE_URI = "ru.taaasty.extra.IMAGE_URI";
    private static final String EXTRA_IMAGE_URL_LIST = "ru.taaasty.extra.IMAGE_URL_LIST";
    private static final String EXTRA_NOTIFY_NOTIFICATION_HELPER = "ru.taaasty.IntentService.extra.EXRA_NOTIFY_NOTIFICATION_HELPER";
    public static final String EXTRA_CONVERSATION_VOICE_REPLY_CONTENT = "ru.taaasty.extra.EXTRA_CONVERSATION_VOICE_REPLY_CONTENT";
    private static final String EXTRA_CONVERSATION_ID = "ru.taaasty.extra.EXTRA_CONVERSATION_ID";
    private static final String EXTRA_CONVERSATION_MESSAGE_IDS = "ru.taaasty.extra.EXTRA_CONVERSATION_MESSAGE_IDS";

    private static final String EXTRA_NOTIFICATION_IDS = "ru.taaasty.extra.EXTRA_NOTIFICATION_IDS";

    private static final Pattern FILENAME_PATTERN = Pattern.compile("^(.+)(\\..{1,5})$");

    private final ApiEntries mApiEntriesService;
    private final ApiUsers mApiUsersService;
    private final ApiDesignSettings mApiDesignService;

    private Handler mHandler;

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see android.app.IntentService
     */
    public static void startPostEntry(Context context, PostForm form) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_POST_ENTRY);
        intent.putExtra(EXTRA_FORM, form.asHtmlForm());
        context.startService(intent);
    }

    public static void startEditEntry(Context context, long entryId, PostForm form) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_EDIT_ENTRY);
        intent.putExtra(EXTRA_ENTRY_ID, entryId);
        intent.putExtra(EXTRA_FORM, form.asHtmlForm());
        context.startService(intent);
    }

    public static void startUploadUserpic(Context context, long userId, Uri imageUri) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_UPLOAD_USERPIC);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        context.startService(intent);
    }

    public static void startUploadBackground(Context context, long userId, Uri imageUri) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_UPLOAD_BACKGROUND);
        intent.putExtra(EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_IMAGE_URI, imageUri);
        context.startService(intent);
    }

    public static void startDownloadImages(Context context, Entry entry) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_DOWNLOAD_IMAGES);
        intent.putStringArrayListExtra(EXTRA_IMAGE_URL_LIST, entry.getImageUrls(true));
        context.startService(intent);
    }

    /**
     * Intent для отметки прочитанными нотификаций
     * @param context контекст
     * @param notificationIds ID нотификаций
     * @param notifyNotificationHelper true, если нужно будет убрать все уведомления из статусбара.
     * @return intent
     */
    public static Intent getMarkNotificationAsReadIntent(Context context, long notificationIds[], boolean notifyNotificationHelper) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_MARK_NOTIFICATION_AS_READ);
        intent.putExtra(EXTRA_NOTIFICATION_IDS, notificationIds);
        intent.putExtra(EXTRA_NOTIFY_NOTIFICATION_HELPER, notifyNotificationHelper);
        return intent;
    }

    public static Intent getVoiceReplyToConversationIntent(Context context, long conversationId, long messageIds[]) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_VOICE_REPLY_TO_CONVERSATION);
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId);
        intent.putExtra(EXTRA_CONVERSATION_MESSAGE_IDS, messageIds);
        return intent;
    }

    public static void markNotificationAsRead(Context context, long id) {
        Intent intent = getMarkNotificationAsReadIntent(context, new long[] {id}, false);
        context.startService(intent);
    }

    public static void markNotificationsAsRead(Context context, long ids[]) {
        Intent intent = getMarkNotificationAsReadIntent(context, ids, false);
        context.startService(intent);
    }

    public static void markAllNotificationsAsRead(Context context) {
        Intent intent = new Intent(context, IntentService.class);
        intent.setAction(ACTION_MARK_ALL_NOTIFICATIONS_AS_READ);
        context.startService(intent);
    }

    public IntentService() {
        super("IntentService");
        mApiEntriesService = RestClient.getAPiEntries();
        mApiUsersService = RestClient.getAPiUsers();
        mApiDesignService = RestClient.getAPiDesignSettings();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST_ENTRY.equals(action)) {
                PostForm.PostFormHtml entry = intent.getParcelableExtra(EXTRA_FORM);
                handlePostEntry(entry);
            } else if (ACTION_EDIT_ENTRY.equals(action)) {
                PostForm.PostFormHtml entry = intent.getParcelableExtra(EXTRA_FORM);
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
            } else if (ACTION_DOWNLOAD_IMAGES.equals(action)) {
                List<String> urlList = intent.getStringArrayListExtra(EXTRA_IMAGE_URL_LIST);
                handleDownloadImages(urlList);
            } else if (ACTION_MARK_NOTIFICATION_AS_READ.equals(action)) {
                long ids[] = intent.getLongArrayExtra(EXTRA_NOTIFICATION_IDS);
                if (ids != null) handleMarkNotificationsAsRead(ids);
                if (intent.getBooleanExtra(EXTRA_NOTIFY_NOTIFICATION_HELPER, false)) {
                    StatusBarNotifications.getInstance().onNotificationsMarkedAsRead();
                }
            } else if (ACTION_MARK_ALL_NOTIFICATIONS_AS_READ.equals(action)) {
                handleMarkAllNotificationsAsRead();
            } else if (ACTION_NOTIFY_CONVERSATION_NOTIFICATION_CANCELLED.equals(action)) {
                StatusBarNotifications.getInstance().onConversationNotificationCancelled();
            } else if (ACTION_VOICE_REPLY_TO_CONVERSATION.equals(action)) {
                long conversationId = intent.getLongExtra(EXTRA_CONVERSATION_ID, -1);
                long messageIds[] = intent.getLongArrayExtra(EXTRA_CONVERSATION_MESSAGE_IDS);
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                CharSequence replyContent = null;
                if (remoteInput != null) {
                    replyContent = remoteInput.getCharSequence(EXTRA_CONVERSATION_VOICE_REPLY_CONTENT);
                }
                try {
                    handleVoiceReplyToConversation(conversationId, messageIds, replyContent);
                }finally {
                    StatusBarNotifications.getInstance().onConversationNotificationCancelled();
                    ((TaaastyApplication)getApplication()).sendAnalyticsEvent(Constants.ANALYTICS_CATEGORY_NOTIFICATIONS,
                            "Ответ в диалог голосом", null);
                }
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handlePostEntry(PostForm.PostFormHtml form) {
        EntryUploadStatus status;
        Entry response = null;

        try {
            if (form instanceof PostAnonymousTextForm.AsHtml) {
                PostAnonymousTextForm.AsHtml postText = (PostAnonymousTextForm.AsHtml) form;
                response = mApiEntriesService.createAnonymousPostSync(
                        postText.title, postText.text);
            } else if (form instanceof PostTextForm.AsHtml) {
                PostTextForm.AsHtml postText = (PostTextForm.AsHtml) form;
                response = mApiEntriesService.createTextPostSync(
                        postText.title, postText.text, postText.privacy);
            } else if (form instanceof PostQuoteForm.AsHtml) {
                PostQuoteForm.AsHtml postQuote = (PostQuoteForm.AsHtml)form;
                response = mApiEntriesService.createQuoteEntrySync(
                        postQuote.text, postQuote.source, postQuote.privacy);
            } else if (form instanceof PostImageForm.AsHtml) {
                PostImageForm.AsHtml postImage = (PostImageForm.AsHtml) form;
                response = mApiEntriesService.createImagePostSync(
                        postImage.title,
                        postImage.privacy,
                        postImage.imageUri == null ? null : new ContentTypedOutput(this, postImage.imageUri, null)
                );
            } else if (form instanceof PostEmbeddForm.AsHtml) {
                PostEmbeddForm.AsHtml postForm = (PostEmbeddForm.AsHtml)form;
                response = mApiEntriesService.createVideoPostSync(postForm.title, postForm.url, postForm.privacy);
            } else {
                throw new IllegalStateException();
            }
            status = EntryUploadStatus.createPostCompleted(form);
        } catch (ResponseErrorException ree) {
            status = EntryUploadStatus.createPostFinishedWithError(form, ree.error.error, ree);
        } catch (Throwable ex) {
            if (DBG) throw new IllegalStateException(ex);
            status = EntryUploadStatus.createPostFinishedWithError(form, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
        if (response != null) EventBus.getDefault().post(new EntryChanged(response));
    }

    private void handleEditEntry(long entryId, PostForm.PostFormHtml form) {
        EntryUploadStatus status;
        Entry response = null;

        try {
            if (form instanceof PostAnonymousTextForm.AsHtml) {
                PostAnonymousTextForm.AsHtml postText = (PostAnonymousTextForm.AsHtml)form;
                response = mApiEntriesService.updateAnonymousPostSync(String.valueOf(entryId),
                        postText.title,
                        postText.text);
            } else if (form instanceof PostTextForm.AsHtml) {
                PostTextForm.AsHtml postText = (PostTextForm.AsHtml)form;
                response = mApiEntriesService.updateTextPostSync(String.valueOf(entryId),
                        postText.title,
                        postText.text,
                        postText.privacy);
            } else if (form instanceof PostQuoteForm.AsHtml) {
                PostQuoteForm.AsHtml postQuote = (PostQuoteForm.AsHtml)form;
                response = mApiEntriesService.updateQuoteEntrySync(String.valueOf(entryId),
                        postQuote.text,
                        postQuote.source,
                        postQuote.privacy);
            } else if (form instanceof PostImageForm.AsHtml) {
                PostImageForm.AsHtml postImage = (PostImageForm.AsHtml) form;
                response = mApiEntriesService.updateImagePostSync(String.valueOf(entryId),
                        postImage.title,
                        postImage.privacy,
                        postImage.imageUri == null ? null : new ContentTypedOutput(this, postImage.imageUri, null)
                );
            } else if (form instanceof PostEmbeddForm.AsHtml) {
                PostEmbeddForm.AsHtml postForm = (PostEmbeddForm.AsHtml) form;
                response = mApiEntriesService.updateVideoPostSync(String.valueOf(entryId), postForm.title, postForm.url, postForm.privacy);
            } else {
                throw new IllegalStateException();
            }
            status = EntryUploadStatus.createPostCompleted(form);
        } catch (ResponseErrorException ree) {
            status = EntryUploadStatus.createPostFinishedWithError(form, ree.error.error, ree);
        } catch (Throwable ex) {
            if (DBG) throw new IllegalStateException(ex);
            status = EntryUploadStatus.createPostFinishedWithError(form, getString(R.string.error_vote), ex);
        }

        if (DBG) Log.v(TAG, "status: " + status);
        EventBus.getDefault().post(status);
        if (response != null) EventBus.getDefault().post(new EntryChanged(response));
    }

    private void handleVoiceReplyToConversation(long conversationId, long messageIds[], CharSequence replyContent) {
        if (conversationId == -1 || TextUtils.isEmpty(replyContent)) {
            if (DBG) Log.e(TAG, "empty content or conversation id");
            return;
        }

        // TODO Как-то уведомлять, что идет загрузка и при ошибке
        try {
            ApiMessenger api = RestClient.getAPiMessenger();
            if (messageIds != null) {
                try {
                    ArrayList<Long> idsArray = new ArrayList<>(messageIds.length);
                    for (long id: messageIds) idsArray.add(id);
                    String ids = TextUtils.join(",", idsArray);
                    api.markMessagesAsReadSync(null, conversationId, ids);
                } catch (ResponseErrorException ignore) {
                    if (DBG) Log.v(TAG, "markMessagesAsReadSync() error");
                }
            }
            Conversation.Message message = api.postMessageSync(null, conversationId, replyContent.toString(),
                    UUID.randomUUID().toString(), null);
            EventBus.getDefault().post(new MessageChanged(message));
        } catch (ResponseErrorException ree) {
            if (DBG) Log.i(TAG, "handleVoiceReplyToConversation() error", ree);
        } catch (Throwable ex) {
            if (DBG) throw new IllegalStateException(ex);
        }
    }

    private void handleUploadUserpic(long userId, Uri imageUri) {
        UserpicUploadStatus status;
        try {
            Userpic response = mApiUsersService.uploadUserpicSync(new ContentTypedOutput(this, imageUri, null));
            status = UserpicUploadStatus.createUploadCompleted(userId, imageUri, response);
            if (DBG) Log.v(TAG, "userpic response: " + response);
        }catch (ResponseErrorException ree) {
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
                    String.valueOf(Session.getInstance().getCurrentUserId()),
                    new ContentTypedOutput(this, imageUri, null));
            status = TlogBackgroundUploadStatus.createUploadCompleted(userId, imageUri, response);
            if (DBG) Log.v(TAG, "userpic response: " + response);
        }catch (ResponseErrorException ree) {
            status = TlogBackgroundUploadStatus.createUploadFinishedWithError(userId, imageUri, ree.error.error, ree);
        } catch (Exception ex) {
            if (DBG) throw ex;
            status = TlogBackgroundUploadStatus.createUploadFinishedWithError(userId, imageUri, getString(R.string.error_upload_userpic), ex);
        }
        EventBus.getDefault().post(status);
    }

    void handleDownloadImages(List<String> urlList) {
        NotificationCompat.Builder notificationBuilder;
        final NotificationManagerCompat notificationManager;

        notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOAD_IMAGES);

        final File picturesDirectory = ImageUtils.getPicturesDirectory(this);
        if (picturesDirectory == null) {
            showToastOnUiThread(getText(R.string.error_no_place_to_save));
            return;
        }

        File firstFile = null;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_file_download)
                .setLargeIcon(largeIcon)
                .setContentTitle(getText(R.string.notification_title_save_images))
                .setContentText(getString(R.string.notification_text_save_images, picturesDirectory.toString()))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setProgress(0, 0, true)
        ;
        notificationManager.notify(Constants.NOTIFICATION_ID_DOWNLOAD_IMAGES, notificationBuilder.build());
        try {
            Set<String> uniqUrlList = new HashSet<>(urlList);
            final List<String> dstFiles = new ArrayList<>(uniqUrlList.size());
            for (String url: uniqUrlList) {
                try {
                    final File dstFile = generateUniqDstFile(url, picturesDirectory);
                    downloadFile(url, dstFile);
                    dstFiles.add(dstFile.getAbsolutePath());
                    if (firstFile == null) firstFile = dstFile;
                } catch (IOException e) {
                    Log.e(TAG, "download error. Url: " + url, e);
                }
            }
            if (dstFiles.isEmpty()) {
                throw new IOException("no files");
            } else {
                if (mHandler != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            MediaScannerConnection.scanFile(getApplicationContext(),
                                    dstFiles.toArray(new String[dstFiles.size()]),
                                    null, null);
                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.pictures_saved_into, picturesDirectory.toString()),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            showToastOnUiThread(getText(R.string.error_saving_image));
            Log.e(TAG, "error saving image ", e);
            firstFile = null;
        } finally {
            notificationManager.cancel(Constants.NOTIFICATION_ID_DOWNLOAD_IMAGES);
            showToastPicturesSaved(notificationManager, firstFile);
        }
    }

    private void handleMarkNotificationsAsRead(long notificationIds[]) {
        if (DBG) Log.v(TAG, "handleMarkNotificationsAsRead ids: " + Arrays.toString(notificationIds));
        ApiMessenger api = RestClient.getAPiMessenger();
        for (long notificationId: notificationIds) {
            try {
                Notification notification = api.markNotificationAsReadSync(null, notificationId);
                EventBus.getDefault().post(new NotificationReceived(notification));
                StatusBarNotifications.getInstance().onNewNotificationIdSeen(notificationId); // На всякий случай, иначе может и не дойти
            } catch (Throwable e) {
                Log.e(TAG, "markNotificationAsReadSync error", e);
            }
        }
    }

    private void handleMarkAllNotificationsAsRead() {
        ApiMessenger api = RestClient.getAPiMessenger();
        EventBus eventBus = EventBus.getDefault();
        try {
            List<MarkNotificationsAsReadResponse> response = api.markAllNotificationsAsRead(null, null);
            eventBus.post(new NotificationMarkedAsRead(response));
            eventBus.post(new MarkAllAsReadRequestCompleted(null));

            long maxId = 0;
            for (MarkNotificationsAsReadResponse item: response) maxId = Math.max(maxId, item.id);
            StatusBarNotifications.getInstance().onNewNotificationIdSeen(maxId); // На всякий случай, иначе может и не дойти
        } catch (Throwable e) {
            Log.e(TAG, "markNotificationAsReadSync error", e);
            EventBus.getDefault().post(new MarkAllAsReadRequestCompleted(e));
        }
    }

    private void showToastOnUiThread(final CharSequence text) {
        if (mHandler == null) return;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showToastPicturesSaved(final NotificationManagerCompat manager, final File firstFile) {
        if (firstFile == null || mHandler == null) return;

        final Bitmap firstFileBitmap;

        Bitmap firstFileBitmap1;
        try {
            firstFileBitmap1 = downloadNotificationPicture(firstFile);
        } catch (IOException e) {
            firstFileBitmap1 = null;
            Log.e(TAG, "download picture error", e);
        }

        firstFileBitmap = firstFileBitmap1;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                NotificationCompat.Builder builder = new NotificationCompat.Builder(IntentService.this)
                        .setSmallIcon(R.drawable.ic_stat_file_download)
                        .setContentTitle(getText(R.string.pictures_saved))
                        .setContentText(firstFile.getPath())
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);
                if (firstFileBitmap != null) {
                    builder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(firstFileBitmap));
                    Intent showImageIntent = new Intent();
                    showImageIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    showImageIntent.setAction(android.content.Intent.ACTION_VIEW);
                    showImageIntent.setDataAndType(Uri.fromFile(firstFile), "image/*");
                    PendingIntent resultPendingIntent =
                            PendingIntent.getActivity(getApplicationContext(), 0,
                                    showImageIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);
                    builder.setContentIntent(resultPendingIntent);
                }
                manager.notify(Constants.NOTIFICATION_ID_DOWNLOAD_IMAGES, builder.build());
            }
        });
    }

    Bitmap downloadNotificationPicture(File file) throws IOException {
        int height = getResources().getDimensionPixelSize(R.dimen.notification_picture_height);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return Picasso.with(this)
                .load(file)
                .resize(dm.widthPixels, height)
                .onlyScaleDown()
                .centerCrop()
                .get();
    }

    private void downloadFile(String url, File dstFile) throws IOException {
        if (DBG) Log.v(TAG, "Downloading " + url + " to " + dstFile);
        OkHttpClient client = NetworkUtils.getInstance().getOkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        byte[] buf = new byte[16384];
        InputStream is = null;
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(dstFile));
            is = response.body().byteStream();
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
        } finally {
            if (is != null) Util.closeQuietly(is);
            if (os != null) Util.closeQuietly(os);
        }
    }

    private File generateUniqDstFile(String url, File dstDir) throws IOException {
        Uri uri = Uri.parse(url);
        String filename = uri.getLastPathSegment();
        if (TextUtils.isEmpty(filename)) {
            filename = "img.jpg";
        }
        File res = new File(dstDir, filename);
        if (!res.exists() && !TextUtils.isEmpty(res.getName())) {
            return res;
        }

        String prefix;
        String suffix;

        Matcher matcher = FILENAME_PATTERN.matcher(filename);
        if (matcher.matches()) {
            prefix = matcher.group(1);
            suffix = matcher.group(2);
            if (suffix == null) suffix = "";
        } else {
            prefix = filename;
            suffix = "";
        }

        for (int i = 0; i < 5000; ++i) {
            res = new File(dstDir, prefix + "_" + String.valueOf(i) + suffix);
            if (!res.exists() && !TextUtils.isEmpty(res.getName())) {
                return res;
            }
        }

        throw new IOException("dst filename error, url: " + url);
    }
}
