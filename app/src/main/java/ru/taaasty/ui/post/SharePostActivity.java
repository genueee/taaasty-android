package ru.taaasty.ui.post;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKApi;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKAttachments;
import com.vk.sdk.api.model.VKWallPostResult;

import de.greenrobot.event.EventBus;
import ru.taaasty.ActivityBase;
import ru.taaasty.BuildConfig;
import ru.taaasty.R;
import ru.taaasty.UploadService;
import ru.taaasty.VkontakteHelper;
import ru.taaasty.events.VkGlobalEvent;
import ru.taaasty.model.Entry;

public class SharePostActivity extends ActivityBase {

    public static final String ARG_ENTRY = "ru.taaasty.ui.post.SharePostActivity.entry";

    private static final String KEY_DO_SHARE_VKONTAKTE = "ru.taaasty.ui.post.SharePostActivity.KEY_DO_SHARE_VKONTAKTE";

    private static final Uri TWITTER_SHARE_URL = Uri.parse("https://twitter.com/intent/tweet");

    private Entry mEntry;

    private boolean mDoShareVkontakte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_post);
        mEntry = getIntent().getParcelableExtra(ARG_ENTRY);
        if (mEntry == null) throw new IllegalArgumentException("ARG_ENTRY not defined");

        if (savedInstanceState != null) {
            mDoShareVkontakte = savedInstanceState.getBoolean(KEY_DO_SHARE_VKONTAKTE);
        }

        boolean isMyEntry = mEntry.isMyEntry();

        findViewById(R.id.ic_add_post_to_favorites).setVisibility(isMyEntry ? View.GONE : View.VISIBLE);
        findViewById(R.id.ic_report_post).setVisibility(mEntry.canReport() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_delete_post).setVisibility(mEntry.canDelete() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_edit_post).setVisibility(mEntry.canEdit() ? View.VISIBLE : View.GONE);
        findViewById(R.id.ic_save_post).setVisibility(mEntry.getImageUrls(true).isEmpty() ? View.GONE : View.VISIBLE);

        setFavoriteIcon();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_DO_SHARE_VKONTAKTE, mDoShareVkontakte);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        // Завершается если юзер ткнул вне панельки с кнопками
        finish();
        return true;
    }

    public void shareVkontakte(View view) {
        if (!VKSdk.wakeUpSession()) {
            mDoShareVkontakte = true;
            VKSdk.authorize(VkontakteHelper.VK_SCOPE, true, false);
        } else {
            mDoShareVkontakte = true;
            doSharePostVkontakte();
        }
    }

    public void shareFacebook(View view) {
        runPostActionActivity(PostActionActivity.ACTION_SHARE_FACEBOOK);
    }

    public void shareTwitter(View view) {
        final Uri.Builder builder;
        builder = TWITTER_SHARE_URL.buildUpon();
        builder.appendQueryParameter("url", mEntry.getEntryUrl());
        // builder.appendQueryParameter("text", text.toString());
        final Uri uri = builder.build();
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
        finish();
    }

    public void shareOther(View view) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (!TextUtils.isEmpty(mEntry.getTitle())) {
            intent.putExtra(Intent.EXTRA_SUBJECT, Html.fromHtml(mEntry.getTitle()).toString());
        }
        intent.putExtra(Intent.EXTRA_TEXT, mEntry.getEntryUrl());
        Intent chooser = Intent.createChooser(intent, getString(R.string.share_title));
        startActivity(chooser);
        finish();
    }

    public void addToFavorites(View view) {
        runPostActionActivity(PostActionActivity.ACTION_ADD_TO_FAVORITES);
        finish();
    }

    public void reportPost(View view) {
        DeleteOrReportDialogActivity.startReportPost(this, mEntry.getId());
        finish();
    }

    public void editPost(View view) {
        EditPostActivity.startEditPostActivity(this, mEntry);
        finish();
    }

    public void deletePost(View view) {
        DeleteOrReportDialogActivity.startDeletePost(this, mEntry.getId());
        finish();
    }

    /**
     * Сохранение картинок
     * @param view
     */
    public void savePost(View view) {
        UploadService.startDownloadImages(this, mEntry);
        finish();
    }

    public void runPostActionActivity( String action ) {
        finish();
        Intent i = new Intent(this, PostActionActivity.class);
        i.setAction(action);
        i.putExtra( PostActionActivity.ARG_ENTRY, mEntry);
        startActivity(i);
    }

    public void linkToPost(View view) {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        Uri copyUri = Uri.parse(mEntry.getEntryUrl());
        ClipData clip = ClipData.newPlainText("URL", mEntry.getEntryUrl());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this,
                getString(R.string.link_have_been_added_to_clipboard, copyUri),
                Toast.LENGTH_LONG).show();
        finish();
    }

    public void setFavoriteIcon() {
        if(mEntry.isFavorited()) {
            TextView action_icon = ((TextView)findViewById(R.id.ic_add_post_to_favorites));
            action_icon.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_post_in_favorites, 0, 0);
            action_icon.setText(R.string.post_in_favorites);
        }
        else {
            TextView action_icon = ((TextView)findViewById(R.id.ic_add_post_to_favorites));
            action_icon.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_add_post_to_favorites, 0, 0);
            action_icon.setText(R.string.add_post_to_favorites);
        }
    }

    public void onEventMainThread(VkGlobalEvent event) {
        if (BuildConfig.DEBUG) Log.v("SharePostActivity", "VkGlobalEvent " + event);
        if (!mDoShareVkontakte) return;

        switch (event.type) {
            case VkGlobalEvent.VK_ACCESS_DENIED:
                // Доступ запрещен.
                assert event.vkError != null;
                if (event.vkError.errorCode != VKError.VK_CANCELED) {
                    CharSequence errorText;
                    if (TextUtils.isEmpty(event.vkError.errorMessage)) {
                        errorText = getText(R.string.error_vkontakte_failed);
                    } else {
                        errorText = event.vkError.errorMessage;
                    }
                    Toast.makeText(this, errorText, Toast.LENGTH_LONG).show();
                }
                finish();
                break;
            case VkGlobalEvent.VK_ACCEPT_USER_TOKEN:
            case VkGlobalEvent.VK_RECEIVE_NEW_TOKEN:
                doSharePostVkontakte();
                break;
            default:
                break;
        }
    }

    private void doSharePostVkontakte() {
        VKAttachments attachments = null; //TODO указывать аттачи и текст
        String message = mEntry.getEntryUrl();

        VKRequest post = VKApi.wall().post(VKParameters.from(VKApiConst.ATTACHMENTS, attachments,
                VKApiConst.MESSAGE, message));
        post.setModelClass(VKWallPostResult.class);
        post.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                VKWallPostResult result = (VKWallPostResult)response.parsedModel;
                Uri url = Uri.parse("https://vk.com/wall"
                        + String.valueOf(VKSdk.getAccessToken().userId)
                        + "_" + String.valueOf(result.post_id));
                Toast.makeText(SharePostActivity.this, "Ссыка добавлена на стену", Toast.LENGTH_LONG).show();
                Intent i = new Intent(Intent.ACTION_VIEW, url);
                startActivity(i);
                finish();
            }

            @Override
            public void onError(VKError error) {
                Toast.makeText(SharePostActivity.this, R.string.error_vkontakte_failed, Toast.LENGTH_LONG).show();
                finish();
            }
        });

    }
}
